package br.edu.unasp.comandadigital.service;

import br.edu.unasp.comandadigital.dto.admin.CategoriaReferenciaResponse;
import br.edu.unasp.comandadigital.dto.admin.PratoAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.PratoAdminResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.Categoria;
import br.edu.unasp.comandadigital.entity.FichaTecnica;
import br.edu.unasp.comandadigital.entity.Prato;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.StatusPrato;
import br.edu.unasp.comandadigital.exception.ConflitoException;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.CategoriaRepository;
import br.edu.unasp.comandadigital.repository.FichaTecnicaItemRepository;
import br.edu.unasp.comandadigital.repository.FichaTecnicaRepository;
import br.edu.unasp.comandadigital.repository.PratoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminPratoService {

    private static final Sort ORDENACAO = Sort.by(
            Sort.Order.asc("nome"),
            Sort.Order.asc("id"));

    private final PratoRepository pratoRepository;
    private final CategoriaRepository categoriaRepository;
    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final FichaTecnicaItemRepository fichaTecnicaItemRepository;

    public AdminPratoService(
            PratoRepository pratoRepository,
            CategoriaRepository categoriaRepository,
            FichaTecnicaRepository fichaTecnicaRepository,
            FichaTecnicaItemRepository fichaTecnicaItemRepository) {
        this.pratoRepository = pratoRepository;
        this.categoriaRepository = categoriaRepository;
        this.fichaTecnicaRepository = fichaTecnicaRepository;
        this.fichaTecnicaItemRepository = fichaTecnicaItemRepository;
    }

    public PaginaResponse<PratoAdminResponse> listar(
            StatusPrato status,
            Long categoriaId,
            Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO);
        Page<Prato> pratos;
        if (status != null && categoriaId != null) {
            pratos = pratoRepository.findByCategoriaIdAndStatus(categoriaId, status, pageRequest);
        } else if (status != null) {
            pratos = pratoRepository.findByStatus(status, pageRequest);
        } else if (categoriaId != null) {
            pratos = pratoRepository.findByCategoriaId(categoriaId, pageRequest);
        } else {
            pratos = pratoRepository.findAll(pageRequest);
        }
        return PaginaResponse.de(pratos.map(this::toResponse));
    }

    @Transactional
    public PratoAdminResponse criar(PratoAdminRequest request) {
        Categoria categoria = buscarCategoria(request.categoriaId());
        String nome = request.nome().trim();
        validarNomeDisponivel(nome, categoria.getId(), null);
        validarAtivacao(request.status(), categoria, null);

        Prato prato = Prato.criar(
                categoria,
                nome,
                normalizarTextoOpcional(request.descricao()),
                normalizarTextoOpcional(request.fotoUrl()),
                request.precoVenda(),
                request.tempoPreparoMin(),
                request.status());
        return toResponse(pratoRepository.saveAndFlush(prato));
    }

    @Transactional
    public PratoAdminResponse atualizar(Long id, PratoAdminRequest request) {
        Prato prato = buscar(id);
        Categoria categoria = buscarCategoria(request.categoriaId());
        String nome = request.nome().trim();
        validarNomeDisponivel(nome, categoria.getId(), id);
        validarAtivacao(request.status(), categoria, id);

        prato.setCategoria(categoria);
        prato.setNome(nome);
        prato.setDescricao(normalizarTextoOpcional(request.descricao()));
        prato.setFotoUrl(normalizarTextoOpcional(request.fotoUrl()));
        prato.setPrecoVenda(request.precoVenda());
        prato.setTempoPreparoMin(request.tempoPreparoMin());
        prato.setStatus(request.status());
        return toResponse(pratoRepository.saveAndFlush(prato));
    }

    @Transactional
    public void desativar(Long id) {
        Prato prato = buscar(id);
        if (prato.getStatus() != StatusPrato.INATIVO) {
            prato.setStatus(StatusPrato.INATIVO);
            pratoRepository.saveAndFlush(prato);
        }
    }

    private Prato buscar(Long id) {
        return pratoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prato não encontrado"));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    private void validarNomeDisponivel(String nome, Long categoriaId, Long idAtual) {
        boolean existe = idAtual == null
                ? pratoRepository.existsByNomeIgnoreCaseAndCategoriaId(nome, categoriaId)
                : pratoRepository.existsByNomeIgnoreCaseAndCategoriaIdAndIdNot(
                        nome,
                        categoriaId,
                        idAtual);
        if (existe) {
            throw new ConflitoException(
                    "Já existe um prato com este nome na categoria informada");
        }
    }

    private void validarAtivacao(StatusPrato status, Categoria categoria, Long pratoId) {
        if (status != StatusPrato.ATIVO) return;
        if (categoria.getStatus() != StatusAtivacao.ATIVO) {
            throw new RegraNegocioException(
                    "Um prato ativo precisa pertencer a uma categoria ativa");
        }
        if (pratoId == null) {
            throw new RegraNegocioException(
                    "O prato precisa de ficha técnica completa para ficar ATIVO");
        }
        FichaTecnica ficha = fichaTecnicaRepository.findByPratoId(pratoId)
                .orElseThrow(() -> new RegraNegocioException(
                        "O prato precisa de ficha técnica completa para ficar ATIVO"));
        if (!fichaTecnicaItemRepository.existsByFichaTecnicaIdAndStatus(
                ficha.getId(),
                StatusAtivacao.ATIVO)) {
            throw new RegraNegocioException(
                    "O prato precisa de ficha técnica completa para ficar ATIVO");
        }
    }

    private PratoAdminResponse toResponse(Prato prato) {
        Categoria categoria = prato.getCategoria();
        return new PratoAdminResponse(
                prato.getId(),
                new CategoriaReferenciaResponse(
                        categoria.getId(),
                        categoria.getNome(),
                        categoria.getStatus()),
                prato.getNome(),
                prato.getDescricao(),
                prato.getFotoUrl(),
                prato.getPrecoVenda(),
                prato.getTempoPreparoMin(),
                prato.getStatus(),
                prato.getCreatedAt(),
                prato.getUpdatedAt());
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
