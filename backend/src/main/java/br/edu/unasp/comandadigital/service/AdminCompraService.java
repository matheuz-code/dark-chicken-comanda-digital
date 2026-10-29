package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraDetalheResponse;
import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraItemRequest;
import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraItemResponse;
import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraRequest;
import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraResumoResponse;
import br.edu.unasp.comandadigital.dto.admin.compra.ReceberPedidoCompraRequest;
import br.edu.unasp.comandadigital.dto.admin.compra.RecebimentoItemRequest;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.EstoqueMovimentacao;
import br.edu.unasp.comandadigital.entity.Fornecedor;
import br.edu.unasp.comandadigital.entity.FornecedorProduto;
import br.edu.unasp.comandadigital.entity.FornecedorProdutoPrecoHistorico;
import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.PedidoCompra;
import br.edu.unasp.comandadigital.entity.PedidoCompraItem;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.StatusPedidoCompra;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.EstoqueMovimentacaoRepository;
import br.edu.unasp.comandadigital.repository.FornecedorProdutoPrecoHistoricoRepository;
import br.edu.unasp.comandadigital.repository.FornecedorProdutoRepository;
import br.edu.unasp.comandadigital.repository.FornecedorRepository;
import br.edu.unasp.comandadigital.repository.IngredienteRepository;
import br.edu.unasp.comandadigital.repository.PedidoCompraItemRepository;
import br.edu.unasp.comandadigital.repository.PedidoCompraRepository;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminCompraService {

    private static final Sort ORDENACAO = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final PedidoCompraRepository pedidoCompraRepository;
    private final PedidoCompraItemRepository itemRepository;
    private final FornecedorRepository fornecedorRepository;
    private final FornecedorProdutoRepository fornecedorProdutoRepository;
    private final FornecedorProdutoPrecoHistoricoRepository historicoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final EstoqueMovimentacaoRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConversorUnidadeService conversorUnidadeService;

    public AdminCompraService(
            PedidoCompraRepository pedidoCompraRepository,
            PedidoCompraItemRepository itemRepository,
            FornecedorRepository fornecedorRepository,
            FornecedorProdutoRepository fornecedorProdutoRepository,
            FornecedorProdutoPrecoHistoricoRepository historicoRepository,
            IngredienteRepository ingredienteRepository,
            EstoqueMovimentacaoRepository movimentacaoRepository,
            UsuarioRepository usuarioRepository,
            ConversorUnidadeService conversorUnidadeService) {
        this.pedidoCompraRepository = pedidoCompraRepository;
        this.itemRepository = itemRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
        this.historicoRepository = historicoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.conversorUnidadeService = conversorUnidadeService;
    }

    public PaginaResponse<PedidoCompraResumoResponse> listar(
            StatusPedidoCompra status,
            Long fornecedorId,
            Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO);
        Page<PedidoCompra> pedidos = pedidoCompraRepository.buscar(
                status,
                fornecedorId,
                pageRequest);
        return PaginaResponse.de(pedidos.map(this::toResumoResponse));
    }

    public PedidoCompraDetalheResponse detalhar(Long id) {
        PedidoCompra pedido = pedidoCompraRepository.findDetalhadoById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido de compra não encontrado"));
        return toDetalheResponse(pedido, buscarItensAtivos(id));
    }

    @Transactional
    public PedidoCompraDetalheResponse criar(
            String emailUsuario,
            PedidoCompraRequest request) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        Fornecedor fornecedor = buscarFornecedorAtivo(request.fornecedorId());
        validarItensSemDuplicidade(request.itens());

        PedidoCompra pedido = pedidoCompraRepository.saveAndFlush(
                PedidoCompra.criar(fornecedor, usuario));
        List<PedidoCompraItem> itens = criarItens(pedido, fornecedor, request.itens());
        itemRepository.saveAllAndFlush(itens);
        pedido.setValorTotal(calcularTotal(itens));
        pedido = pedidoCompraRepository.saveAndFlush(pedido);
        return toDetalheResponse(pedido, itens);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PedidoCompraDetalheResponse atualizar(
            Long id,
            PedidoCompraRequest request) {
        PedidoCompra pedido = bloquearPedido(id);
        exigirStatus(pedido, StatusPedidoCompra.RASCUNHO,
                "Somente pedidos de compra em rascunho podem ser alterados");
        Fornecedor fornecedor = buscarFornecedorAtivo(request.fornecedorId());
        validarItensSemDuplicidade(request.itens());

        List<PedidoCompraItem> existentes = itemRepository
                .findByPedidoCompraIdOrderById(id);
        Map<Long, PedidoCompraItem> itemPorIngrediente = existentes.stream()
                .collect(Collectors.toMap(
                        item -> item.getIngrediente().getId(),
                        Function.identity()));
        existentes.forEach(item -> item.setStatus(StatusAtivacao.INATIVO));

        List<PedidoCompraItem> ativos = new ArrayList<>();
        for (PedidoCompraItemRequest itemRequest : request.itens()) {
            FornecedorProduto catalogo = buscarItemCatalogoAtivo(
                    fornecedor.getId(),
                    itemRequest.ingredienteId());
            PedidoCompraItem item = itemPorIngrediente.get(itemRequest.ingredienteId());
            BigDecimal subtotal = calcularSubtotal(
                    itemRequest.quantidade(),
                    itemRequest.precoUnitario());
            if (item == null) {
                item = PedidoCompraItem.criar(
                        pedido,
                        catalogo.getIngrediente(),
                        itemRequest.quantidade(),
                        itemRequest.precoUnitario(),
                        catalogo.getUnidadeVenda(),
                        subtotal);
                existentes.add(item);
            } else {
                item.setQuantidade(itemRequest.quantidade());
                item.setPrecoUnitario(itemRequest.precoUnitario());
                item.setUnidadeCompra(catalogo.getUnidadeVenda());
                item.setSubtotal(subtotal);
                item.setStatus(StatusAtivacao.ATIVO);
            }
            ativos.add(item);
        }

        pedido.setFornecedor(fornecedor);
        pedido.setValorTotal(calcularTotal(ativos));
        itemRepository.saveAllAndFlush(existentes);
        pedido = pedidoCompraRepository.saveAndFlush(pedido);
        return toDetalheResponse(pedido, ativos);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelar(Long id) {
        PedidoCompra pedido = bloquearPedido(id);
        if (pedido.getStatus() == StatusPedidoCompra.RECEBIDO) {
            throw new RegraNegocioException(
                    "Um pedido de compra recebido não pode ser cancelado");
        }
        if (pedido.getStatus() != StatusPedidoCompra.CANCELADO) {
            pedido.setStatus(StatusPedidoCompra.CANCELADO);
            pedidoCompraRepository.saveAndFlush(pedido);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PedidoCompraDetalheResponse enviar(Long id) {
        PedidoCompra pedido = bloquearPedido(id);
        exigirStatus(pedido, StatusPedidoCompra.RASCUNHO,
                "Somente pedidos de compra em rascunho podem ser enviados");
        List<PedidoCompraItem> itens = buscarItensAtivos(id);
        if (itens.isEmpty()) {
            throw new RegraNegocioException(
                    "Não é possível enviar um pedido de compra sem itens");
        }
        pedido.setStatus(StatusPedidoCompra.ENVIADO);
        pedido = pedidoCompraRepository.saveAndFlush(pedido);
        return toDetalheResponse(pedido, itens);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PedidoCompraDetalheResponse receber(
            String emailUsuario,
            Long id,
            ReceberPedidoCompraRequest request) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        PedidoCompra pedido = bloquearPedido(id);
        exigirStatus(pedido, StatusPedidoCompra.ENVIADO,
                "Somente pedidos de compra enviados podem ser recebidos");
        List<PedidoCompraItem> itens = buscarItensAtivos(id);
        if (itens.isEmpty()) {
            throw new RegraNegocioException(
                    "Não é possível receber um pedido de compra sem itens");
        }

        Map<Long, RecebimentoItemRequest> dadosRecebimento = mapearDadosRecebimento(
                request,
                itens);
        List<Long> ingredienteIds = itens.stream()
                .map(item -> item.getIngrediente().getId())
                .sorted()
                .toList();
        Map<Long, Ingrediente> ingredientes = ingredienteRepository
                .findAllByIdInForUpdate(ingredienteIds)
                .stream()
                .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));
        if (ingredientes.size() != ingredienteIds.size()) {
            throw new RecursoNaoEncontradoException(
                    "Um dos ingredientes do pedido de compra não foi encontrado");
        }

        List<EstoqueMovimentacao> movimentacoes = new ArrayList<>();
        for (PedidoCompraItem item : itens) {
            Ingrediente ingrediente = ingredientes.get(item.getIngrediente().getId());
            BigDecimal quantidadeEstoque = converterQuantidadeParaEstoque(item, ingrediente);
            BigDecimal custoNaUnidadePadrao = normalizarPreco(
                    item.getPrecoUnitario(),
                    item.getUnidadeCompra(),
                    ingrediente.getUnidadePadrao());
            RecebimentoItemRequest dados = dadosRecebimento.get(ingrediente.getId());
            String lote = dados == null || dados.lote() == null || dados.lote().isBlank()
                    ? "COMPRA-" + pedido.getId()
                    : dados.lote().trim();
            movimentacoes.add(EstoqueMovimentacao.entradaCompra(
                    ingrediente,
                    quantidadeEstoque,
                    custoNaUnidadePadrao,
                    pedido,
                    lote,
                    dados == null ? null : dados.validade(),
                    usuario));
            ingrediente.setCustoUnitario(custoNaUnidadePadrao);
            atualizarPrecoCatalogo(pedido, item, usuario);
        }

        ingredienteRepository.saveAll(ingredientes.values());
        movimentacaoRepository.saveAllAndFlush(movimentacoes);
        pedido.setStatus(StatusPedidoCompra.RECEBIDO);
        pedido = pedidoCompraRepository.saveAndFlush(pedido);
        return toDetalheResponse(pedido, itens);
    }

    private List<PedidoCompraItem> criarItens(
            PedidoCompra pedido,
            Fornecedor fornecedor,
            List<PedidoCompraItemRequest> requests) {
        List<PedidoCompraItem> itens = new ArrayList<>();
        for (PedidoCompraItemRequest request : requests) {
            FornecedorProduto catalogo = buscarItemCatalogoAtivo(
                    fornecedor.getId(),
                    request.ingredienteId());
            itens.add(PedidoCompraItem.criar(
                    pedido,
                    catalogo.getIngrediente(),
                    request.quantidade(),
                    request.precoUnitario(),
                    catalogo.getUnidadeVenda(),
                    calcularSubtotal(request.quantidade(), request.precoUnitario())));
        }
        return itens;
    }

    private void atualizarPrecoCatalogo(
            PedidoCompra pedido,
            PedidoCompraItem item,
            Usuario usuario) {
        FornecedorProduto catalogo = fornecedorProdutoRepository
                .findByFornecedorIdAndIngredienteId(
                        pedido.getFornecedor().getId(),
                        item.getIngrediente().getId())
                .orElseThrow(() -> new RegraNegocioException(
                        "O item do fornecedor não existe mais no catálogo"));
        boolean alterou = catalogo.getPreco().compareTo(item.getPrecoUnitario()) != 0
                || catalogo.getUnidadeVenda() != item.getUnidadeCompra();
        if (alterou) {
            catalogo.setPreco(item.getPrecoUnitario());
            catalogo.setUnidadeVenda(item.getUnidadeCompra());
            fornecedorProdutoRepository.save(catalogo);
            historicoRepository.save(FornecedorProdutoPrecoHistorico.registrar(
                    catalogo,
                    catalogo.getPreco(),
                    catalogo.getUnidadeVenda(),
                    usuario));
        }
    }

    private Map<Long, RecebimentoItemRequest> mapearDadosRecebimento(
            ReceberPedidoCompraRequest request,
            List<PedidoCompraItem> itens) {
        Set<Long> idsPedido = itens.stream()
                .map(item -> item.getIngrediente().getId())
                .collect(Collectors.toSet());
        Map<Long, RecebimentoItemRequest> resultado = new HashMap<>();
        for (RecebimentoItemRequest item : request.itens()) {
            if (!idsPedido.contains(item.ingredienteId())) {
                throw new RegraNegocioException(
                        "Os dados de recebimento contêm ingrediente que não pertence ao pedido");
            }
            if (resultado.put(item.ingredienteId(), item) != null) {
                throw new RegraNegocioException(
                        "O ingrediente está duplicado nos dados de recebimento");
            }
        }
        return resultado;
    }

    private BigDecimal converterQuantidadeParaEstoque(
            PedidoCompraItem item,
            Ingrediente ingrediente) {
        BigDecimal convertido = conversorUnidadeService.converter(
                item.getQuantidade(),
                item.getUnidadeCompra(),
                ingrediente.getUnidadePadrao())
                .setScale(4, RoundingMode.HALF_UP);
        if (convertido.signum() <= 0 || convertido.precision() - convertido.scale() > 10) {
            throw new RegraNegocioException(
                    "A quantidade convertida do ingrediente '"
                            + ingrediente.getNome()
                            + "' não cabe no estoque");
        }
        return convertido;
    }

    private BigDecimal normalizarPreco(
            BigDecimal preco,
            UnidadeMedida unidadeCompra,
            UnidadeMedida unidadePadrao) {
        BigDecimal quantidadePadrao = conversorUnidadeService.converter(
                BigDecimal.ONE,
                unidadeCompra,
                unidadePadrao);
        return preco.divide(quantidadePadrao, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularSubtotal(BigDecimal quantidade, BigDecimal preco) {
        return quantidade.multiply(preco).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTotal(List<PedidoCompraItem> itens) {
        return itens.stream()
                .map(PedidoCompraItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validarItensSemDuplicidade(List<PedidoCompraItemRequest> itens) {
        Set<Long> ids = new HashSet<>();
        for (PedidoCompraItemRequest item : itens) {
            if (!ids.add(item.ingredienteId())) {
                throw new RegraNegocioException(
                        "O pedido de compra não pode conter ingredientes duplicados");
            }
        }
    }

    private PedidoCompra bloquearPedido(Long id) {
        return pedidoCompraRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido de compra não encontrado"));
    }

    private List<PedidoCompraItem> buscarItensAtivos(Long pedidoId) {
        return itemRepository.findByPedidoCompraIdAndStatusOrderById(
                pedidoId,
                StatusAtivacao.ATIVO);
    }

    private Fornecedor buscarFornecedorAtivo(Long id) {
        return fornecedorRepository.findById(id)
                .filter(fornecedor -> fornecedor.getStatus() == StatusAtivacao.ATIVO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Fornecedor não encontrado ou inativo"));
    }

    private FornecedorProduto buscarItemCatalogoAtivo(
            Long fornecedorId,
            Long ingredienteId) {
        return fornecedorProdutoRepository
                .findByFornecedorIdAndIngredienteId(fornecedorId, ingredienteId)
                .filter(produto -> produto.getStatus() == StatusAtivacao.ATIVO)
                .filter(produto -> produto.getIngrediente().getStatus() == StatusAtivacao.ATIVO)
                .orElseThrow(() -> new RegraNegocioException(
                        "O ingrediente " + ingredienteId
                                + " não está ativo no catálogo do fornecedor"));
    }

    private Usuario carregarUsuarioInterno(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .filter(usuario -> usuario.getStatus() == StatusAtivacao.ATIVO)
                .filter(usuario -> usuario.getPerfil() != PerfilUsuario.CLIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário interno autenticado não encontrado ou inativo"));
    }

    private void exigirStatus(
            PedidoCompra pedido,
            StatusPedidoCompra esperado,
            String mensagem) {
        if (pedido.getStatus() != esperado) {
            throw new RegraNegocioException(mensagem);
        }
    }

    private PedidoCompraResumoResponse toResumoResponse(PedidoCompra pedido) {
        return new PedidoCompraResumoResponse(
                pedido.getId(),
                pedido.getFornecedor().getId(),
                pedido.getFornecedor().getRazaoSocial(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                pedido.getUsuario().getId(),
                pedido.getUsuario().getNome(),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt());
    }

    private PedidoCompraDetalheResponse toDetalheResponse(
            PedidoCompra pedido,
            List<PedidoCompraItem> itens) {
        return new PedidoCompraDetalheResponse(
                pedido.getId(),
                pedido.getFornecedor().getId(),
                pedido.getFornecedor().getRazaoSocial(),
                pedido.getFornecedor().getCnpj(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                pedido.getUsuario().getId(),
                pedido.getUsuario().getNome(),
                itens.stream().map(this::toItemResponse).toList(),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt());
    }

    private PedidoCompraItemResponse toItemResponse(PedidoCompraItem item) {
        Ingrediente ingrediente = item.getIngrediente();
        return new PedidoCompraItemResponse(
                item.getId(),
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getSku(),
                item.getQuantidade(),
                item.getUnidadeCompra(),
                item.getPrecoUnitario(),
                item.getSubtotal());
    }
}
