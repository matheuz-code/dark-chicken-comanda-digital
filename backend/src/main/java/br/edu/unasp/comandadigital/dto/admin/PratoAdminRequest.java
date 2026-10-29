package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusPrato;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PratoAdminRequest(
        @NotNull(message = "A categoria do prato é obrigatória")
        @Positive(message = "A categoria do prato deve possuir um identificador válido")
        Long categoriaId,

        @NotBlank(message = "O nome do prato é obrigatório")
        @Size(max = 150, message = "O nome do prato deve ter no máximo 150 caracteres")
        String nome,

        @Size(max = 1000, message = "A descrição do prato deve ter no máximo 1000 caracteres")
        String descricao,

        @Size(max = 1000, message = "A URL da foto deve ter no máximo 1000 caracteres")
        String fotoUrl,

        @NotNull(message = "O preço de venda é obrigatório")
        @DecimalMin(value = "0.00", message = "O preço de venda não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "O preço de venda deve possuir no máximo 2 casas decimais")
        BigDecimal precoVenda,

        @NotNull(message = "O tempo de preparo é obrigatório")
        @Positive(message = "O tempo de preparo deve ser maior que zero")
        Integer tempoPreparoMin,

        @NotNull(message = "O status do prato é obrigatório")
        StatusPrato status) {
}
