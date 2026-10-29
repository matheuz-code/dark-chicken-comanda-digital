package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.fornecedor.CotacaoFornecedorResponse;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorProdutoRequest;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorProdutoResponse;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorRequest;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorResponse;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.HistoricoPrecoResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.service.AdminFornecedorService;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Fornecedores", description = "Fornecedores, catálogos, preços e cotações")
public class AdminFornecedorController {

    private final AdminFornecedorService fornecedorService;

    public AdminFornecedorController(AdminFornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @GetMapping("/fornecedores")
    @Operation(summary = "Listar fornecedores", description = "Lista paginada com filtro de status")
    public PaginaResponse<FornecedorResponse> listar(
            @RequestParam(required = false) StatusAtivacao status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return fornecedorService.listar(status, pageable);
    }

    @PostMapping("/fornecedores")
    @Operation(summary = "Cadastrar fornecedor", description = "Valida e normaliza o CNPJ")
    public ResponseEntity<FornecedorResponse> criar(
            @Valid @RequestBody FornecedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fornecedorService.criar(request));
    }

    @PutMapping("/fornecedores/{id}")
    @Operation(summary = "Editar fornecedor")
    public FornecedorResponse atualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody FornecedorRequest request) {
        return fornecedorService.atualizar(id, request);
    }

    @DeleteMapping("/fornecedores/{id}")
    @Operation(summary = "Desativar fornecedor", description = "Soft delete: preserva o registro")
    public ResponseEntity<Void> desativar(@PathVariable @Positive Long id) {
        fornecedorService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fornecedores/{fornecedorId}/produtos")
    @Operation(summary = "Listar catálogo do fornecedor")
    public PaginaResponse<FornecedorProdutoResponse> listarCatalogo(
            @PathVariable @Positive Long fornecedorId,
            @RequestParam(required = false) StatusAtivacao status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return fornecedorService.listarCatalogo(fornecedorId, status, pageable);
    }

    @PostMapping("/fornecedores/{fornecedorId}/produtos")
    @Operation(summary = "Adicionar item ao catálogo", description = "Registra o preço inicial no histórico")
    public ResponseEntity<FornecedorProdutoResponse> adicionarProduto(
            Authentication authentication,
            @PathVariable @Positive Long fornecedorId,
            @Valid @RequestBody FornecedorProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fornecedorService.adicionarProduto(
                        authentication.getName(),
                        fornecedorId,
                        request));
    }

    @PutMapping("/fornecedores/{fornecedorId}/produtos/{produtoId}")
    @Operation(summary = "Atualizar item do catálogo", description = "Mudanças de preço são historiadas")
    public FornecedorProdutoResponse atualizarProduto(
            Authentication authentication,
            @PathVariable @Positive Long fornecedorId,
            @PathVariable @Positive Long produtoId,
            @Valid @RequestBody FornecedorProdutoRequest request) {
        return fornecedorService.atualizarProduto(
                authentication.getName(),
                fornecedorId,
                produtoId,
                request);
    }

    @DeleteMapping("/fornecedores/{fornecedorId}/produtos/{produtoId}")
    @Operation(summary = "Desativar item do catálogo", description = "Soft delete: preserva preços e histórico")
    public ResponseEntity<Void> desativarProduto(
            @PathVariable @Positive Long fornecedorId,
            @PathVariable @Positive Long produtoId) {
        fornecedorService.desativarProduto(fornecedorId, produtoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fornecedores/{fornecedorId}/produtos/{produtoId}/historico-precos")
    @Operation(summary = "Consultar histórico de preços", description = "Dados paginados prontos para gráfico")
    public PaginaResponse<HistoricoPrecoResponse> listarHistorico(
            @PathVariable @Positive Long fornecedorId,
            @PathVariable @Positive Long produtoId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return fornecedorService.listarHistorico(fornecedorId, produtoId, pageable);
    }

    @GetMapping("/cotacao/{ingredienteId}")
    @Operation(
            summary = "Comparar cotação por ingrediente",
            description = "Ordena todos os fornecedores ativos pelo preço convertido à unidade padrão")
    public PaginaResponse<CotacaoFornecedorResponse> cotar(
            @PathVariable @Positive Long ingredienteId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return fornecedorService.cotar(ingredienteId, pageable);
    }
}
