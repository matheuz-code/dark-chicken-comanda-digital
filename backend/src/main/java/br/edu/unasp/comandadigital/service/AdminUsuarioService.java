package br.edu.unasp.comandadigital.service;

import java.util.Locale;

import br.edu.unasp.comandadigital.dto.admin.usuario.AtualizarUsuarioAdminRequest;
import br.edu.unasp.comandadigital.dto.admin.usuario.CriarUsuarioAdminRequest;
import br.edu.unasp.comandadigital.dto.common.PaginaResponse;
import br.edu.unasp.comandadigital.dto.usuario.UsuarioResponse;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.exception.ConflitoException;
import br.edu.unasp.comandadigital.exception.RecursoNaoEncontradoException;
import br.edu.unasp.comandadigital.exception.RegraNegocioException;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUsuarioService {

    private static final Sort ORDENACAO = Sort.by(
            Sort.Order.asc("nome"),
            Sort.Order.asc("id"));

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PaginaResponse<UsuarioResponse> listar(
            PerfilUsuario perfil,
            StatusAtivacao status,
            Pageable pageable) {
        Specification<Usuario> filtros = Specification.unrestricted();
        if (perfil != null) {
            filtros = filtros.and((root, query, builder) -> builder.equal(root.get("perfil"), perfil));
        }
        if (status != null) {
            filtros = filtros.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        PageRequest pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ORDENACAO);
        Page<Usuario> usuarios = usuarioRepository.findAll(filtros, pageRequest);
        return PaginaResponse.de(usuarios.map(this::toResponse));
    }

    @Transactional
    public UsuarioResponse criar(CriarUsuarioAdminRequest request) {
        String email = normalizarEmail(request.email());
        validarEmailDisponivel(email, null);
        Usuario usuario = Usuario.novoCliente(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                normalizarTexto(request.telefone()),
                normalizarTexto(request.endereco()));
        usuario.setPerfil(request.perfil());
        usuario.setStatus(request.status());
        return toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, AtualizarUsuarioAdminRequest request) {
        Usuario usuario = buscar(id);
        String email = normalizarEmail(request.email());
        validarEmailDisponivel(email, id);
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(request.perfil());
        usuario.setTelefone(normalizarTexto(request.telefone()));
        usuario.setEndereco(normalizarTexto(request.endereco()));
        usuario.setStatus(request.status());
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        }
        return toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public void desativar(Long id, String emailAdministrador) {
        Usuario usuario = buscar(id);
        if (usuario.getEmail().equalsIgnoreCase(emailAdministrador)) {
            throw new RegraNegocioException("O administrador não pode desativar a própria conta");
        }
        if (usuario.getStatus() != StatusAtivacao.INATIVO) {
            usuario.setStatus(StatusAtivacao.INATIVO);
            usuarioRepository.saveAndFlush(usuario);
        }
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private void validarEmailDisponivel(String email, Long idAtual) {
        boolean existe = idAtual == null
                ? usuarioRepository.existsByEmailIgnoreCase(email)
                : usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, idAtual);
        if (existe) {
            throw new ConflitoException("Já existe um usuário cadastrado com este email");
        }
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getTelefone(),
                usuario.getEndereco(),
                usuario.getStatus());
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
