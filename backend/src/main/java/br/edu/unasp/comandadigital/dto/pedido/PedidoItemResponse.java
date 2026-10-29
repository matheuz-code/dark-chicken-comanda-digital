package br.edu.unasp.comandadigital.dto.pedido;

import java.math.BigDecimal;

public record PedidoItemResponse(
        Long pratoId,
        String nomePrato,
        String fotoUrl,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        String observacoes) {
}
