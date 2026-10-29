package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.PratoAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.PratoAdminResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.enums.StatusPrato;
import br.edu.unasp.comandadigital.service.AdminPratoService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/pratos")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Pratos", description = "Gestão administrativa dos pratos")
public class AdminPratoController {

    private final AdminPratoService pratoService;

    public AdminPratoController(AdminPratoService pratoService) {
        this.pratoService = pratoService;
    }

    @GetMapping
    @Operation(summary = "Listar pratos", description = "Lista paginada com filtros opcionais")
    public PaginaResponse<PratoAdminResponse> listar(
            @RequestParam(required = false) StatusPrato status,
            @RequestParam(required = false) @Positive Long categoriaId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return pratoService.listar(status, categoriaId, pageable);
    }

    @PostMapping
    @Operation(summary = "Criar prato", description = "RN01: exige ficha técnica para status ATIVO")
    public ResponseEntity<PratoAdminResponse> criar(
            @Valid @RequestBody PratoAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pratoService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar prato", description = "RN01: exige ficha técnica para status ATIVO")
    public PratoAdminResponse atualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PratoAdminRequest request) {
        return pratoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar prato", description = "Soft delete: preserva o registro")
    public ResponseEntity<Void> desativar(@PathVariable @Positive Long id) {
        pratoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
