package br.edu.unasp.comandadigital.dto.admin.compra;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoCompraItemRequest(
        @NotNull(message = "O ingrediente é obrigatório")
        @Positive(message = "O ingrediente deve possuir um identificador válido")
        Long ingredienteId,

        @NotNull(message = "A quantidade é obrigatória")
        @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero")
        @Digits(integer = 10, fraction = 4, message = "A quantidade deve possuir no máximo 4 casas decimais")
        BigDecimal quantidade,

        @NotNull(message = "O preço unitário é obrigatório")
        @DecimalMin(value = "0.0001", message = "O preço unitário deve ser maior que zero")
        @Digits(integer = 10, fraction = 4, message = "O preço unitário deve possuir no máximo 4 casas decimais")
        BigDecimal precoUnitario) {
}
