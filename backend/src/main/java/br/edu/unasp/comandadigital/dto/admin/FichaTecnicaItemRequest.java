package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FichaTecnicaItemRequest(
        @NotNull(message = "O ingrediente é obrigatório")
        @Positive(message = "O ingrediente deve possuir um identificador válido")
        Long ingredienteId,

        @NotNull(message = "A quantidade do ingrediente é obrigatória")
        @DecimalMin(value = "0.0001", message = "A quantidade do ingrediente deve ser maior que zero")
        @Digits(integer = 10, fraction = 4, message = "A quantidade deve possuir no máximo 4 casas decimais")
        BigDecimal quantidade,

        @NotNull(message = "A unidade do ingrediente é obrigatória")
        UnidadeMedida unidade,

        @NotNull(message = "O fator de correção é obrigatório")
        @DecimalMin(value = "1.0000", message = "O fator de correção deve ser maior ou igual a 1")
        @Digits(integer = 6, fraction = 4, message = "O fator de correção deve possuir no máximo 4 casas decimais")
        BigDecimal fatorCorrecao) {
}
