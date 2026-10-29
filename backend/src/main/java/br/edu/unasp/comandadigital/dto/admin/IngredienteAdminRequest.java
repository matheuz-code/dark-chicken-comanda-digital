package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IngredienteAdminRequest(
        @NotBlank(message = "O nome do ingrediente é obrigatório")
        @Size(max = 150, message = "O nome do ingrediente deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "O SKU do ingrediente é obrigatório")
        @Size(max = 50, message = "O SKU do ingrediente deve ter no máximo 50 caracteres")
        @Pattern(
                regexp = "[A-Za-z0-9][A-Za-z0-9._-]*",
                message = "O SKU deve conter somente letras, números, ponto, hífen ou sublinhado")
        String sku,

        @NotNull(message = "A unidade padrão do ingrediente é obrigatória")
        UnidadeMedida unidadePadrao,

        @NotNull(message = "O estoque mínimo é obrigatório")
        @DecimalMin(value = "0.0000", message = "O estoque mínimo não pode ser negativo")
        @Digits(integer = 10, fraction = 4, message = "O estoque mínimo deve possuir no máximo 4 casas decimais")
        BigDecimal estoqueMinimo,

        @NotNull(message = "O custo unitário é obrigatório")
        @DecimalMin(value = "0.0000", message = "O custo unitário não pode ser negativo")
        @Digits(integer = 10, fraction = 4, message = "O custo unitário deve possuir no máximo 4 casas decimais")
        BigDecimal custoUnitario,

        @NotNull(message = "O status do ingrediente é obrigatório")
        StatusAtivacao status) {
}
