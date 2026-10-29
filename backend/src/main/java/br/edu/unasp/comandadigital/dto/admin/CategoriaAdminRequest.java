package br.edu.unasp.comandadigital.dto.admin;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaAdminRequest(
        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres")
        String nome,

        @Size(max = 500, message = "A descrição da categoria deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "A ordem da categoria é obrigatória")
        @Min(value = 0, message = "A ordem da categoria não pode ser negativa")
        Integer ordem,

        @NotNull(message = "O status da categoria é obrigatório")
        StatusAtivacao status) {
}
