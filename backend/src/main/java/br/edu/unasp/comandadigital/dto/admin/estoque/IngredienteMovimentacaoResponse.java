package br.edu.unasp.comandadigital.dto.admin.estoque;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record IngredienteMovimentacaoResponse(
        Long id,
        String nome,
        String sku,
        UnidadeMedida unidadePadrao) {
}
