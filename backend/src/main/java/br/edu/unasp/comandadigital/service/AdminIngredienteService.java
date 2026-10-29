package br.edu.unasp.comandadigital.service;

import java.util.Locale;

import br.edu.unasp.comandadigital.dto.admin.IngredienteAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.IngredienteAdminResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.exception.ConflitoException;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.repository.IngredienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminIngredienteService {

    private static final Sort ORDENACAO = Sort.by(
            Sort.Order.asc("nome"),
            Sort.Order.asc("id"));

    private final IngredienteRepository ingredienteRepository;

    public AdminIngredienteService(IngredienteRepository ingredienteRepository) {
        this.ingredienteRepository = ingredienteRepository;
    }

    public PaginaResponse<IngredienteAdminResponse> listar(
            StatusAtivacao status,
            Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO);
        Page<Ingrediente> ingredientes = status == null
                ? ingredienteRepository.findAll(pageRequest)
                : ingredienteRepository.findByStatus(status, pageRequest);
        return PaginaResponse.de(ingredientes.map(this::toResponse));
    }

    @Transactional
    public IngredienteAdminResponse criar(IngredienteAdminRequest request) {
        String sku = normalizarSku(request.sku());
        validarSkuDisponivel(sku, null);
        Ingrediente ingrediente = Ingrediente.criar(
                request.nome().trim(),
                sku,
                request.unidadePadrao(),
                request.estoqueMinimo(),
                request.custoUnitario(),
                request.status());
        return toResponse(ingredienteRepository.saveAndFlush(ingrediente));
    }

    @Transactional
    public IngredienteAdminResponse atualizar(Long id, IngredienteAdminRequest request) {
        Ingrediente ingrediente = buscar(id);
        String sku = normalizarSku(request.sku());
        validarSkuDisponivel(sku, id);

        ingrediente.setNome(request.nome().trim());
        ingrediente.setSku(sku);
        ingrediente.setUnidadePadrao(request.unidadePadrao());
        ingrediente.setEstoqueMinimo(request.estoqueMinimo());
        ingrediente.setCustoUnitario(request.custoUnitario());
        ingrediente.setStatus(request.status());
        return toResponse(ingredienteRepository.saveAndFlush(ingrediente));
    }

    @Transactional
    public void desativar(Long id) {
        Ingrediente ingrediente = buscar(id);
        if (ingrediente.getStatus() != StatusAtivacao.INATIVO) {
            ingrediente.setStatus(StatusAtivacao.INATIVO);
            ingredienteRepository.saveAndFlush(ingrediente);
        }
    }

    private Ingrediente buscar(Long id) {
        return ingredienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ingrediente não encontrado"));
    }

    private void validarSkuDisponivel(String sku, Long idAtual) {
        boolean existe = idAtual == null
                ? ingredienteRepository.existsBySkuIgnoreCase(sku)
                : ingredienteRepository.existsBySkuIgnoreCaseAndIdNot(sku, idAtual);
        if (existe) {
            throw new ConflitoException("Já existe um ingrediente cadastrado com este SKU");
        }
    }

    private String normalizarSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private IngredienteAdminResponse toResponse(Ingrediente ingrediente) {
        return new IngredienteAdminResponse(
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getSku(),
                ingrediente.getUnidadePadrao(),
                ingrediente.getEstoqueMinimo(),
                ingrediente.getCustoUnitario(),
                ingrediente.getStatus(),
                ingrediente.getCreatedAt(),
                ingrediente.getUpdatedAt());
    }
}
