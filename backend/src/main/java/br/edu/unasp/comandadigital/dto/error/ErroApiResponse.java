package br.edu.unasp.comandadigital.dto.error;

import java.time.Instant;
import java.util.List;

public record ErroApiResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<ErroCampoResponse> campos) {

    public ErroApiResponse {
        campos = List.copyOf(campos);
    }
}
