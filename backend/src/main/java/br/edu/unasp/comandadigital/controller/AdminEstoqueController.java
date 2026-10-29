package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.estoque.MovimentacaoEstoqueManualRequest;
import br.edu.unasp.comandadigital.dto.admin.estoque.MovimentacaoEstoqueResponse;
import br.edu.unasp.comandadigital.dto.admin.estoque.SaldoEstoqueResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.enums.MotivoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.TipoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.service.AdminEstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/estoque")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Estoque", description = "Saldos, alertas e movimentações de estoque")
public class AdminEstoqueController {

    private final AdminEstoqueService estoqueService;

    public AdminEstoqueController(AdminEstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping("/saldo")
    @Operation(
            summary = "Consultar saldos",
            description = "Retorna o saldo em tempo real de cada ingrediente")
    public PaginaResponse<SaldoEstoqueResponse> listarSaldos(
            @RequestParam(required = false) StatusAtivacao status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return estoqueService.listarSaldos(status, pageable);
    }

    @GetMapping("/alertas")
    @Operation(
            summary = "Consultar alertas de estoque",
            description = "Lista ingredientes ativos com saldo abaixo do estoque mínimo")
    public PaginaResponse<SaldoEstoqueResponse> listarAlertas(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return estoqueService.listarAlertas(pageable);
    }

    @GetMapping("/movimentacoes")
    @Operation(
            summary = "Consultar movimentações",
            description = "Histórico paginado com filtros opcionais por ingrediente, tipo e motivo")
    public PaginaResponse<MovimentacaoEstoqueResponse> listarMovimentacoes(
            @RequestParam(required = false) @Positive Long ingredienteId,
            @RequestParam(required = false) TipoMovimentacaoEstoque tipo,
            @RequestParam(required = false) MotivoMovimentacaoEstoque motivo,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return estoqueService.listarMovimentacoes(
                ingredienteId,
                tipo,
                motivo,
                pageable);
    }

    @PostMapping("/movimentacao")
    @Operation(
            summary = "Registrar saída manual",
            description = "Registra perda, vencimento, quebra, uso interno ou ajuste sem permitir saldo negativo")
    public ResponseEntity<MovimentacaoEstoqueResponse> registrarSaidaManual(
            Authentication authentication,
            @Valid @RequestBody MovimentacaoEstoqueManualRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estoqueService.registrarSaidaManual(
                        authentication.getName(),
                        request));
    }
}
