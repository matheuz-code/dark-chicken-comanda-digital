package br.edu.unasp.comandadigital.dto.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FichaTecnicaRequest(
        @NotNull(message = "O rendimento é obrigatório")
        @Positive(message = "O rendimento deve ser maior que zero")
        Integer rendimento,

        @NotBlank(message = "O modo de preparo é obrigatório")
        @Size(max = 10_000, message = "O modo de preparo deve ter no máximo 10000 caracteres")
        String modoPreparo,

        @NotEmpty(message = "A ficha técnica deve possuir pelo menos um ingrediente")
        @Size(max = 100, message = "A ficha técnica deve possuir no máximo 100 ingredientes")
        List<@Valid FichaTecnicaItemRequest> itens) {

    public FichaTecnicaRequest {
        itens = itens == null ? null : List.copyOf(itens);
    }
}
