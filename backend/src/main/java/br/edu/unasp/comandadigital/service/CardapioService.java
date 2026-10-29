package br.edu.unasp.comandadigital.service;

import br.edu.unasp.comandadigital.dto.cardapio.CategoriaCardapioResponse;
import br.edu.unasp.comandadigital.dto.cardapio.PratoCardapioResponse;
import br.edu.unasp.comandadigital.dto.cardapio.PratoDetalheResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.Prato;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.StatusPrato;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.repository.PratoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CardapioService {

    private static final Sort ORDENACAO_CARDAPIO = Sort.by(
            Sort.Order.asc("categoria.ordem"),
            Sort.Order.asc("nome"));

    private final PratoRepository pratoRepository;

    public CardapioService(PratoRepository pratoRepository) {
        this.pratoRepository = pratoRepository;
    }

    public PaginaResponse<PratoCardapioResponse> listar(Long categoriaId, Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO_CARDAPIO);

        Page<Prato> pratos = categoriaId == null
                ? pratoRepository.findByStatusAndCategoriaStatus(
                        StatusPrato.ATIVO,
                        StatusAtivacao.ATIVO,
                        pageRequest)
                : pratoRepository.findByCategoriaIdAndStatusAndCategoriaStatus(
                        categoriaId,
                        StatusPrato.ATIVO,
                        StatusAtivacao.ATIVO,
                        pageRequest);

        return PaginaResponse.de(pratos.map(this::toCardapioResponse));
    }

    public PratoDetalheResponse buscarDetalhe(Long id) {
        Prato prato = pratoRepository.findByIdAndStatusAndCategoriaStatus(
                        id,
                        StatusPrato.ATIVO,
                        StatusAtivacao.ATIVO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Prato ativo não encontrado"));

        return new PratoDetalheResponse(
                prato.getId(),
                prato.getNome(),
                prato.getDescricao(),
                prato.getFotoUrl(),
                prato.getPrecoVenda(),
                prato.getTempoPreparoMin(),
                toCategoriaResponse(prato));
    }

    private PratoCardapioResponse toCardapioResponse(Prato prato) {
        return new PratoCardapioResponse(
                prato.getId(),
                prato.getNome(),
                prato.getDescricao(),
                prato.getFotoUrl(),
                prato.getPrecoVenda(),
                toCategoriaResponse(prato));
    }

    private CategoriaCardapioResponse toCategoriaResponse(Prato prato) {
        return new CategoriaCardapioResponse(
                prato.getCategoria().getId(),
                prato.getCategoria().getNome());
    }
}
