package br.edu.unasp.comandadigital.dto.usuario;

import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        String telefone,
        String endereco,
        StatusAtivacao status) {
}
