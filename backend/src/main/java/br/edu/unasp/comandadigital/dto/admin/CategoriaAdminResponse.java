package br.edu.unasp.comandadigital.dto.admin;

import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;

public record CategoriaAdminResponse(
        Long id,
        String nome,
        String descricao,
        Integer ordem,
        StatusAtivacao status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
