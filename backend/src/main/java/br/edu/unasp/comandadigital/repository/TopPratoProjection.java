package br.edu.unasp.comandadigital.repository;

import java.math.BigDecimal;

public interface TopPratoProjection {
    Long getPratoId();
    String getPratoNome();
    Long getQuantidadeVendida();
    BigDecimal getFaturamento();
}
