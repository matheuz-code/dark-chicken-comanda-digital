package br.edu.unasp.comandadigital.dto.admin.fornecedor;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FornecedorProdutoRequest(
        @NotNull(message = "O ingrediente é obrigatório")
        @Positive(message = "O ingrediente deve possuir um identificador válido")
        Long ingredienteId,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.0001", message = "O preço deve ser maior que zero")
        @Digits(integer = 10, fraction = 4, message = "O preço deve possuir no máximo 4 casas decimais")
        BigDecimal preco,

        @NotNull(message = "A unidade de venda é obrigatória")
        UnidadeMedida unidadeVenda) {
}
