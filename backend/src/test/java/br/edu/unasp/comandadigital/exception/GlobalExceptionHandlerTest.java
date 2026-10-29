package br.edu.unasp.comandadigital.exception;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unasp.comandadigital.dto.auth.LoginRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAStandardizedValidationError() throws Exception {
        mockMvc.perform(post("/probe/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"email-invalido","senha":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Existem campos inválidos na requisição"))
                .andExpect(jsonPath("$.caminho").value("/probe/validacao"))
                .andExpect(jsonPath("$.campos[*].campo", hasItems("email", "senha")));
    }

    @Test
    void mapsAuthenticationAndAuthorizationErrors() throws Exception {
        mockMvc.perform(get("/probe/nao-autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"));

        mockMvc.perform(get("/probe/sem-permissao"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Acesso negado"));
    }

    @Test
    void mapsDomainErrorsToTheirRequiredHttpStatus() throws Exception {
        mockMvc.perform(get("/probe/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/probe/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(get("/probe/regra-negocio"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @PostMapping("/validacao")
        void validate(@Valid @RequestBody LoginRequest request) {
        }

        @GetMapping("/nao-autenticado")
        void unauthenticated() {
            throw new BadCredentialsException("não deve aparecer na resposta");
        }

        @GetMapping("/sem-permissao")
        void forbidden() {
            throw new AccessDeniedException("não deve aparecer na resposta");
        }

        @GetMapping("/nao-encontrado")
        void notFound() {
            throw new RecursoNaoEncontradoException("Recurso de teste não encontrado");
        }

        @GetMapping("/conflito")
        void conflict() {
            throw new ConflitoException("Conflito de teste");
        }

        @GetMapping("/regra-negocio")
        void businessRule() {
            throw new RegraNegocioException("Regra de teste violada");
        }
    }
}
