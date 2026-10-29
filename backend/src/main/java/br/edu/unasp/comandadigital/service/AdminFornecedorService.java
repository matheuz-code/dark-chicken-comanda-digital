package br.edu.unasp.comandadigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.dto.admin.fornecedor.CotacaoFornecedorResponse;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorProdutoRequest;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorProdutoResponse;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorRequest;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.FornecedorResponse;
import br.edu.unasp.comandadigital.dto.admin.fornecedor.HistoricoPrecoResponse;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.entity.Fornecedor;
import br.edu.unasp.comandadigital.entity.FornecedorProduto;
import br.edu.unasp.comandadigital.entity.FornecedorProdutoPrecoHistorico;
import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import br.edu.unasp.comandadigital.exception.ConflitoException;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.FornecedorProdutoPrecoHistoricoRepository;
import br.edu.unasp.comandadigital.repository.FornecedorProdutoRepository;
import br.edu.unasp.comandadigital.repository.FornecedorRepository;
import br.edu.unasp.comandadigital.repository.IngredienteRepository;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminFornecedorService {

    private static final Sort ORDENACAO_FORNECEDORES = Sort.by(
            Sort.Order.asc("razaoSocial"),
            Sort.Order.asc("id"));
    private static final Sort ORDENACAO_CATALOGO = Sort.by(
            Sort.Order.asc("ingrediente.nome"),
            Sort.Order.asc("id"));
    private static final Sort ORDENACAO_HISTORICO = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorProdutoRepository fornecedorProdutoRepository;
    private final FornecedorProdutoPrecoHistoricoRepository historicoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConversorUnidadeService conversorUnidadeService;

    public AdminFornecedorService(
            FornecedorRepository fornecedorRepository,
            FornecedorProdutoRepository fornecedorProdutoRepository,
            FornecedorProdutoPrecoHistoricoRepository historicoRepository,
            IngredienteRepository ingredienteRepository,
            UsuarioRepository usuarioRepository,
            ConversorUnidadeService conversorUnidadeService) {
        this.fornecedorRepository = fornecedorRepository;
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
        this.historicoRepository = historicoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.conversorUnidadeService = conversorUnidadeService;
    }

    public PaginaResponse<FornecedorResponse> listar(
            StatusAtivacao status,
            Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO_FORNECEDORES);
        Page<Fornecedor> fornecedores = status == null
                ? fornecedorRepository.findAll(pageRequest)
                : fornecedorRepository.findByStatus(status, pageRequest);
        return PaginaResponse.de(fornecedores.map(this::toFornecedorResponse));
    }

    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        String cnpj = normalizarCnpj(request.cnpj());
        validarCnpjDisponivel(cnpj, null);
        Fornecedor fornecedor = Fornecedor.criar(
                request.razaoSocial().trim(),
                cnpj,
                normalizarOpcional(request.telefone()),
                normalizarEmail(request.email()),
                normalizarOpcional(request.categoriasProdutos()),
                request.status());
        return toFornecedorResponse(fornecedorRepository.saveAndFlush(fornecedor));
    }

    @Transactional
    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = buscarFornecedor(id);
        String cnpj = normalizarCnpj(request.cnpj());
        validarCnpjDisponivel(cnpj, id);
        fornecedor.setRazaoSocial(request.razaoSocial().trim());
        fornecedor.setCnpj(cnpj);
        fornecedor.setTelefone(normalizarOpcional(request.telefone()));
        fornecedor.setEmail(normalizarEmail(request.email()));
        fornecedor.setCategoriasProdutos(normalizarOpcional(request.categoriasProdutos()));
        fornecedor.setStatus(request.status());
        return toFornecedorResponse(fornecedorRepository.saveAndFlush(fornecedor));
    }

    @Transactional
    public void desativar(Long id) {
        Fornecedor fornecedor = buscarFornecedor(id);
        if (fornecedor.getStatus() != StatusAtivacao.INATIVO) {
            fornecedor.setStatus(StatusAtivacao.INATIVO);
            fornecedorRepository.saveAndFlush(fornecedor);
        }
    }

    public PaginaResponse<FornecedorProdutoResponse> listarCatalogo(
            Long fornecedorId,
            StatusAtivacao status,
            Pageable pageable) {
        buscarFornecedor(fornecedorId);
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO_CATALOGO);
        Page<FornecedorProduto> produtos = status == null
                ? fornecedorProdutoRepository.findByFornecedorId(fornecedorId, pageRequest)
                : fornecedorProdutoRepository.findByFornecedorIdAndStatus(
                        fornecedorId,
                        status,
                        pageRequest);
        return PaginaResponse.de(produtos.map(this::toProdutoResponse));
    }

    @Transactional
    public FornecedorProdutoResponse adicionarProduto(
            String emailUsuario,
            Long fornecedorId,
            FornecedorProdutoRequest request) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        Fornecedor fornecedor = buscarFornecedorAtivo(fornecedorId);
        Ingrediente ingrediente = buscarIngredienteAtivo(request.ingredienteId());
        validarCompatibilidade(request.unidadeVenda(), ingrediente.getUnidadePadrao());

        FornecedorProduto produto = fornecedorProdutoRepository
                .findByFornecedorIdAndIngredienteId(fornecedorId, ingrediente.getId())
                .orElse(null);
        if (produto != null && produto.getStatus() == StatusAtivacao.ATIVO) {
            throw new ConflitoException(
                    "O ingrediente já está ativo no catálogo deste fornecedor");
        }
        if (produto == null) {
            produto = fornecedorProdutoRepository.saveAndFlush(FornecedorProduto.criar(
                    fornecedor,
                    ingrediente,
                    request.preco(),
                    request.unidadeVenda()));
        } else {
            produto.setPreco(request.preco());
            produto.setUnidadeVenda(request.unidadeVenda());
            produto.setStatus(StatusAtivacao.ATIVO);
            produto = fornecedorProdutoRepository.saveAndFlush(produto);
        }
        registrarHistorico(produto, usuario);
        return toProdutoResponse(produto);
    }

    @Transactional
    public FornecedorProdutoResponse atualizarProduto(
            String emailUsuario,
            Long fornecedorId,
            Long produtoId,
            FornecedorProdutoRequest request) {
        Usuario usuario = carregarUsuarioInterno(emailUsuario);
        buscarFornecedorAtivo(fornecedorId);
        FornecedorProduto produto = buscarProduto(fornecedorId, produtoId);
        if (!produto.getIngrediente().getId().equals(request.ingredienteId())) {
            throw new RegraNegocioException(
                    "O ingrediente de um item de catálogo não pode ser substituído");
        }
        validarCompatibilidade(
                request.unidadeVenda(),
                produto.getIngrediente().getUnidadePadrao());
        boolean alterouPreco = produto.getPreco().compareTo(request.preco()) != 0
                || produto.getUnidadeVenda() != request.unidadeVenda();
        produto.setPreco(request.preco());
        produto.setUnidadeVenda(request.unidadeVenda());
        produto.setStatus(StatusAtivacao.ATIVO);
        produto = fornecedorProdutoRepository.saveAndFlush(produto);
        if (alterouPreco) registrarHistorico(produto, usuario);
        return toProdutoResponse(produto);
    }

    @Transactional
    public void desativarProduto(Long fornecedorId, Long produtoId) {
        buscarFornecedor(fornecedorId);
        FornecedorProduto produto = buscarProduto(fornecedorId, produtoId);
        if (produto.getStatus() != StatusAtivacao.INATIVO) {
            produto.setStatus(StatusAtivacao.INATIVO);
            fornecedorProdutoRepository.saveAndFlush(produto);
        }
    }

    public PaginaResponse<HistoricoPrecoResponse> listarHistorico(
            Long fornecedorId,
            Long produtoId,
            Pageable pageable) {
        buscarFornecedor(fornecedorId);
        FornecedorProduto produto = buscarProduto(fornecedorId, produtoId);
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO_HISTORICO);
        return PaginaResponse.de(historicoRepository
                .findByFornecedorProdutoId(produtoId, pageRequest)
                .map(historico -> toHistoricoResponse(historico, produto.getIngrediente())));
    }

    public PaginaResponse<CotacaoFornecedorResponse> cotar(
            Long ingredienteId,
            Pageable pageable) {
        Ingrediente ingrediente = buscarIngredienteAtivo(ingredienteId);
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize());
        Page<Long> ids = fornecedorProdutoRepository.findIdsCotacaoAtiva(
                ingredienteId,
                pageRequest);
        Map<Long, FornecedorProduto> produtos = fornecedorProdutoRepository
                .findByIdIn(ids.getContent())
                .stream()
                .collect(Collectors.toMap(FornecedorProduto::getId, Function.identity()));
        List<CotacaoFornecedorResponse> conteudo = ids.getContent().stream()
                .map(produtos::get)
                .map(this::toCotacaoResponse)
                .toList();
        return PaginaResponse.de(new PageImpl<>(
                conteudo,
                pageRequest,
                ids.getTotalElements()));
    }

    private void registrarHistorico(FornecedorProduto produto, Usuario usuario) {
        historicoRepository.save(FornecedorProdutoPrecoHistorico.registrar(
                produto,
                produto.getPreco(),
                produto.getUnidadeVenda(),
                usuario));
    }

    private BigDecimal normalizarPreco(
            BigDecimal preco,
            UnidadeMedida unidadeVenda,
            UnidadeMedida unidadePadrao) {
        BigDecimal quantidadePadrao = conversorUnidadeService.converter(
                BigDecimal.ONE,
                unidadeVenda,
                unidadePadrao);
        return preco.divide(quantidadePadrao, 8, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private void validarCompatibilidade(UnidadeMedida origem, UnidadeMedida destino) {
        conversorUnidadeService.converter(BigDecimal.ONE, origem, destino);
    }

    private Fornecedor buscarFornecedor(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Fornecedor não encontrado"));
    }

    private Fornecedor buscarFornecedorAtivo(Long id) {
        Fornecedor fornecedor = buscarFornecedor(id);
        if (fornecedor.getStatus() != StatusAtivacao.ATIVO) {
            throw new RegraNegocioException("O fornecedor está inativo");
        }
        return fornecedor;
    }

    private Ingrediente buscarIngredienteAtivo(Long id) {
        return ingredienteRepository.findById(id)
                .filter(ingrediente -> ingrediente.getStatus() == StatusAtivacao.ATIVO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ingrediente não encontrado ou inativo"));
    }

    private FornecedorProduto buscarProduto(Long fornecedorId, Long produtoId) {
        return fornecedorProdutoRepository.findByIdAndFornecedorId(produtoId, fornecedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado no catálogo deste fornecedor"));
    }

    private Usuario carregarUsuarioInterno(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .filter(usuario -> usuario.getStatus() == StatusAtivacao.ATIVO)
                .filter(usuario -> usuario.getPerfil() != PerfilUsuario.CLIENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário interno autenticado não encontrado ou inativo"));
    }

    private void validarCnpjDisponivel(String cnpj, Long idAtual) {
        boolean existe = idAtual == null
                ? fornecedorRepository.existsByCnpj(cnpj)
                : fornecedorRepository.existsByCnpjAndIdNot(cnpj, idAtual);
        if (existe) {
            throw new ConflitoException("Já existe um fornecedor cadastrado com este CNPJ");
        }
    }

    private String normalizarCnpj(String cnpj) {
        return cnpj.replaceAll("\\D", "");
    }

    private String normalizarEmail(String email) {
        String valor = normalizarOpcional(email);
        return valor == null ? null : valor.toLowerCase();
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private FornecedorResponse toFornecedorResponse(Fornecedor fornecedor) {
        return new FornecedorResponse(
                fornecedor.getId(),
                fornecedor.getRazaoSocial(),
                fornecedor.getCnpj(),
                fornecedor.getTelefone(),
                fornecedor.getEmail(),
                fornecedor.getCategoriasProdutos(),
                fornecedor.getStatus(),
                fornecedor.getCreatedAt(),
                fornecedor.getUpdatedAt());
    }

    private FornecedorProdutoResponse toProdutoResponse(FornecedorProduto produto) {
        Ingrediente ingrediente = produto.getIngrediente();
        return new FornecedorProdutoResponse(
                produto.getId(),
                produto.getFornecedor().getId(),
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getSku(),
                produto.getPreco(),
                produto.getUnidadeVenda(),
                normalizarPreco(
                        produto.getPreco(),
                        produto.getUnidadeVenda(),
                        ingrediente.getUnidadePadrao()),
                ingrediente.getUnidadePadrao(),
                produto.getStatus(),
                produto.getCreatedAt(),
                produto.getUpdatedAt());
    }

    private CotacaoFornecedorResponse toCotacaoResponse(FornecedorProduto produto) {
        Fornecedor fornecedor = produto.getFornecedor();
        Ingrediente ingrediente = produto.getIngrediente();
        return new CotacaoFornecedorResponse(
                produto.getId(),
                fornecedor.getId(),
                fornecedor.getRazaoSocial(),
                fornecedor.getCnpj(),
                ingrediente.getId(),
                ingrediente.getNome(),
                produto.getPreco(),
                produto.getUnidadeVenda(),
                normalizarPreco(
                        produto.getPreco(),
                        produto.getUnidadeVenda(),
                        ingrediente.getUnidadePadrao()),
                ingrediente.getUnidadePadrao(),
                produto.getUpdatedAt());
    }

    private HistoricoPrecoResponse toHistoricoResponse(
            FornecedorProdutoPrecoHistorico historico,
            Ingrediente ingrediente) {
        Usuario usuario = historico.getUsuario();
        return new HistoricoPrecoResponse(
                historico.getId(),
                historico.getPreco(),
                historico.getUnidadeVenda(),
                normalizarPreco(
                        historico.getPreco(),
                        historico.getUnidadeVenda(),
                        ingrediente.getUnidadePadrao()),
                ingrediente.getUnidadePadrao(),
                usuario == null ? null : usuario.getId(),
                usuario == null ? null : usuario.getNome(),
                historico.getCreatedAt());
    }
}
