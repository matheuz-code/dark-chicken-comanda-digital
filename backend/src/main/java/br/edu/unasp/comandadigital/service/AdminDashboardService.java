package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.edu.unasp.comandadigital.dto.admin.dashboard.DashboardResponse;
import br.edu.unasp.comandadigital.dto.admin.dashboard.TopPratoResponse;
import br.edu.unasp.comandadigital.repository.DashboardCustoProjection;
import br.edu.unasp.comandadigital.repository.DashboardRepository;
import br.edu.unasp.comandadigital.repository.DashboardResumoProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int ESCALA_MONETARIA = 2;

    private final DashboardRepository dashboardRepository;

    public AdminDashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardResponse consultarKpisDoDia() {
        PeriodoDia periodo = periodoHoje();
        DashboardResumoProjection resumo = dashboardRepository.resumirPedidosPagos(
                periodo.inicio(),
                periodo.fim());
        DashboardCustoProjection custo = dashboardRepository.calcularCustoIngredientes(
                periodo.inicio(),
                periodo.fim());

        long totalPedidos = resumo.getTotalPedidos() == null
                ? 0L
                : resumo.getTotalPedidos();
        BigDecimal faturamento = valorOuZero(resumo.getFaturamento())
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal custoIngredientes = valorOuZero(custo.getCustoIngredientes())
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal ticketMedio = totalPedidos == 0
                ? BigDecimal.ZERO.setScale(ESCALA_MONETARIA)
                : faturamento.divide(
                        BigDecimal.valueOf(totalPedidos),
                        ESCALA_MONETARIA,
                        RoundingMode.HALF_UP);
        BigDecimal foodCostMedio = faturamento.signum() == 0
                ? BigDecimal.ZERO.setScale(ESCALA_MONETARIA)
                : custoIngredientes.multiply(BigDecimal.valueOf(100))
                        .divide(faturamento, ESCALA_MONETARIA, RoundingMode.HALF_UP);

        return new DashboardResponse(
                periodo.data(),
                faturamento,
                totalPedidos,
                ticketMedio,
                custoIngredientes,
                foodCostMedio);
    }

    public List<TopPratoResponse> consultarTopPratos(int limit) {
        PeriodoDia periodo = periodoHoje();
        return dashboardRepository.buscarTopPratos(
                        periodo.inicio(),
                        periodo.fim(),
                        PageRequest.of(0, limit))
                .stream()
                .map(prato -> new TopPratoResponse(
                        prato.getPratoId(),
                        prato.getPratoNome(),
                        prato.getQuantidadeVendida(),
                        prato.getFaturamento().setScale(
                                ESCALA_MONETARIA,
                                RoundingMode.HALF_UP)))
                .toList();
    }

    private PeriodoDia periodoHoje() {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.atStartOfDay();
        return new PeriodoDia(hoje, inicio, inicio.plusDays(1));
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private record PeriodoDia(
            LocalDate data,
            LocalDateTime inicio,
            LocalDateTime fim) {
    }
}
