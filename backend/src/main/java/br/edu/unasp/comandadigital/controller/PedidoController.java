package br.edu.unasp.comandadigital.controller;

import java.net.URI;

import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.dto.pedido.CriarPedidoRequest;
import br.edu.unasp.comandadigital.dto.pedido.PedidoCriadoResponse;
import br.edu.unasp.comandadigital.dto.pedido.PedidoResumoResponse;
import br.edu.unasp.comandadigital.dto.pedido.PedidoStatusResponse;
import br.edu.unasp.comandadigital.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pedidos do cliente", description = "Criação e acompanhamento dos próprios pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @Operation(summary = "Criar pedido", description = "Recalcula preços e valida o estoque no servidor")
    public ResponseEntity<PedidoCriadoResponse> criar(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CriarPedidoRequest request) {
        PedidoCriadoResponse response = pedidoService.criar(authentication.getName(), idempotencyKey, request);
        return ResponseEntity.created(URI.create("/api/pedidos/" + response.id() + "/status"))
                .body(response);
    }

    @GetMapping("/meus")
    @Operation(summary = "Listar meus pedidos")
    public PaginaResponse<PedidoResumoResponse> listarMeus(
            Authentication authentication,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return pedidoService.listarMeus(authentication.getName(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar meu pedido", description = "Detalhes somente do cliente proprietário")
    public PedidoCriadoResponse detalhar(Authentication authentication, @PathVariable @Positive Long id) {
        return pedidoService.detalharMeu(authentication.getName(), id);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Consultar status do pedido", description = "Retorna o status atual e a timeline")
    public PedidoStatusResponse consultarStatus(
            Authentication authentication,
            @PathVariable @Positive Long id) {
        return pedidoService.consultarStatus(authentication.getName(), id);
    }
}
