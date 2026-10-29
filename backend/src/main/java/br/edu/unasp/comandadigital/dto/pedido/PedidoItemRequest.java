package br.edu.unasp.comandadigital.dto.pedido;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PedidoItemRequest(
        @NotNull(message = "O prato é obrigatório")
        @Positive(message = "O prato deve possuir um identificador válido")
        Long pratoId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1")
        @Max(value = 100, message = "A quantidade deve ser no máximo 100")
        Integer quantidade,

        @Size(max = 500, message = "A observação do item deve ter no máximo 500 caracteres")
        String observacoes) {
}
