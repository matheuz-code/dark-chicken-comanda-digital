package br.edu.unasp.comandadigital.controller;

import br.edu.unasp.comandadigital.dto.auth.AutenticacaoResponse;
import br.edu.unasp.comandadigital.dto.auth.CadastroClienteRequest;
import br.edu.unasp.comandadigital.dto.auth.LoginRequest;
import br.edu.unasp.comandadigital.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login e cadastro público de clientes")
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Retorna um JWT válido por 8 horas")
    public ResponseEntity<AutenticacaoResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(autenticacaoService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar cliente", description = "Cria exclusivamente um usuário com perfil CLIENTE")
    public ResponseEntity<AutenticacaoResponse> register(
            @Valid @RequestBody CadastroClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(autenticacaoService.cadastrarCliente(request));
    }
}
