package br.edu.unasp.comandadigital.dto.admin.compra;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoCompraRequest(
        @NotNull(message = "O fornecedor é obrigatório")
        @Positive(message = "O fornecedor deve possuir um identificador válido")
        Long fornecedorId,

        @NotEmpty(message = "O pedido de compra deve possuir ao menos um item")
        List<@Valid PedidoCompraItemRequest> itens) {

    public PedidoCompraRequest {
        itens = itens == null ? null : List.copyOf(itens);
    }
}
