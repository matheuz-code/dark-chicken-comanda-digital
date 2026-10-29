package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.dto.admin.estoque.IngredienteMovimentacaoResponse;
import br.edu.unasp.comandadigital.dto.admin.estoque.MovimentacaoEstoqueManualRequest;
import br.edu.unasp.comandadigital.dto.admin.estoque.MovimentacaoEstoqueResponse;
import br.edu.unasp.comandadigital.dto.admin.estoque.SaldoEstoqueResponse;
import br.edu.unasp.comandadigital.dto.admin.estoque.UsuarioMovimentacaoResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.EstoqueMovimentacao;
import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.MotivoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.TipoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.EstoqueMovimentacaoRepository;
import br.edu.unasp.comandadigital.repository.IngredienteRepository;
import br.edu.unasp.comandadigital.repository.SaldoEstoqueProjection;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminEstoqueService {

    private static final Sort ORDENACAO_INGREDIENTES = Sort.by(
            Sort.Order.asc("nome"),
            Sort.Order.asc("id"));
    private static final Sort ORDENACAO_MOVIMENTACOES = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));
    private static final Set<MotivoMovimentacaoEstoque> MOTIVOS_SAIDA_MANUAL = EnumSet.of(
            MotivoMovimentacaoEstoque.DESPERDICIO,
            MotivoMovimentacaoEstoque.VENCIMENTO,
            MotivoMovimentacaoEstoque.QUEBRA,
            MotivoMovimentacaoEstoque.USO_INTERNO,
            MotivoMovimentacaoEstoque.AJUSTE);

    private final IngredienteRepository ingredienteRepository;
    private final EstoqueMovimentacaoRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminEstoqueService(
            IngredienteRepository ingredienteRepository,
            EstoqueMovimentacaoRepository movimentacaoRepository,
            UsuarioRepository usuarioRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public PaginaResponse<SaldoEstoqueResponse> listarSaldos(
            StatusAtivacao status,
            Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO_INGREDIENTES);
        Page<Ingrediente> ingredientes = status == null
                ? ingredienteRepository.findAll(pageRequest)
                : ingredienteRepository.findByStatus(status, pageRequest);
        Map<Long, BigDecimal> saldos = calcularSaldos(ingredientes.getContent());
        return PaginaResponse.de(ingredientes.map(
                ingrediente -> toSaldoResponse(ingrediente, saldos.get(ingrediente.getId()))));
    }

    public PaginaResponse<SaldoEstoqueResponse> listarAlertas(Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize());
        Page<Long> ids = ingredienteRepository.findIdsAbaixoDoEstoqueMinimo(pageRequest);
        Map<Long, Ingrediente> ingredientes = ingredienteRepository.findAllById(ids.getContent())
                .stream()
                .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));
        Map<Long, BigDecimal> saldos = calcularSaldos(ingredientes.values().stream().toList());
        List<SaldoEstoqueResponse> conteudo = ids.getContent().stream()
                .map(ingredientes::get)
                .map(ingrediente -> toSaldoResponse(
                        ingrediente,
                        saldos.get(ingrediente.getId())))
                .toList();
        return PaginaResponse.de(new PageImpl<>(
                conteudo,
                pageRequest,
                ids.getTotalElements()));
    }

    public PaginaResponse<MovimentacaoEstoqueResponse> listarMovimentacoes(
            Long ingredienteId,
            TipoMovimentacaoEstoque tipo,
            MotivoMovimentacaoEstoque motivo,
            Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO_MOVIMENTACOES);
        Page<EstoqueMovimentacao> movimentacoes = movimentacaoRepository.buscarHistorico(
                ingredienteId,
                tipo,
                motivo,
                pageRequest);
        return PaginaResponse.de(movimentacoes.map(this::toMovimentacaoResponse));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimentacaoEstoqueResponse registrarSaidaManual(
            String emailUsuario,
            MovimentacaoEstoqueManualRequest request) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        validarMotivoSaidaManual(request.motivo());
        Ingrediente ingrediente = bloquearIngrediente(request.ingredienteId());
        if (ingrediente.getStatus() != StatusAtivacao.ATIVO) {
            throw new RegraNegocioException(
                    "Não é permitido movimentar um ingrediente inativo");
        }

        BigDecimal quantidade = request.quantidade().setScale(4, RoundingMode.UNNECESSARY);
        BigDecimal saldoAtual = calcularSaldo(ingrediente.getId());
        if (saldoAtual.compareTo(quantidade) < 0) {
            throw new RegraNegocioException(
                    "Estoque insuficiente para o ingrediente '"
                            + ingrediente.getNome()
                            + "': solicitado "
                            + quantidade.stripTrailingZeros().toPlainString()
                            + " " + ingrediente.getUnidadePadrao()
                            + ", disponível "
                            + saldoAtual.stripTrailingZeros().toPlainString()
                            + " " + ingrediente.getUnidadePadrao());
        }

        EstoqueMovimentacao movimentacao = EstoqueMovimentacao.saidaManual(
                ingrediente,
                quantidade,
                request.motivo(),
                normalizarLote(request.lote()),
                request.validade(),
                usuario);
        return toMovimentacaoResponse(movimentacaoRepository.saveAndFlush(movimentacao));
    }

    private Map<Long, BigDecimal> calcularSaldos(List<Ingrediente> ingredientes) {
        if (ingredientes.isEmpty()) return Map.of();
        return movimentacaoRepository.calcularSaldos(
                        ingredientes.stream().map(Ingrediente::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        SaldoEstoqueProjection::getIngredienteId,
                        SaldoEstoqueProjection::getSaldo));
    }

    private BigDecimal calcularSaldo(Long ingredienteId) {
        return movimentacaoRepository.calcularSaldos(List.of(ingredienteId))
                .stream()
                .findFirst()
                .map(SaldoEstoqueProjection::getSaldo)
                .orElse(BigDecimal.ZERO);
    }

    private SaldoEstoqueResponse toSaldoResponse(
            Ingrediente ingrediente,
            BigDecimal saldo) {
        BigDecimal saldoAtual = saldo == null ? BigDecimal.ZERO : saldo;
        return new SaldoEstoqueResponse(
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getSku(),
                ingrediente.getUnidadePadrao(),
                saldoAtual,
                ingrediente.getEstoqueMinimo(),
                saldoAtual.compareTo(ingrediente.getEstoqueMinimo()) < 0,
                ingrediente.getStatus());
    }

    private MovimentacaoEstoqueResponse toMovimentacaoResponse(
            EstoqueMovimentacao movimentacao) {
        Ingrediente ingrediente = movimentacao.getIngrediente();
        Usuario usuario = movimentacao.getUsuario();
        return new MovimentacaoEstoqueResponse(
                movimentacao.getId(),
                new IngredienteMovimentacaoResponse(
                        ingrediente.getId(),
                        ingrediente.getNome(),
                        ingrediente.getSku(),
                        ingrediente.getUnidadePadrao()),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getMotivo(),
                movimentacao.getLote(),
                movimentacao.getValidade(),
                movimentacao.getCustoUnitario(),
                movimentacao.getPedido() == null ? null : movimentacao.getPedido().getId(),
                movimentacao.getPedidoCompra() == null
                        ? null
                        : movimentacao.getPedidoCompra().getId(),
                new UsuarioMovimentacaoResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getPerfil()),
                movimentacao.getCreatedAt());
    }

    private Ingrediente bloquearIngrediente(Long ingredienteId) {
        return ingredienteRepository.findAllByIdInForUpdate(List.of(ingredienteId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ingrediente não encontrado"));
    }

    private Usuario carregarUsuarioInterno(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .filter(usuario -> usuario.getStatus() == StatusAtivacao.ATIVO)
                .filter(usuario -> usuario.getPerfil() != PerfilUsuario.CLIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário interno autenticado não encontrado ou inativo"));
    }

    private void validarMotivoSaidaManual(MotivoMovimentacaoEstoque motivo) {
        if (!MOTIVOS_SAIDA_MANUAL.contains(motivo)) {
            throw new RegraNegocioException(
                    "O motivo " + motivo + " não é permitido para uma saída manual");
        }
    }

    private String normalizarLote(String lote) {
        return lote == null || lote.isBlank() ? null : lote.trim();
    }
}
