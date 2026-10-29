package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FichaTecnicaResponse(
        Long id,
        Long pratoId,
        String nomePrato,
        Integer rendimento,
        String modoPreparo,
        BigDecimal custoUnitario,
        BigDecimal foodCostPercentual,
        ClassificacaoFoodCost classificacao,
        String aviso,
        List<FichaTecnicaItemResponse> itens,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public FichaTecnicaResponse {
        itens = List.copyOf(itens);
    }
}
