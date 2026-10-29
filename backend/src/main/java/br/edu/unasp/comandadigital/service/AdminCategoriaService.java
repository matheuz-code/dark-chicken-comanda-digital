package br.edu.unasp.comandadigital.service;

import br.edu.unasp.comandadigital.dto.admin.CategoriaAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.CategoriaAdminResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.Categoria;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.exception.ConflitoException;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.repository.CategoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminCategoriaService {

    private static final Sort ORDENACAO = Sort.by(
            Sort.Order.asc("ordem"),
            Sort.Order.asc("nome"),
            Sort.Order.asc("id"));

    private final CategoriaRepository categoriaRepository;

    public AdminCategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public PaginaResponse<CategoriaAdminResponse> listar(
            StatusAtivacao status,
            Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO);
        Page<Categoria> categorias = status == null
                ? categoriaRepository.findAll(pageRequest)
                : categoriaRepository.findByStatus(status, pageRequest);
        return PaginaResponse.de(categorias.map(this::toResponse));
    }

    @Transactional
    public CategoriaAdminResponse criar(CategoriaAdminRequest request) {
        String nome = request.nome().trim();
        validarNomeDisponivel(nome, null);
        Categoria categoria = Categoria.criar(
                nome,
                normalizarTextoOpcional(request.descricao()),
                request.ordem(),
                request.status());
        return toResponse(categoriaRepository.saveAndFlush(categoria));
    }

    @Transactional
    public CategoriaAdminResponse atualizar(Long id, CategoriaAdminRequest request) {
        Categoria categoria = buscar(id);
        String nome = request.nome().trim();
        validarNomeDisponivel(nome, id);

        categoria.setNome(nome);
        categoria.setDescricao(normalizarTextoOpcional(request.descricao()));
        categoria.setOrdem(request.ordem());
        categoria.setStatus(request.status());
        return toResponse(categoriaRepository.saveAndFlush(categoria));
    }

    @Transactional
    public void desativar(Long id) {
        Categoria categoria = buscar(id);
        if (categoria.getStatus() != StatusAtivacao.INATIVO) {
            categoria.setStatus(StatusAtivacao.INATIVO);
            categoriaRepository.saveAndFlush(categoria);
        }
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada"));
    }

    private void validarNomeDisponivel(String nome, Long idAtual) {
        boolean existe = idAtual == null
                ? categoriaRepository.existsByNomeIgnoreCase(nome)
                : categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, idAtual);
        if (existe) {
            throw new ConflitoException("Já existe uma categoria cadastrada com este nome");
        }
    }

    private CategoriaAdminResponse toResponse(Categoria categoria) {
        return new CategoriaAdminResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getOrdem(),
                categoria.getStatus(),
                categoria.getCreatedAt(),
                categoria.getUpdatedAt());
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
