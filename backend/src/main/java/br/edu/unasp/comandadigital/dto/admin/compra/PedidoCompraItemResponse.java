package br.edu.unasp.comandadigital.dto.admin.compra;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record PedidoCompraItemResponse(
        Long id,
        Long ingredienteId,
        String ingredienteNome,
        String ingredienteSku,
        BigDecimal quantidade,
        UnidadeMedida unidadeCompra,
        BigDecimal precoUnitario,
        BigDecimal subtotal) {
}
