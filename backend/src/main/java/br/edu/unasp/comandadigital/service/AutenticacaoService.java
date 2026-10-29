package br.edu.unasp.comandadigital.service;

import java.util.Locale;

import br.edu.unasp.comandadigital.dto.auth.AutenticacaoResponse;
import br.edu.unasp.comandadigital.dto.auth.CadastroClienteRequest;
import br.edu.unasp.comandadigital.dto.auth.LoginRequest;
import br.edu.unasp.comandadigital.dto.usuario.UsuarioResponse;
import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.exception.ConflitoException;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import br.edu.unasp.comandadigital.service.JwtService.TokenGerado;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AutenticacaoService(
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            UsuarioRepository usuarioRepository,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AutenticacaoResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.senha()));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        return createAuthenticationResponse(usuario);
    }

    @Transactional
    public AutenticacaoResponse cadastrarCliente(CadastroClienteRequest request) {
        String email = normalizeEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um usuário cadastrado com este email");
        }

        Usuario usuario = Usuario.novoCliente(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                request.telefone().trim(),
                request.endereco().trim());

        Usuario usuarioSalvo = usuarioRepository.saveAndFlush(usuario);
        return createAuthenticationResponse(usuarioSalvo);
    }

    private AutenticacaoResponse createAuthenticationResponse(Usuario usuario) {
        TokenGerado token = jwtService.gerarToken(usuario);
        return new AutenticacaoResponse(
                token.valor(),
                AutenticacaoResponse.TIPO_BEARER,
                token.expiraEmSegundos(),
                toResponse(usuario));
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
