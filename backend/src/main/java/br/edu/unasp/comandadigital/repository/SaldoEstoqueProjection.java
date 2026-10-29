package br.edu.unasp.comandadigital.repository;

import java.math.BigDecimal;

public interface SaldoEstoqueProjection {
    Long getIngredienteId();
    BigDecimal getSaldo();
}
