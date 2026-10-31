package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.dto.pedido.CriarPedidoRequest;
import br.edu.unasp.comandadigital.dto.pedido.PedidoCriadoResponse;
import br.edu.unasp.comandadigital.dto.pedido.PedidoItemRequest;
import br.edu.unasp.comandadigital.dto.pedido.PedidoItemResponse;
import br.edu.unasp.comandadigital.dto.pedido.PedidoResumoResponse;
import br.edu.unasp.comandadigital.dto.pedido.PedidoStatusHistoricoResponse;
import br.edu.unasp.comandadigital.dto.pedido.PedidoStatusResponse;
import br.edu.unasp.comandadigital.entity.Pedido;
import br.edu.unasp.comandadigital.entity.PedidoItem;
import br.edu.unasp.comandadigital.entity.PedidoStatusHistorico;
import br.edu.unasp.comandadigital.entity.Prato;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.StatusPrato;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.PedidoItemRepository;
import br.edu.unasp.comandadigital.repository.PedidoQuantidadeItensProjection;
import br.edu.unasp.comandadigital.repository.PedidoRepository;
import br.edu.unasp.comandadigital.repository.PedidoStatusHistoricoRepository;
import br.edu.unasp.comandadigital.repository.PratoRepository;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PedidoService {

    private static final Sort ORDENACAO_MEUS_PEDIDOS = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final UsuarioRepository usuarioRepository;
    private final PratoRepository pratoRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final PedidoStatusHistoricoRepository pedidoStatusHistoricoRepository;
    private final NecessidadeEstoqueService necessidadeEstoqueService;

    public PedidoService(
            UsuarioRepository usuarioRepository,
            PratoRepository pratoRepository,
            PedidoRepository pedidoRepository,
            PedidoItemRepository pedidoItemRepository,
            PedidoStatusHistoricoRepository pedidoStatusHistoricoRepository,
            NecessidadeEstoqueService necessidadeEstoqueService) {
        this.usuarioRepository = usuarioRepository;
        this.pratoRepository = pratoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoItemRepository = pedidoItemRepository;
        this.pedidoStatusHistoricoRepository = pedidoStatusHistoricoRepository;
        this.necessidadeEstoqueService = necessidadeEstoqueService;
    }

    @Transactional
    public PedidoCriadoResponse criar(String emailCliente, String idempotencyKey, CriarPedidoRequest request) {
        Usuario cliente = carregarClienteParaCriacao(emailCliente);
        String chave = validarChaveIdempotencia(idempotencyKey);
        String requestHash = hashRequest(request);
        var pedidoExistente = pedidoRepository.findByClienteIdAndIdempotencyKey(cliente.getId(), chave);
        if (pedidoExistente.isPresent()) {
            Pedido pedido = pedidoExistente.get();
            if (!requestHash.equals(pedido.getIdempotencyRequestHash())) {
                throw new RegraNegocioException("A chave de idempotência já foi usada com outro pedido");
            }
            return toCriadoResponse(pedido, pedidoItemRepository.findByPedidoId(pedido.getId()));
        }
        Map<Long, PedidoItemRequest> itensPorPrato = validarItensUnicos(request.itens());
        Map<Long, Prato> pratosPorId = carregarPratosAtivos(itensPorPrato.keySet());

        validarFichasTecnicasEEstoque(itensPorPrato, pratosPorId);

        Pedido pedido = Pedido.novoPedidoSite(
                cliente,
                request.formaPagamento(),
                request.enderecoEntrega().trim(),
                normalizarTextoOpcional(request.observacoes()), chave, requestHash);

        List<PedidoItem> itens = itensPorPrato.values().stream()
                .map(itemRequest -> {
                    Prato prato = pratosPorId.get(itemRequest.pratoId());
                    return PedidoItem.criar(
                            pedido,
                            prato,
                            itemRequest.quantidade(),
                            prato.getPrecoVenda(),
                            normalizarTextoOpcional(itemRequest.observacoes()));
                })
                .toList();

        BigDecimal valorTotal = itens.stream()
                .map(item -> item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        pedido.setValorTotal(valorTotal);

        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        pedidoItemRepository.saveAll(itens);
        pedidoStatusHistoricoRepository.save(PedidoStatusHistorico.registrar(
                pedidoSalvo,
                pedidoSalvo.getStatus(),
                cliente,
                "Pedido recebido pelo site"));

        return toCriadoResponse(pedidoSalvo, itens);
    }

    public PaginaResponse<PedidoResumoResponse> listarMeus(
            String emailCliente,
            Pageable pageable) {
        Usuario cliente = carregarCliente(emailCliente);
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO_MEUS_PEDIDOS);
        Page<Pedido> pedidos = pedidoRepository.findByClienteId(cliente.getId(), pageRequest);

        Map<Long, Long> quantidades = carregarQuantidadesItens(
                pedidos.getContent().stream().map(Pedido::getId).toList());

        return PaginaResponse.de(pedidos.map(pedido -> new PedidoResumoResponse(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getStatusPagamento(),
                pedido.getFormaPagamento(),
                pedido.getValorTotal(),
                quantidades.getOrDefault(pedido.getId(), 0L),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt())));
    }

    public PedidoCriadoResponse detalharMeu(String emailCliente, Long pedidoId) {
        Usuario cliente = carregarCliente(emailCliente);
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, cliente.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado para o cliente autenticado"));
        return toCriadoResponse(pedido, pedidoItemRepository.findByPedidoId(pedido.getId()));
    }

    public PedidoStatusResponse consultarStatus(String emailCliente, Long pedidoId) {
        Usuario cliente = carregarCliente(emailCliente);
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, cliente.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido não encontrado para o cliente autenticado"));

        List<PedidoStatusHistoricoResponse> timeline = pedidoStatusHistoricoRepository
                .findAllByPedidoIdOrderByCreatedAtAsc(pedido.getId())
                .stream()
                .map(historico -> new PedidoStatusHistoricoResponse(
                        historico.getStatus(),
                        historico.getObservacao(),
                        historico.getCreatedAt()))
                .toList();

        return new PedidoStatusResponse(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getStatusPagamento(),
                pedido.getUpdatedAt(),
                timeline);
    }

    private Usuario carregarCliente(String emailCliente) {
        return usuarioRepository.findByEmailIgnoreCase(emailCliente)
                .filter(usuario -> usuario.getStatus() == StatusAtivacao.ATIVO)
                .filter(usuario -> usuario.getPerfil() == PerfilUsuario.CLIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente autenticado não encontrado ou inativo"));
    }

    private Usuario carregarClienteParaCriacao(String emailCliente) {
        return usuarioRepository.findByEmailIgnoreCaseForUpdate(emailCliente)
                .filter(usuario -> usuario.getStatus() == StatusAtivacao.ATIVO)
                .filter(usuario -> usuario.getPerfil() == PerfilUsuario.CLIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente autenticado não encontrado ou inativo"));
    }

    private String validarChaveIdempotencia(String chave) {
        if (chave == null || chave.isBlank()) return UUID.randomUUID().toString();
        if (chave == null || !chave.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw new RegraNegocioException("Idempotency-Key deve ter entre 8 e 100 caracteres seguros");
        }
        return chave;
    }

    private String hashRequest(CriarPedidoRequest request) {
        String valor = request.itens().stream()
                .sorted(java.util.Comparator.comparing(PedidoItemRequest::pratoId))
                .map(item -> item.pratoId() + ":" + item.quantidade() + ":" + normalizarTextoOpcional(item.observacoes()))
                .collect(Collectors.joining("|")) + "#" + request.enderecoEntrega().trim() + "#" + request.formaPagamento() + "#" + normalizarTextoOpcional(request.observacoes());
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private Map<Long, PedidoItemRequest> validarItensUnicos(List<PedidoItemRequest> itens) {
        Map<Long, PedidoItemRequest> itensPorPrato = new LinkedHashMap<>();
        for (PedidoItemRequest item : itens) {
            if (itensPorPrato.putIfAbsent(item.pratoId(), item) != null) {
                throw new RegraNegocioException(
                        "Cada prato deve aparecer somente uma vez no pedido");
            }
        }
        return itensPorPrato;
    }

    private Map<Long, Prato> carregarPratosAtivos(Set<Long> pratoIds) {
        Map<Long, Prato> pratosPorId = pratoRepository.findByIdInAndStatusAndCategoriaStatus(
                        pratoIds,
                        StatusPrato.ATIVO,
                        StatusAtivacao.ATIVO)
                .stream()
                .collect(Collectors.toMap(Prato::getId, Function.identity()));

        if (pratosPorId.size() != pratoIds.size()) {
            Long pratoIndisponivel = pratoIds.stream()
                    .filter(id -> !pratosPorId.containsKey(id))
                    .findFirst()
                    .orElseThrow();
            throw new RegraNegocioException(
                    "O prato " + pratoIndisponivel + " não existe ou não está disponível");
        }
        return pratosPorId;
    }

    private void validarFichasTecnicasEEstoque(
            Map<Long, PedidoItemRequest> itensPorPrato,
            Map<Long, Prato> pratosPorId) {
        Map<Long, Integer> quantidadesPorPrato = itensPorPrato.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().quantidade()));
        var necessidades = necessidadeEstoqueService.calcular(
                pratosPorId,
                quantidadesPorPrato);
        necessidadeEstoqueService.validarDisponibilidade(necessidades);
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

    private PedidoCriadoResponse toCriadoResponse(Pedido pedido, List<PedidoItem> itens) {
        List<PedidoItemResponse> itemResponses = itens.stream()
                .map(item -> new PedidoItemResponse(
                        item.getPrato().getId(),
                        item.getPrato().getNome(),
                        item.getPrato().getFotoUrl(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getPrecoUnitario()
                                .multiply(BigDecimal.valueOf(item.getQuantidade()))
                                .setScale(2, RoundingMode.HALF_UP),
                        item.getObservacoes()))
                .toList();

        return new PedidoCriadoResponse(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getStatusPagamento(),
                pedido.getFormaPagamento(),
                pedido.getValorTotal(),
                pedido.getEnderecoEntrega(),
                pedido.getObservacoes(),
                pedido.getCreatedAt(),
                itemResponses);
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return texto.trim();
    }

}
