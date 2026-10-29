package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import org.springframework.stereotype.Service;

@Service
public class ConversorUnidadeService {

    private static final int ESCALA_CONVERSAO = 8;

    public BigDecimal converter(
            BigDecimal quantidade,
            UnidadeMedida origem,
            UnidadeMedida destino) {
        if (origem == destino) return quantidade;
        if (origem == UnidadeMedida.KG && destino == UnidadeMedida.G) {
            return quantidade.multiply(BigDecimal.valueOf(1000));
        }
        if (origem == UnidadeMedida.G && destino == UnidadeMedida.KG) {
            return quantidade.divide(
                    BigDecimal.valueOf(1000),
                    ESCALA_CONVERSAO,
                    RoundingMode.HALF_UP);
        }
        if (origem == UnidadeMedida.L && destino == UnidadeMedida.ML) {
            return quantidade.multiply(BigDecimal.valueOf(1000));
        }
        if (origem == UnidadeMedida.ML && destino == UnidadeMedida.L) {
            return quantidade.divide(
                    BigDecimal.valueOf(1000),
                    ESCALA_CONVERSAO,
                    RoundingMode.HALF_UP);
        }
        throw new RegraNegocioException(
                "Não é possível converter a unidade " + origem + " para " + destino);
    }
}
