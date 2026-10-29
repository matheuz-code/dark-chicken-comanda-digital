package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.edu.unasp.comandadigital.dto.admin.pedido.AtualizarStatusPedidoRequest;
import br.edu.unasp.comandadigital.dto.admin.pedido.CancelarPedidoRequest;
import br.edu.unasp.comandadigital.dto.admin.pedido.ClientePedidoAdminResponse;
import br.edu.unasp.comandadigital.dto.admin.pedido.PedidoAdminDetalheResponse;
import br.edu.unasp.comandadigital.dto.admin.pedido.PedidoAdminHistoricoResponse;
import br.edu.unasp.comandadigital.dto.admin.pedido.PedidoAdminItemResponse;
import br.edu.unasp.comandadigital.dto.admin.pedido.PedidoAdminResumoResponse;
import br.edu.unasp.comandadigital.dto.admin.pedido.UsuarioHistoricoAdminResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.Pedido;
import br.edu.unasp.comandadigital.entity.PedidoItem;
import br.edu.unasp.comandadigital.entity.PedidoStatusHistorico;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.CanalPedido;
import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.StatusPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.PedidoItemRepository;
import br.edu.unasp.comandadigital.repository.PedidoQuantidadeItensProjection;
import br.edu.unasp.comandadigital.repository.PedidoRepository;
import br.edu.unasp.comandadigital.repository.PedidoStatusHistoricoRepository;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminPedidoService {

    private static final Sort ORDENACAO = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));
    private static final Set<StatusPedido> STATUS_VISIVEIS_COZINHA = EnumSet.of(
            StatusPedido.RECEBIDO,
            StatusPedido.CONFIRMADO,
            StatusPedido.EM_PREPARO,
            StatusPedido.PRONTO);
    private static final Set<StatusPedido> STATUS_COM_RESTRICAO_CANCELAMENTO = EnumSet.of(
            StatusPedido.EM_PREPARO,
            StatusPedido.PRONTO,
            StatusPedido.SAIU_ENTREGA);

    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final PedidoStatusHistoricoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoquePedidoService estoquePedidoService;

    public AdminPedidoService(
            PedidoRepository pedidoRepository,
            PedidoItemRepository pedidoItemRepository,
            PedidoStatusHistoricoRepository historicoRepository,
            UsuarioRepository usuarioRepository,
            EstoquePedidoService estoquePedidoService) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoItemRepository = pedidoItemRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoquePedidoService = estoquePedidoService;
    }

    public PaginaResponse<PedidoAdminResumoResponse> listar(
            String emailUsuario,
            StatusPedido status,
            CanalPedido canal,
            Pageable pageable) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO);
        Page<Pedido> pedidos = usuario.getPerfil() == PerfilUsuario.COZINHEIRO
                ? listarParaCozinheiro(status, canal, pageRequest)
                : listarParaGestao(status, canal, pageRequest);
        Map<Long, Long> quantidades = carregarQuantidadesItens(
                pedidos.getContent().stream().map(Pedido::getId).toList());

        return PaginaResponse.de(pedidos.map(pedido -> new PedidoAdminResumoResponse(
                pedido.getId(),
                toCliente(pedido.getCliente()),
                pedido.getCanal(),
                pedido.getStatus(),
                pedido.getStatusPagamento(),
                pedido.getFormaPagamento(),
                pedido.getValorTotal(),
                quantidades.getOrDefault(pedido.getId(), 0L),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt())));
    }

    public PedidoAdminDetalheResponse detalhar(String emailUsuario, Long pedidoId) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        Pedido pedido = buscarComCliente(pedidoId);
        validarVisibilidade(usuario, pedido);
        return toDetalhe(pedido);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PedidoAdminDetalheResponse atualizarStatus(
            String emailUsuario,
            Long pedidoId,
            AtualizarStatusPedidoRequest request) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        Pedido pedido = buscarParaAtualizacao(pedidoId);
        StatusPedido statusAnterior = pedido.getStatus();
        StatusPedido novoStatus = request.status();

        if (novoStatus == StatusPedido.CANCELADO) {
            throw new RegraNegocioException(
                    "Use o endpoint de cancelamento para cancelar um pedido");
        }
        validarPermissaoTransicao(usuario, statusAnterior, novoStatus);
        validarSequenciaStatus(statusAnterior, novoStatus);

        if (novoStatus == StatusPedido.EM_PREPARO) {
            estoquePedidoService.baixarAoIniciarPreparo(pedido, usuario);
        }
        pedido.setStatus(novoStatus);
        pedidoRepository.saveAndFlush(pedido);
        historicoRepository.saveAndFlush(PedidoStatusHistorico.registrar(
                pedido,
                novoStatus,
                usuario,
                observacaoTransicao(request.observacao(), statusAnterior, novoStatus)));
        return toDetalhe(pedido);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PedidoAdminDetalheResponse cancelar(
            String emailUsuario,
            Long pedidoId,
            CancelarPedidoRequest request) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        Pedido pedido = buscarParaAtualizacao(pedidoId);
        StatusPedido statusAnterior = pedido.getStatus();

        if (statusAnterior == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("O pedido já está cancelado");
        }
        if (statusAnterior == StatusPedido.FINALIZADO) {
            throw new RegraNegocioException("Um pedido finalizado não pode ser cancelado");
        }
        if (STATUS_COM_RESTRICAO_CANCELAMENTO.contains(statusAnterior)
                && usuario.getPerfil() == PerfilUsuario.COZINHEIRO) {
            throw new AccessDeniedException(
                    "Depois de EM_PREPARO, somente GERENTE ou ADMIN pode cancelar");
        }

        String motivo = request.motivo().trim();
        estoquePedidoService.estornarAoCancelar(pedido, usuario);
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setMotivoCancelamento(motivo);
        if (pedido.getStatusPagamento() == StatusPagamento.PAGO) {
            pedido.setStatusPagamento(StatusPagamento.ESTORNADO);
        } else if (pedido.getStatusPagamento() == StatusPagamento.PENDENTE) {
            pedido.setStatusPagamento(StatusPagamento.CANCELADO);
        }
        pedidoRepository.saveAndFlush(pedido);
        historicoRepository.saveAndFlush(PedidoStatusHistorico.registrar(
                pedido,
                StatusPedido.CANCELADO,
                usuario,
                "Cancelado: " + motivo));
        return toDetalhe(pedido);
    }

    private Page<Pedido> listarParaGestao(
            StatusPedido status,
            CanalPedido canal,
            Pageable pageable) {
        if (status != null && canal != null) {
            return pedidoRepository.findByStatusAndCanal(status, canal, pageable);
        }
        if (status != null) return pedidoRepository.findByStatus(status, pageable);
        if (canal != null) return pedidoRepository.findByCanal(canal, pageable);
        return pedidoRepository.findAll(pageable);
    }

    private Page<Pedido> listarParaCozinheiro(
            StatusPedido status,
            CanalPedido canal,
            Pageable pageable) {
        if (status != null && !STATUS_VISIVEIS_COZINHA.contains(status)) {
            throw new AccessDeniedException(
                    "O cozinheiro só pode consultar pedidos pendentes da cozinha");
        }
        Collection<StatusPedido> statuses = status == null
                ? STATUS_VISIVEIS_COZINHA
                : List.of(status);
        return canal == null
                ? pedidoRepository.findByStatusIn(statuses, pageable)
                : pedidoRepository.findByStatusInAndCanal(statuses, canal, pageable);
    }

    private void validarVisibilidade(Usuario usuario, Pedido pedido) {
        if (usuario.getPerfil() == PerfilUsuario.COZINHEIRO
                && !STATUS_VISIVEIS_COZINHA.contains(pedido.getStatus())) {
            throw new AccessDeniedException(
                    "O cozinheiro só pode consultar pedidos pendentes da cozinha");
        }
    }

    private void validarPermissaoTransicao(
            Usuario usuario,
            StatusPedido statusAtual,
            StatusPedido novoStatus) {
        if (usuario.getPerfil() != PerfilUsuario.COZINHEIRO) return;
        boolean permitido = statusAtual == StatusPedido.CONFIRMADO
                && novoStatus == StatusPedido.EM_PREPARO
                || statusAtual == StatusPedido.EM_PREPARO
                && novoStatus == StatusPedido.PRONTO;
        if (!permitido) {
            throw new AccessDeniedException(
                    "O cozinheiro só pode executar CONFIRMADO → EM_PREPARO → PRONTO");
        }
    }

    private void validarSequenciaStatus(
            StatusPedido statusAtual,
            StatusPedido novoStatus) {
        StatusPedido statusEsperado = switch (statusAtual) {
            case RECEBIDO -> StatusPedido.CONFIRMADO;
            case CONFIRMADO -> StatusPedido.EM_PREPARO;
            case EM_PREPARO -> StatusPedido.PRONTO;
            case PRONTO -> StatusPedido.SAIU_ENTREGA;
            case SAIU_ENTREGA -> StatusPedido.FINALIZADO;
            case FINALIZADO, CANCELADO -> null;
        };
        if (statusEsperado == null || novoStatus != statusEsperado) {
            throw new RegraNegocioException(
                    "Transição de status inválida: " + statusAtual + " → " + novoStatus);
        }
    }

    private PedidoAdminDetalheResponse toDetalhe(Pedido pedido) {
        List<PedidoAdminItemResponse> itens = pedidoItemRepository.findByPedidoId(pedido.getId())
                .stream()
                .map(this::toItem)
                .toList();
        List<PedidoAdminHistoricoResponse> timeline = historicoRepository
                .findAllByPedidoIdOrderByCreatedAtAsc(pedido.getId())
                .stream()
                .map(this::toHistorico)
                .toList();
        return new PedidoAdminDetalheResponse(
                pedido.getId(),
                toCliente(pedido.getCliente()),
                pedido.getCanal(),
                pedido.getStatus(),
                pedido.getStatusPagamento(),
                pedido.getFormaPagamento(),
                pedido.getValorTotal(),
                pedido.getEnderecoEntrega(),
                pedido.getObservacoes(),
                pedido.getMotivoCancelamento(),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt(),
                itens,
                timeline);
    }

    private PedidoAdminItemResponse toItem(PedidoItem item) {
        BigDecimal subtotal = item.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(item.getQuantidade()))
                .setScale(2, RoundingMode.HALF_UP);
        return new PedidoAdminItemResponse(
                item.getPrato().getId(),
                item.getPrato().getNome(),
                item.getPrato().getFotoUrl(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                subtotal,
                item.getObservacoes());
    }

    private PedidoAdminHistoricoResponse toHistorico(PedidoStatusHistorico historico) {
        Usuario usuario = historico.getUsuario();
        UsuarioHistoricoAdminResponse usuarioResponse = usuario == null
                ? null
                : new UsuarioHistoricoAdminResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getPerfil());
        return new PedidoAdminHistoricoResponse(
                historico.getStatus(),
                historico.getObservacao(),
                usuarioResponse,
                historico.getCreatedAt());
    }

    private ClientePedidoAdminResponse toCliente(Usuario cliente) {
        return new ClientePedidoAdminResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone());
    }

    private Map<Long, Long> carregarQuantidadesItens(Collection<Long> pedidoIds) {
        if (pedidoIds.isEmpty()) return Map.of();
        Map<Long, Long> quantidades = new HashMap<>();
        for (PedidoQuantidadeItensProjection projection
                : pedidoItemRepository.somarQuantidadesPorPedido(pedidoIds)) {
            quantidades.put(projection.getPedidoId(), projection.getQuantidadeItens());
        }
        return quantidades;
    }

    private Usuario carregarUsuarioInterno(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .filter(usuario -> usuario.getStatus() == StatusAtivacao.ATIVO)
                .filter(usuario -> usuario.getPerfil() != PerfilUsuario.CLIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário interno autenticado não encontrado ou inativo"));
    }

    private Pedido buscarComCliente(Long id) {
        return pedidoRepository.findComClienteById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    private Pedido buscarParaAtualizacao(Long id) {
        return pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    private String observacaoTransicao(
            String observacao,
            StatusPedido statusAnterior,
            StatusPedido novoStatus) {
        if (observacao != null && !observacao.isBlank()) return observacao.trim();
        return "Status alterado de " + statusAnterior + " para " + novoStatus;
    }
}
