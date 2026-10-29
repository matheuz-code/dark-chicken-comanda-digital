package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.CustoPratoResponse;
import br.edu.unasp.comandadigital.dto.admin.FichaTecnicaRequest;
import br.edu.unasp.comandadigital.dto.admin.FichaTecnicaResponse;
import br.edu.unasp.comandadigital.service.AdminFichaTecnicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/pratos/{pratoId}")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Fichas técnicas", description = "Ficha, custo e food cost dos pratos")
public class AdminFichaTecnicaController {

    private final AdminFichaTecnicaService fichaTecnicaService;

    public AdminFichaTecnicaController(AdminFichaTecnicaService fichaTecnicaService) {
        this.fichaTecnicaService = fichaTecnicaService;
    }

    @GetMapping("/ficha")
    @Operation(summary = "Consultar ficha técnica")
    public FichaTecnicaResponse buscar(@PathVariable @Positive Long pratoId) {
        return fichaTecnicaService.buscar(pratoId);
    }

    @PostMapping("/ficha")
    @Operation(summary = "Criar ficha técnica")
    public ResponseEntity<FichaTecnicaResponse> criar(
            @PathVariable @Positive Long pratoId,
            @Valid @RequestBody FichaTecnicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fichaTecnicaService.criar(pratoId, request));
    }

    @PutMapping("/ficha")
    @Operation(summary = "Atualizar ficha técnica", description = "Itens omitidos são desativados")
    public FichaTecnicaResponse atualizar(
            @PathVariable @Positive Long pratoId,
            @Valid @RequestBody FichaTecnicaRequest request) {
        return fichaTecnicaService.atualizar(pratoId, request);
    }

    @GetMapping("/custo")
    @Operation(summary = "Calcular custo e food cost")
    public CustoPratoResponse calcularCusto(@PathVariable @Positive Long pratoId) {
        return fichaTecnicaService.calcularCusto(pratoId);
    }
}
