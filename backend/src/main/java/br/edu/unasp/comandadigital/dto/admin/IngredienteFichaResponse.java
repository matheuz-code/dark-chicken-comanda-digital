package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record IngredienteFichaResponse(
        Long id,
        String nome,
        String sku,
        UnidadeMedida unidadePadrao,
        BigDecimal custoUnitario) {
}
