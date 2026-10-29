package br.edu.unasp.comandadigital.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.entity.EstoqueMovimentacao;
import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.Pedido;
import br.edu.unasp.comandadigital.entity.PedidoItem;
import br.edu.unasp.comandadigital.entity.Prato;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.MotivoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.entity.enums.TipoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.EstoqueMovimentacaoRepository;
import br.edu.unasp.comandadigital.repository.IngredienteRepository;
import br.edu.unasp.comandadigital.repository.PedidoItemRepository;
import br.edu.unasp.comandadigital.service.NecessidadeEstoqueService.NecessidadeIngrediente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoquePedidoService {

    private final PedidoItemRepository pedidoItemRepository;
    private final EstoqueMovimentacaoRepository movimentacaoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final NecessidadeEstoqueService necessidadeEstoqueService;

    public EstoquePedidoService(
            PedidoItemRepository pedidoItemRepository,
            EstoqueMovimentacaoRepository movimentacaoRepository,
            IngredienteRepository ingredienteRepository,
            NecessidadeEstoqueService necessidadeEstoqueService) {
        this.pedidoItemRepository = pedidoItemRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.necessidadeEstoqueService = necessidadeEstoqueService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void baixarAoIniciarPreparo(Pedido pedido, Usuario usuario) {
        if (movimentacaoRepository.existsByPedidoIdAndTipoAndMotivo(
                pedido.getId(),
                TipoMovimentacaoEstoque.SAIDA,
                MotivoMovimentacaoEstoque.VENDA)) {
            return;
        }

        List<PedidoItem> itens = pedidoItemRepository.findByPedidoId(pedido.getId());
        if (itens.isEmpty()) {
            throw new RegraNegocioException(
                    "O pedido não possui itens para realizar a baixa de estoque");
        }
        Map<Long, Prato> pratosPorId = itens.stream()
                .map(PedidoItem::getPrato)
                .collect(Collectors.toMap(
                        Prato::getId,
                        Function.identity(),
                        (primeiro, ignorado) -> primeiro));
        Map<Long, Integer> quantidadesPorPrato = itens.stream()
                .collect(Collectors.toMap(
                        item -> item.getPrato().getId(),
                        PedidoItem::getQuantidade,
                        Integer::sum));
        Map<Long, NecessidadeIngrediente> necessidades = necessidadeEstoqueService.calcular(
                pratosPorId,
                quantidadesPorPrato);

        Map<Long, Ingrediente> ingredientesBloqueados = bloquearIngredientes(
                necessidades.keySet().stream().sorted().toList());
        necessidadeEstoqueService.validarDisponibilidade(necessidades);

        List<EstoqueMovimentacao> saidas = necessidades.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> EstoqueMovimentacao.saidaVenda(
                        ingredientesBloqueados.get(entry.getKey()),
                        entry.getValue().quantidade(),
                        pedido,
                        usuario))
                .toList();
        movimentacaoRepository.saveAllAndFlush(saidas);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void estornarAoCancelar(Pedido pedido, Usuario usuario) {
        List<EstoqueMovimentacao> saidas = movimentacaoRepository
                .findByPedidoIdAndTipoAndMotivoOrderByIngredienteId(
                        pedido.getId(),
                        TipoMovimentacaoEstoque.SAIDA,
                        MotivoMovimentacaoEstoque.VENDA);
        if (saidas.isEmpty()) return;

        List<EstoqueMovimentacao> estornosExistentes = movimentacaoRepository
                .findByPedidoIdAndTipoAndMotivoOrderByIngredienteId(
                        pedido.getId(),
                        TipoMovimentacaoEstoque.ESTORNO,
                        MotivoMovimentacaoEstoque.ESTORNO);
        if (!estornosExistentes.isEmpty()) {
            if (estornosExistentes.size() == saidas.size()) return;
            throw new RegraNegocioException(
                    "O estorno de estoque do pedido está inconsistente");
        }

        bloquearIngredientes(saidas.stream()
                .map(saida -> saida.getIngrediente().getId())
                .distinct()
                .sorted()
                .toList());
        List<EstoqueMovimentacao> estornos = saidas.stream()
                .map(saida -> EstoqueMovimentacao.estornoVenda(
                        saida,
                        pedido,
                        usuario))
                .toList();
        movimentacaoRepository.saveAllAndFlush(estornos);
    }

    private Map<Long, Ingrediente> bloquearIngredientes(List<Long> ingredienteIds) {
        List<Ingrediente> ingredientes = ingredienteRepository.findAllByIdInForUpdate(
                ingredienteIds);
        if (ingredientes.size() != ingredienteIds.size()) {
            throw new RegraNegocioException(
                    "Não foi possível bloquear todos os ingredientes do pedido");
        }
        return ingredientes.stream().collect(Collectors.toMap(
                Ingrediente::getId,
                Function.identity()));
    }
}
