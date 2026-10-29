package br.edu.unasp.comandadigital.dto.admin.estoque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.MotivoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.entity.enums.TipoMovimentacaoEstoque;

public record MovimentacaoEstoqueResponse(
        Long id,
        IngredienteMovimentacaoResponse ingrediente,
        TipoMovimentacaoEstoque tipo,
        BigDecimal quantidade,
        MotivoMovimentacaoEstoque motivo,
        String lote,
        LocalDate validade,
        BigDecimal custoUnitario,
        Long pedidoId,
        Long pedidoCompraId,
        UsuarioMovimentacaoResponse usuario,
        LocalDateTime criadoEm) {
}
