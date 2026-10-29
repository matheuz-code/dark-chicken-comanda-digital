package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraDetalheResponse;
import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraRequest;
import br.edu.unasp.comandadigital.dto.admin.compra.PedidoCompraResumoResponse;
import br.edu.unasp.comandadigital.dto.admin.compra.ReceberPedidoCompraRequest;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.enums.StatusPedidoCompra;
import br.edu.unasp.comandadigital.service.AdminCompraService;
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
@RequestMapping("/api/admin/compras")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Compras", description = "Pedidos de compra e recebimento de mercadorias")
public class AdminCompraController {

    private final AdminCompraService compraService;

    public AdminCompraController(AdminCompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    @Operation(summary = "Listar pedidos de compra", description = "Filtros opcionais por status e fornecedor")
    public PaginaResponse<PedidoCompraResumoResponse> listar(
            @RequestParam(required = false) StatusPedidoCompra status,
            @RequestParam(required = false) @Positive Long fornecedorId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return compraService.listar(status, fornecedorId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar pedido de compra")
    public PedidoCompraDetalheResponse detalhar(@PathVariable @Positive Long id) {
        return compraService.detalhar(id);
    }

    @PostMapping
    @Operation(summary = "Criar pedido de compra", description = "Cria o pedido no estado RASCUNHO")
    public ResponseEntity<PedidoCompraDetalheResponse> criar(
            Authentication authentication,
            @Valid @RequestBody PedidoCompraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compraService.criar(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar pedido de compra", description = "Permitido somente no estado RASCUNHO")
    public PedidoCompraDetalheResponse atualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PedidoCompraRequest request) {
        return compraService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar pedido de compra", description = "Exclusão lógica pelo estado CANCELADO")
    public ResponseEntity<Void> cancelar(@PathVariable @Positive Long id) {
        compraService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enviar")
    @Operation(summary = "Enviar pedido de compra", description = "Transição RASCUNHO para ENVIADO")
    public PedidoCompraDetalheResponse enviar(@PathVariable @Positive Long id) {
        return compraService.enviar(id);
    }

    @PostMapping("/{id}/receber")
    @Operation(
            summary = "Receber pedido de compra",
            description = "Transição ENVIADO para RECEBIDO, com entrada no estoque e atualização de custos")
    public PedidoCompraDetalheResponse receber(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) ReceberPedidoCompraRequest request) {
        return compraService.receber(
                authentication.getName(),
                id,
                request == null ? ReceberPedidoCompraRequest.vazio() : request);
    }
}
