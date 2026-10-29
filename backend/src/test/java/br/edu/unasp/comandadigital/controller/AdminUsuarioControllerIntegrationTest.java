package br.edu.unasp.comandadigital.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void protegeCrudDeUsuariosParaSomenteAdministrador() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized());

        String cliente = registrarCliente("cliente.usuarios@teste.com");
        mockMvc.perform(get("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isForbidden());

        String admin = login("admin@email.com", "senha123");
        mockMvc.perform(get("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].senha").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].senhaHash").doesNotExist());
    }

    @Test
    void executaCrudComEmailUnicoSenhaCriptografadaESoftDelete() throws Exception {
        String admin = login("admin@email.com", "senha123");
        MvcResult createResult = mockMvc.perform(post("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson(
                                "Gerente Administrativo",
                                "GERENTE.ADMIN@TESTE.COM",
                                "senha456",
                                "GERENTE",
                                "ATIVO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("gerente.admin@teste.com"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andReturn();
        long usuarioId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("id").asLong();

        String senhaHash = jdbcTemplate.queryForObject(
                "SELECT senha_hash FROM usuario WHERE id = ?",
                String.class,
                usuarioId);
        org.assertj.core.api.Assertions.assertThat(senhaHash)
                .isNotEqualTo("senha456")
                .startsWith("$2");

        mockMvc.perform(post("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson(
                                "Duplicado",
                                "gerente.admin@teste.com",
                                "senha456",
                                "GERENTE",
                                "ATIVO")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("email")));

        mockMvc.perform(put("/api/admin/usuarios/{id}", usuarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson(
                                "Cozinheiro Administrativo",
                                "cozinha.admin@teste.com",
                                "novaSenha789",
                                "COZINHEIRO",
                                "ATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("COZINHEIRO"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cozinha.admin@teste.com","senha":"novaSenha789"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.perfil").value("COZINHEIRO"));

        mockMvc.perform(delete("/api/admin/usuarios/{id}", usuarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cozinha.admin@teste.com","senha":"novaSenha789"}
                                """))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM usuario WHERE id = ?",
                String.class,
                usuarioId)).isEqualTo("INATIVO");

        mockMvc.perform(delete("/api/admin/usuarios/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("própria conta")));
    }

    private String usuarioJson(
            String nome,
            String email,
            String senha,
            String perfil,
            String status) {
        return """
                {
                  "nome":"%s",
                  "email":"%s",
                  "senha":"%s",
                  "perfil":"%s",
                  "telefone":"(11) 99999-0000",
                  "endereco":"Rua Administrativa, 30",
                  "status":"%s"
                }
                """.formatted(nome, email, senha, perfil, status);
    }

    private String registrarCliente(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Cliente Usuários",
                                  "email":"%s",
                                  "senha":"senha456",
                                  "telefone":"(11) 98888-7777",
                                  "endereco":"Rua Cliente, 10"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return token(result);
    }

    private String login(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn();
        return token(result);
    }

    private String token(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
