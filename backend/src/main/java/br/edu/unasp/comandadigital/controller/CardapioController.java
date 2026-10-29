package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.cardapio.PratoCardapioResponse;
import br.edu.unasp.comandadigital.dto.cardapio.PratoDetalheResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.service.CardapioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/cardapio")
@Tag(name = "Cardápio", description = "Consulta pública de pratos disponíveis")
public class CardapioController {

    private final CardapioService cardapioService;

    public CardapioController(CardapioService cardapioService) {
        this.cardapioService = cardapioService;
    }

    @GetMapping
    @Operation(summary = "Listar cardápio", description = "Lista somente pratos e categorias ativos")
    public PaginaResponse<PratoCardapioResponse> listar(
            @RequestParam(required = false) @Positive Long categoriaId,
            @ParameterObject @PageableDefault(size = 12) Pageable pageable) {
        return cardapioService.listar(categoriaId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar prato ativo")
    public PratoDetalheResponse detalhe(@PathVariable @Positive Long id) {
        return cardapioService.buscarDetalhe(id);
    }
}
