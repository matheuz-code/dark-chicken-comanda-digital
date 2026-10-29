package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.IngredienteAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.IngredienteAdminResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.service.AdminIngredienteService;
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
@RequestMapping("/api/admin/ingredientes")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Ingredientes", description = "Gestão do cadastro de ingredientes")
public class AdminIngredienteController {

    private final AdminIngredienteService ingredienteService;

    public AdminIngredienteController(AdminIngredienteService ingredienteService) {
        this.ingredienteService = ingredienteService;
    }

    @GetMapping
    @Operation(summary = "Listar ingredientes", description = "Lista paginada com filtro de status")
    public PaginaResponse<IngredienteAdminResponse> listar(
            @RequestParam(required = false) StatusAtivacao status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ingredienteService.listar(status, pageable);
    }

    @PostMapping
    @Operation(summary = "Criar ingrediente")
    public ResponseEntity<IngredienteAdminResponse> criar(
            @Valid @RequestBody IngredienteAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ingredienteService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar ingrediente")
    public IngredienteAdminResponse atualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody IngredienteAdminRequest request) {
        return ingredienteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar ingrediente", description = "Soft delete: preserva o registro")
    public ResponseEntity<Void> desativar(@PathVariable @Positive Long id) {
        ingredienteService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
