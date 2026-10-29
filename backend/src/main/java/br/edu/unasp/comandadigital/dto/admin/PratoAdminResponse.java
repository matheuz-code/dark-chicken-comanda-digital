package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusPrato;

public record PratoAdminResponse(
        Long id,
        CategoriaReferenciaResponse categoria,
        String nome,
        String descricao,
        String fotoUrl,
        BigDecimal precoVenda,
        Integer tempoPreparoMin,
        StatusPrato status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
