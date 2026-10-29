package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.dto.admin.ClassificacaoFoodCost;
import br.edu.unasp.comandadigital.dto.admin.CustoPratoResponse;
import br.edu.unasp.comandadigital.dto.admin.FichaTecnicaItemRequest;
import br.edu.unasp.comandadigital.dto.admin.FichaTecnicaItemResponse;
import br.edu.unasp.comandadigital.dto.admin.FichaTecnicaRequest;
import br.edu.unasp.comandadigital.dto.admin.FichaTecnicaResponse;
import br.edu.unasp.comandadigital.dto.admin.IngredienteFichaResponse;
import br.edu.unasp.comandadigital.entity.FichaTecnica;
import br.edu.unasp.comandadigital.entity.FichaTecnicaItem;
import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.Prato;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.exception.ConflitoException;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.FichaTecnicaItemRepository;
import br.edu.unasp.comandadigital.repository.FichaTecnicaRepository;
import br.edu.unasp.comandadigital.repository.IngredienteRepository;
import br.edu.unasp.comandadigital.repository.PratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminFichaTecnicaService {

    private static final int ESCALA_CUSTO = 4;
    private static final BigDecimal LIMITE_VERDE = new BigDecimal("30.00");
    private static final BigDecimal LIMITE_AMARELO = new BigDecimal("35.00");
    private static final String AVISO_FOOD_COST =
            "Food cost acima de 35%; revise o preço de venda ou o custo dos ingredientes";

    private final PratoRepository pratoRepository;
    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final FichaTecnicaItemRepository fichaTecnicaItemRepository;
    private final IngredienteRepository ingredienteRepository;
    private final ConversorUnidadeService conversorUnidadeService;

    public AdminFichaTecnicaService(
            PratoRepository pratoRepository,
            FichaTecnicaRepository fichaTecnicaRepository,
            FichaTecnicaItemRepository fichaTecnicaItemRepository,
            IngredienteRepository ingredienteRepository,
            ConversorUnidadeService conversorUnidadeService) {
        this.pratoRepository = pratoRepository;
        this.fichaTecnicaRepository = fichaTecnicaRepository;
        this.fichaTecnicaItemRepository = fichaTecnicaItemRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.conversorUnidadeService = conversorUnidadeService;
    }

    public FichaTecnicaResponse buscar(Long pratoId) {
        Prato prato = buscarPrato(pratoId);
        FichaTecnica ficha = buscarFicha(pratoId);
        return montarFichaResponse(prato, ficha, carregarItensAtivos(ficha.getId()));
    }

    public CustoPratoResponse calcularCusto(Long pratoId) {
        Prato prato = buscarPrato(pratoId);
        FichaTecnica ficha = buscarFicha(pratoId);
        CalculoFicha calculo = calcular(ficha, carregarItensAtivos(ficha.getId()));
        return new CustoPratoResponse(
                prato.getId(),
                prato.getNome(),
                prato.getPrecoVenda(),
                calculo.custoUnitario(),
                calculo.foodCostPercentual(),
                calculo.classificacao(),
                calculo.aviso());
    }

    @Transactional
    public FichaTecnicaResponse criar(Long pratoId, FichaTecnicaRequest request) {
        Prato prato = buscarPrato(pratoId);
        if (fichaTecnicaRepository.findByPratoId(pratoId).isPresent()) {
            throw new ConflitoException("O prato já possui ficha técnica");
        }
        Map<Long, Ingrediente> ingredientes = carregarIngredientesAtivos(request.itens());
        validarUnidades(request.itens(), ingredientes);

        FichaTecnica novaFicha = FichaTecnica.criar(
                prato,
                request.rendimento(),
                request.modoPreparo().trim());
        FichaTecnica ficha = fichaTecnicaRepository.saveAndFlush(novaFicha);

        List<FichaTecnicaItem> itens = request.itens().stream()
                .map(item -> FichaTecnicaItem.criar(
                        ficha,
                        ingredientes.get(item.ingredienteId()),
                        item.quantidade(),
                        item.unidade(),
                        item.fatorCorrecao()))
                .toList();
        fichaTecnicaItemRepository.saveAllAndFlush(itens);
        return montarFichaResponse(prato, ficha, itens);
    }

    @Transactional
    public FichaTecnicaResponse atualizar(Long pratoId, FichaTecnicaRequest request) {
        Prato prato = buscarPrato(pratoId);
        FichaTecnica ficha = buscarFicha(pratoId);
        Map<Long, Ingrediente> ingredientes = carregarIngredientesAtivos(request.itens());
        validarUnidades(request.itens(), ingredientes);

        ficha.setRendimento(request.rendimento());
        ficha.setModoPreparo(request.modoPreparo().trim());
        fichaTecnicaRepository.saveAndFlush(ficha);
        sincronizarItens(ficha, request.itens(), ingredientes);

        return montarFichaResponse(prato, ficha, carregarItensAtivos(ficha.getId()));
    }

    private void sincronizarItens(
            FichaTecnica ficha,
            List<FichaTecnicaItemRequest> requests,
            Map<Long, Ingrediente> ingredientes) {
        List<FichaTecnicaItem> existentes = fichaTecnicaItemRepository
                .findByFichaTecnicaId(ficha.getId());
        Map<Long, FichaTecnicaItem> porIngrediente = existentes.stream()
                .collect(Collectors.toMap(
                        item -> item.getIngrediente().getId(),
                        Function.identity()));
        Set<Long> solicitados = requests.stream()
                .map(FichaTecnicaItemRequest::ingredienteId)
                .collect(Collectors.toSet());

        existentes.stream()
                .filter(item -> !solicitados.contains(item.getIngrediente().getId()))
                .forEach(item -> item.setStatus(StatusAtivacao.INATIVO));

        for (FichaTecnicaItemRequest request : requests) {
            FichaTecnicaItem item = porIngrediente.get(request.ingredienteId());
            if (item == null) {
                item = FichaTecnicaItem.criar(
                        ficha,
                        ingredientes.get(request.ingredienteId()),
                        request.quantidade(),
                        request.unidade(),
                        request.fatorCorrecao());
                existentes.add(item);
            } else {
                item.setQuantidade(request.quantidade());
                item.setUnidade(request.unidade());
                item.setFatorCorrecao(request.fatorCorrecao());
                item.setStatus(StatusAtivacao.ATIVO);
            }
        }
        fichaTecnicaItemRepository.saveAllAndFlush(existentes);
    }

    private Map<Long, Ingrediente> carregarIngredientesAtivos(
            List<FichaTecnicaItemRequest> itens) {
        Map<Long, FichaTecnicaItemRequest> itensUnicos = new LinkedHashMap<>();
        for (FichaTecnicaItemRequest item : itens) {
            if (itensUnicos.putIfAbsent(item.ingredienteId(), item) != null) {
                throw new RegraNegocioException(
                        "Cada ingrediente deve aparecer somente uma vez na ficha técnica");
            }
        }

        Map<Long, Ingrediente> ingredientes = ingredienteRepository.findByIdInAndStatus(
                        itensUnicos.keySet(),
                        StatusAtivacao.ATIVO)
                .stream()
                .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));
        if (ingredientes.size() != itensUnicos.size()) {
            Long idIndisponivel = itensUnicos.keySet().stream()
                    .filter(id -> !ingredientes.containsKey(id))
                    .findFirst()
                    .orElseThrow();
            throw new RegraNegocioException(
                    "O ingrediente " + idIndisponivel + " não existe ou está inativo");
        }
        return ingredientes;
    }

    private void validarUnidades(
            List<FichaTecnicaItemRequest> itens,
            Map<Long, Ingrediente> ingredientes) {
        for (FichaTecnicaItemRequest item : itens) {
            Ingrediente ingrediente = ingredientes.get(item.ingredienteId());
            conversorUnidadeService.converter(
                    item.quantidade(),
                    item.unidade(),
                    ingrediente.getUnidadePadrao());
        }
    }

    private FichaTecnicaResponse montarFichaResponse(
            Prato prato,
            FichaTecnica ficha,
            List<FichaTecnicaItem> itens) {
        CalculoFicha calculo = calcular(ficha, itens);
        Map<Long, BigDecimal> custos = calculo.custosComponentes();
        List<FichaTecnicaItemResponse> itemResponses = itens.stream()
                .sorted(Comparator.comparing(item -> item.getIngrediente().getNome()))
                .map(item -> {
                    Ingrediente ingrediente = item.getIngrediente();
                    return new FichaTecnicaItemResponse(
                            item.getId(),
                            new IngredienteFichaResponse(
                                    ingrediente.getId(),
                                    ingrediente.getNome(),
                                    ingrediente.getSku(),
                                    ingrediente.getUnidadePadrao(),
                                    ingrediente.getCustoUnitario()),
                            item.getQuantidade(),
                            item.getUnidade(),
                            item.getFatorCorrecao(),
                            custos.get(item.getId()));
                })
                .toList();

        return new FichaTecnicaResponse(
                ficha.getId(),
                prato.getId(),
                prato.getNome(),
                ficha.getRendimento(),
                ficha.getModoPreparo(),
                calculo.custoUnitario(),
                calculo.foodCostPercentual(),
                calculo.classificacao(),
                calculo.aviso(),
                itemResponses,
                ficha.getCreatedAt(),
                ficha.getUpdatedAt());
    }

    private CalculoFicha calcular(
            FichaTecnica ficha,
            List<FichaTecnicaItem> itens) {
        if (itens.isEmpty()) {
            throw new RegraNegocioException(
                    "A ficha técnica precisa possuir pelo menos um ingrediente ativo");
        }
        BigDecimal rendimento = BigDecimal.valueOf(ficha.getRendimento());
        BigDecimal custoTotalReceita = BigDecimal.ZERO;
        Map<Long, BigDecimal> custosComponentes = new LinkedHashMap<>();

        for (FichaTecnicaItem item : itens) {
            Ingrediente ingrediente = item.getIngrediente();
            BigDecimal quantidadePadrao = conversorUnidadeService.converter(
                    item.getQuantidade(),
                    item.getUnidade(),
                    ingrediente.getUnidadePadrao());
            BigDecimal custoReceita = quantidadePadrao
                    .multiply(item.getFatorCorrecao())
                    .multiply(ingrediente.getCustoUnitario());
            custoTotalReceita = custoTotalReceita.add(custoReceita);
            custosComponentes.put(
                    item.getId(),
                    custoReceita.divide(rendimento, ESCALA_CUSTO, RoundingMode.HALF_UP));
        }

        BigDecimal custoUnitario = custoTotalReceita
                .divide(rendimento, ESCALA_CUSTO, RoundingMode.HALF_UP);
        Prato prato = ficha.getPrato();
        ResultadoFoodCost foodCost = classificarFoodCost(custoUnitario, prato.getPrecoVenda());
        return new CalculoFicha(
                custoUnitario,
                foodCost.percentual(),
                foodCost.classificacao(),
                foodCost.aviso(),
                custosComponentes);
    }

    private ResultadoFoodCost classificarFoodCost(
            BigDecimal custoUnitario,
            BigDecimal precoVenda) {
        if (precoVenda == null || precoVenda.compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultadoFoodCost(
                    null,
                    ClassificacaoFoodCost.SEM_PRECO,
                    "Defina um preço de venda maior que zero para calcular o food cost");
        }
        BigDecimal percentual = custoUnitario
                .multiply(BigDecimal.valueOf(100))
                .divide(precoVenda, 2, RoundingMode.HALF_UP);
        if (percentual.compareTo(LIMITE_VERDE) <= 0) {
            return new ResultadoFoodCost(percentual, ClassificacaoFoodCost.VERDE, null);
        }
        if (percentual.compareTo(LIMITE_AMARELO) <= 0) {
            return new ResultadoFoodCost(percentual, ClassificacaoFoodCost.AMARELO, null);
        }
        return new ResultadoFoodCost(
                percentual,
                ClassificacaoFoodCost.VERMELHO,
                AVISO_FOOD_COST);
    }

    private List<FichaTecnicaItem> carregarItensAtivos(Long fichaId) {
        return fichaTecnicaItemRepository.findByFichaTecnicaIdAndStatus(
                fichaId,
                StatusAtivacao.ATIVO);
    }

    private Prato buscarPrato(Long id) {
        return pratoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prato não encontrado"));
    }

    private FichaTecnica buscarFicha(Long pratoId) {
        return fichaTecnicaRepository.findByPratoId(pratoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ficha técnica não encontrada para o prato"));
    }

    private record ResultadoFoodCost(
            BigDecimal percentual,
            ClassificacaoFoodCost classificacao,
            String aviso) {
    }

    private record CalculoFicha(
            BigDecimal custoUnitario,
            BigDecimal foodCostPercentual,
            ClassificacaoFoodCost classificacao,
            String aviso,
            Map<Long, BigDecimal> custosComponentes) {
    }
}
