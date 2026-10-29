package br.edu.unasp.comandadigital.dto.admin.pedido;

import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;

public record UsuarioHistoricoAdminResponse(
        Long id,
        String nome,
        PerfilUsuario perfil) {
}
