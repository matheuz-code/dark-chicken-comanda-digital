package br.edu.unasp.comandadigital.dto.admin.estoque;

import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;

public record UsuarioMovimentacaoResponse(
        Long id,
        String nome,
        PerfilUsuario perfil) {
}
