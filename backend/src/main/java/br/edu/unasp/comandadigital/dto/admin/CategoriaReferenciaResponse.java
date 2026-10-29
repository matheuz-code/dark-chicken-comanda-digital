package br.edu.unasp.comandadigital.dto.admin;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;

public record CategoriaReferenciaResponse(
        Long id,
        String nome,
        StatusAtivacao status) {
}
