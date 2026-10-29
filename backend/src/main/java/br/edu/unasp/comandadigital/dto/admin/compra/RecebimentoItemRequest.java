package br.edu.unasp.comandadigital.dto.admin.compra;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecebimentoItemRequest(
        @NotNull(message = "O ingrediente é obrigatório nos dados de recebimento")
        @Positive(message = "O ingrediente deve possuir um identificador válido")
        Long ingredienteId,

        @Size(max = 100, message = "O lote deve ter no máximo 100 caracteres")
        String lote,

        @FutureOrPresent(message = "A validade não pode estar no passado")
        LocalDate validade) {
}
