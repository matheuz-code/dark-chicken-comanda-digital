package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.CategoriaAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.CategoriaAdminResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.service.AdminCategoriaService;
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
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Categorias", description = "Gestão administrativa das categorias")
public class AdminCategoriaController {

    private final AdminCategoriaService categoriaService;

    public AdminCategoriaController(AdminCategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar categorias", description = "Lista paginada, incluindo inativas")
    public PaginaResponse<CategoriaAdminResponse> listar(
            @RequestParam(required = false) StatusAtivacao status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return categoriaService.listar(status, pageable);
    }

    @PostMapping
    @Operation(summary = "Criar categoria")
    public ResponseEntity<CategoriaAdminResponse> criar(
            @Valid @RequestBody CategoriaAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar categoria")
    public CategoriaAdminResponse atualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CategoriaAdminRequest request) {
        return categoriaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar categoria", description = "Soft delete: preserva o registro")
    public ResponseEntity<Void> desativar(@PathVariable @Positive Long id) {
        categoriaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
