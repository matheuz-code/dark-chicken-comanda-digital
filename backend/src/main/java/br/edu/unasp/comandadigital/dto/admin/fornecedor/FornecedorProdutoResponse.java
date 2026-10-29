package br.edu.unasp.comandadigital.dto.admin.fornecedor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record FornecedorProdutoResponse(
        Long id,
        Long fornecedorId,
        Long ingredienteId,
        String ingredienteNome,
        String ingredienteSku,
        BigDecimal preco,
        UnidadeMedida unidadeVenda,
        BigDecimal precoNaUnidadePadrao,
        UnidadeMedida unidadePadrao,
        StatusAtivacao status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
