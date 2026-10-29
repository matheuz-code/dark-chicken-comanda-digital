package br.edu.unasp.comandadigital.controller;

import java.util.List;

import br.edu.unasp.comandadigital.dto.admin.dashboard.DashboardResponse;
import br.edu.unasp.comandadigital.dto.admin.dashboard.TopPratoResponse;
import br.edu.unasp.comandadigital.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Dashboard", description = "Indicadores gerenciais do dia")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar KPIs do dia",
            description = "Faturamento, pedidos pagos, ticket médio e food cost médio ponderado")
    public DashboardResponse consultarKpis() {
        return dashboardService.consultarKpisDoDia();
    }

    @GetMapping("/top-pratos")
    @Operation(summary = "Consultar pratos mais vendidos do dia")
    public List<TopPratoResponse> consultarTopPratos(
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "O limite deve ser no mínimo 1")
            @Max(value = 20, message = "O limite deve ser no máximo 20")
            int limit) {
        return dashboardService.consultarTopPratos(limit);
    }
}
