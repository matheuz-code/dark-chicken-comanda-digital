package br.edu.unasp.comandadigital.repository;

import java.math.BigDecimal;

public interface DashboardResumoProjection {
    Long getTotalPedidos();
    BigDecimal getFaturamento();
}
