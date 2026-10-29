package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.admin.pedido.AtualizarStatusPedidoRequest;
import br.edu.unasp.comandadigital.dto.admin.pedido.CancelarPedidoRequest;
import br.edu.unasp.comandadigital.dto.admin.pedido.PedidoAdminDetalheResponse;
import br.edu.unasp.comandadigital.dto.admin.pedido.PedidoAdminResumoResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.enums.CanalPedido;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;
import br.edu.unasp.comandadigital.service.AdminPedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'COZINHEIRO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Pedidos", description = "Gestão do ciclo dos pedidos")
public class AdminPedidoController {

    private final AdminPedidoService pedidoService;

    public AdminPedidoController(AdminPedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    @Operation(summary = "Listar pedidos", description = "Lista paginada com filtros por status e canal")
    public PaginaResponse<PedidoAdminResumoResponse> listar(
            Authentication authentication,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) CanalPedido canal,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return pedidoService.listar(
                authentication.getName(),
                status,
                canal,
                pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar pedido", description = "Retorna cliente, itens e timeline")
    public PedidoAdminDetalheResponse detalhar(
            Authentication authentication,
            @PathVariable @Positive Long id) {
        return pedidoService.detalhar(authentication.getName(), id);
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Mudar status",
            description = "Aceita somente a próxima etapa válida do fluxo e baixa o estoque ao iniciar o preparo")
    public PedidoAdminDetalheResponse atualizarStatus(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody AtualizarStatusPedidoRequest request) {
        return pedidoService.atualizarStatus(authentication.getName(), id, request);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(
            summary = "Cancelar pedido",
            description = "Aplica a RN04, registra o motivo e estorna o estoque que já tiver sido baixado")
    public PedidoAdminDetalheResponse cancelar(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody CancelarPedidoRequest request) {
        return pedidoService.cancelar(authentication.getName(), id, request);
    }
}
