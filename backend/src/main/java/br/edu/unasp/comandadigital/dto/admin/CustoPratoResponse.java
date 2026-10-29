package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;

public record CustoPratoResponse(
        Long pratoId,
        String nomePrato,
        BigDecimal precoVenda,
        BigDecimal custoUnitario,
        BigDecimal foodCostPercentual,
        ClassificacaoFoodCost classificacao,
        String aviso) {
}
