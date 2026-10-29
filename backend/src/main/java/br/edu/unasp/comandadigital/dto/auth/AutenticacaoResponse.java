package br.edu.unasp.comandadigital.dto.auth;

import br.edu.unasp.comandadigital.dto.usuario.UsuarioResponse;

public record AutenticacaoResponse(
        String token,
        String tipo,
        long expiraEmSegundos,
        UsuarioResponse usuario) {

    public static final String TIPO_BEARER = "Bearer";
}
