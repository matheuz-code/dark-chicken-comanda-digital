package br.edu.unasp.comandadigital.dto.pedido;

import java.util.List;

import br.edu.unasp.comandadigital.entity.enums.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarPedidoRequest(
        @NotEmpty(message = "O pedido deve possuir pelo menos um item")
        @Size(max = 50, message = "O pedido deve possuir no máximo 50 itens diferentes")
        List<@Valid PedidoItemRequest> itens,

        @NotBlank(message = "O endereço de entrega é obrigatório")
        @Size(max = 500, message = "O endereço de entrega deve ter no máximo 500 caracteres")
        String enderecoEntrega,

        @NotNull(message = "A forma de pagamento é obrigatória")
        FormaPagamento formaPagamento,

        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres")
        String observacoes) {

    public CriarPedidoRequest {
        itens = itens == null ? null : List.copyOf(itens);
    }
}
