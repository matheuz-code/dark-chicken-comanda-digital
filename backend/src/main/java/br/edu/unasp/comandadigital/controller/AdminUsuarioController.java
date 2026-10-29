package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.usuario.AtualizarUsuarioAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.usuario.CriarUsuarioAdminRequest;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.dto.usuario.UsuarioResponse;
import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.service.AdminUsuarioService;
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
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Usuários", description = "Gestão dos quatro perfis do sistema")
public class AdminUsuarioController {

    private final AdminUsuarioService usuarioService;

    public AdminUsuarioController(AdminUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuários")
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(required = false) PerfilUsuario perfil,
            @RequestParam(required = false) StatusAtivacao status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return usuarioService.listar(perfil, status, pageable);
    }

    @PostMapping
    @Operation(summary = "Criar usuário")
    public ResponseEntity<UsuarioResponse> criar(
            @Valid @RequestBody CriarUsuarioAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar usuário")
    public UsuarioResponse atualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AtualizarUsuarioAdminRequest request) {
        return usuarioService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar usuário", description = "Soft delete da conta")
    public ResponseEntity<Void> desativar(
            Authentication authentication,
            @PathVariable @Positive Long id) {
        usuarioService.desativar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
