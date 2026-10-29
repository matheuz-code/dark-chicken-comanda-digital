package br.edu.unasp.comandadigital.dto.admin.compra;

import java.util.List;

import jakarta.validation.Valid;

public record ReceberPedidoCompraRequest(
        List<@Valid RecebimentoItemRequest> itens) {

    public ReceberPedidoCompraRequest {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    public static ReceberPedidoCompraRequest vazio() {
        return new ReceberPedidoCompraRequest(List.of());
    }
}
