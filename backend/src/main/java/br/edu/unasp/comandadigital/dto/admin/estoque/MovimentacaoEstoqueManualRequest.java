package br.edu.unasp.comandadigital.dto.admin.estoque;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.unasp.comandadigital.entity.enums.MotivoMovimentacaoEstoque;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MovimentacaoEstoqueManualRequest(
        @NotNull(message = "O ingrediente é obrigatório")
        @Positive(message = "O ingrediente deve possuir um identificador positivo")
        Long ingredienteId,

        @NotNull(message = "A quantidade é obrigatória")
        @DecimalMin(
                value = "0.0001",
                message = "A quantidade da movimentação deve ser maior que zero")
        @Digits(
                integer = 10,
                fraction = 4,
                message = "A quantidade deve possuir no máximo 4 casas decimais")
        BigDecimal quantidade,

        @NotNull(message = "O motivo da movimentação é obrigatório")
        MotivoMovimentacaoEstoque motivo,

        @Size(max = 100, message = "O lote deve ter no máximo 100 caracteres")
        String lote,

        LocalDate validade) {
}
