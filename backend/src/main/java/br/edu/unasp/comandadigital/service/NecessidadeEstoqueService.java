package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.entity.FichaTecnica;
import br.edu.unasp.comandadigital.entity.FichaTecnicaItem;
import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.Prato;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.EstoqueMovimentacaoRepository;
import br.edu.unasp.comandadigital.repository.FichaTecnicaItemRepository;
import br.edu.unasp.comandadigital.repository.FichaTecnicaRepository;
import br.edu.unasp.comandadigital.repository.SaldoEstoqueProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NecessidadeEstoqueService {

    private static final int ESCALA_ESTOQUE = 4;

    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final FichaTecnicaItemRepository fichaTecnicaItemRepository;
    private final EstoqueMovimentacaoRepository estoqueMovimentacaoRepository;
    private final ConversorUnidadeService conversorUnidadeService;

    public NecessidadeEstoqueService(
            FichaTecnicaRepository fichaTecnicaRepository,
            FichaTecnicaItemRepository fichaTecnicaItemRepository,
            EstoqueMovimentacaoRepository estoqueMovimentacaoRepository,
            ConversorUnidadeService conversorUnidadeService) {
        this.fichaTecnicaRepository = fichaTecnicaRepository;
        this.fichaTecnicaItemRepository = fichaTecnicaItemRepository;
        this.estoqueMovimentacaoRepository = estoqueMovimentacaoRepository;
        this.conversorUnidadeService = conversorUnidadeService;
    }

    public Map<Long, NecessidadeIngrediente> calcular(
            Map<Long, Prato> pratosPorId,
            Map<Long, Integer> quantidadesPorPrato) {
        Map<Long, FichaTecnica> fichasPorPrato = fichaTecnicaRepository
                .findByPratoIdIn(pratosPorId.keySet())
                .stream()
                .collect(Collectors.toMap(
                        ficha -> ficha.getPrato().getId(),
                        Function.identity()));

        for (Prato prato : pratosPorId.values()) {
            if (!fichasPorPrato.containsKey(prato.getId())) {
                throw new RegraNegocioException(
                        "O prato '" + prato.getNome() + "' não possui ficha técnica");
            }
        }

        List<FichaTecnicaItem> fichaItens = fichaTecnicaItemRepository
                .findByFichaTecnicaIdInAndStatus(
                        fichasPorPrato.values().stream().map(FichaTecnica::getId).toList(),
                        StatusAtivacao.ATIVO);
        Map<Long, List<FichaTecnicaItem>> itensPorFicha = fichaItens.stream()
                .collect(Collectors.groupingBy(item -> item.getFichaTecnica().getId()));
        Map<Long, NecessidadeIngrediente> necessidades = new LinkedHashMap<>();

        for (Map.Entry<Long, Integer> entry : quantidadesPorPrato.entrySet()) {
            Prato prato = pratosPorId.get(entry.getKey());
            if (prato == null) {
                throw new RegraNegocioException(
                        "O prato " + entry.getKey() + " não está disponível para cálculo de estoque");
            }
            FichaTecnica ficha = fichasPorPrato.get(entry.getKey());
            List<FichaTecnicaItem> componentes = itensPorFicha.getOrDefault(
                    ficha.getId(),
                    List.of());
            if (componentes.isEmpty()) {
                throw new RegraNegocioException(
                        "O prato '" + prato.getNome() + "' possui ficha técnica sem ingredientes");
            }

            for (FichaTecnicaItem componente : componentes) {
                Ingrediente ingrediente = componente.getIngrediente();
                if (ingrediente.getStatus() != StatusAtivacao.ATIVO) {
                    throw new RegraNegocioException(
                            "O ingrediente '" + ingrediente.getNome() + "' está inativo");
                }

                BigDecimal quantidadeNecessaria = componente.getQuantidade()
                        .multiply(componente.getFatorCorrecao())
                        .multiply(BigDecimal.valueOf(entry.getValue()))
                        .divide(
                                BigDecimal.valueOf(ficha.getRendimento()),
                                ESCALA_ESTOQUE + 4,
                                RoundingMode.CEILING);
                quantidadeNecessaria = conversorUnidadeService.converter(
                                quantidadeNecessaria,
                                componente.getUnidade(),
                                ingrediente.getUnidadePadrao())
                        .setScale(ESCALA_ESTOQUE, RoundingMode.CEILING);

                necessidades.merge(
                        ingrediente.getId(),
                        new NecessidadeIngrediente(ingrediente, quantidadeNecessaria),
                        (atual, nova) -> new NecessidadeIngrediente(
                                atual.ingrediente(),
                                atual.quantidade().add(nova.quantidade())
                                        .setScale(ESCALA_ESTOQUE, RoundingMode.CEILING)));
            }
        }
        return Collections.unmodifiableMap(necessidades);
    }

    public void validarDisponibilidade(
            Map<Long, NecessidadeIngrediente> necessidades) {
        Map<Long, BigDecimal> saldos = estoqueMovimentacaoRepository
                .calcularSaldos(necessidades.keySet())
                .stream()
                .collect(Collectors.toMap(
                        SaldoEstoqueProjection::getIngredienteId,
                        SaldoEstoqueProjection::getSaldo));

        for (Map.Entry<Long, NecessidadeIngrediente> entry : necessidades.entrySet()) {
            BigDecimal saldo = saldos.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            NecessidadeIngrediente necessidade = entry.getValue();
            if (saldo.compareTo(necessidade.quantidade()) < 0) {
                throw new RegraNegocioException(
                        "Estoque insuficiente para o ingrediente '"
                                + necessidade.ingrediente().getNome()
                                + "': necessário "
                                + necessidade.quantidade().stripTrailingZeros().toPlainString()
                                + " " + necessidade.ingrediente().getUnidadePadrao()
                                + ", disponível " + saldo.stripTrailingZeros().toPlainString()
                                + " " + necessidade.ingrediente().getUnidadePadrao());
            }
        }
    }

    public record NecessidadeIngrediente(
            Ingrediente ingrediente,
            BigDecimal quantidade) {
    }
}
