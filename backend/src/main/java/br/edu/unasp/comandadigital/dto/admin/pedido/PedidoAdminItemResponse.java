package br.edu.unasp.comandadigital.dto.admin.pedido;

import java.math.BigDecimal;

public record PedidoAdminItemResponse(
        Long pratoId,
        String nomePrato,
        String fotoUrl,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        String observacoes) {
}
