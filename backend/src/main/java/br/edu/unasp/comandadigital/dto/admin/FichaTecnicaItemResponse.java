package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record FichaTecnicaItemResponse(
        Long id,
        IngredienteFichaResponse ingrediente,
        BigDecimal quantidade,
        UnidadeMedida unidade,
        BigDecimal fatorCorrecao,
        BigDecimal custoComponente) {
}
