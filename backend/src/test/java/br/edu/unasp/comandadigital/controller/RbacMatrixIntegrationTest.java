package br.edu.unasp.comandadigital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class RbacMatrixIntegrationTest {

    private enum Perfil {
        ADMIN,
        GERENTE,
        COZINHEIRO,
        CLIENTE
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Map<Perfil, String> tokens = new EnumMap<>(Perfil.class);

    @BeforeEach
    void autenticarTodosOsPerfis() throws Exception {
        tokens.put(Perfil.ADMIN, login("admin@email.com", "senha123"));
        tokens.put(
                Perfil.GERENTE,
                criarUsuarioInternoELogar(
                        "Gerente RBAC",
                        "gerente.matriz@teste.com",
                        Perfil.GERENTE));
        tokens.put(
                Perfil.COZINHEIRO,
                criarUsuarioInternoELogar(
                        "Cozinheiro RBAC",
                        "cozinheiro.matriz@teste.com",
                        Perfil.COZINHEIRO));
        tokens.put(Perfil.CLIENTE, registrarCliente("cliente.matriz@teste.com"));
    }

    @Test
    void aplicaAcessoPublicoERejeitaCredenciaisAusentesOuInvalidas() throws Exception {
        mockMvc.perform(get("/api/cardapio"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cardapio/1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/status"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/pedidos/meus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void aplicaMatrizCompletaDoSrsEmTodosOsGruposProtegidos() throws Exception {
        Set<Perfil> somenteAdmin = EnumSet.of(Perfil.ADMIN);
        verificarMatriz("/api/admin/categorias", somenteAdmin);
        verificarMatriz("/api/admin/pratos", somenteAdmin);
        verificarMatriz("/api/admin/pratos/1/ficha", somenteAdmin);
        verificarMatriz("/api/admin/usuarios", somenteAdmin);

        Set<Perfil> adminOuGerente = EnumSet.of(Perfil.ADMIN, Perfil.GERENTE);
        verificarMatriz("/api/admin/ingredientes", adminOuGerente);
        verificarMatriz("/api/admin/dashboard", adminOuGerente);
        verificarMatriz("/api/admin/estoque/saldo", adminOuGerente);
        verificarMatriz("/api/admin/fornecedores", adminOuGerente);
        verificarMatriz("/api/admin/compras", adminOuGerente);

        verificarMatriz(
                "/api/admin/pedidos",
                EnumSet.of(Perfil.ADMIN, Perfil.GERENTE, Perfil.COZINHEIRO));
        verificarMatriz("/api/pedidos/meus", EnumSet.of(Perfil.CLIENTE));
    }

    private void verificarMatriz(String endpoint, Set<Perfil> perfisPermitidos) throws Exception {
        for (Perfil perfil : Perfil.values()) {
            var resultadoEsperado = perfisPermitidos.contains(perfil)
                    ? status().isOk()
                    : status().isForbidden();

            mockMvc.perform(get(endpoint)
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(perfil))))
                    .andExpect(resultadoEsperado);
        }
    }

    private String criarUsuarioInternoELogar(String nome, String email, Perfil perfil)
            throws Exception {
        jdbcTemplate.update("""
                INSERT INTO usuario (nome, email, senha_hash, perfil, status)
                SELECT ?, ?, senha_hash, ?, 'ATIVO'
                FROM usuario WHERE id = 1
                """, nome, email, perfil.name());
        return login(email, "senha123");
    }

    private String registrarCliente(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Matriz RBAC",
                                  "email": "%s",
                                  "senha": "senha456",
                                  "telefone": "(11) 98888-7777",
                                  "endereco": "Rua do Cliente, 10"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.perfil").value("CLIENTE"))
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
                .path("token")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
