package br.edu.unasp.comandadigital.controller;

import static org.hamcrest.Matchers.containsString;
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
class AdminFichaTecnicaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void calculatesTheSeedDishCostAndReturnsTheRn02Warning() throws Exception {
        String admin = login("admin@email.com");

        mockMvc.perform(get("/api/admin/pratos/1/custo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pratoId").value(1))
                .andExpect(jsonPath("$.precoVenda").value(24.90))
                .andExpect(jsonPath("$.custoUnitario").value(10.2150))
                .andExpect(jsonPath("$.foodCostPercentual").value(41.02))
                .andExpect(jsonPath("$.classificacao").value("VERMELHO"))
                .andExpect(jsonPath("$.aviso", containsString("acima de 35%")));

        mockMvc.perform(get("/api/admin/pratos/1/ficha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendimento").value(1))
                .andExpect(jsonPath("$.modoPreparo").exists())
                .andExpect(jsonPath("$.itens.length()").value(4))
                .andExpect(jsonPath("$.itens[0].ingrediente.custoUnitario").exists())
                .andExpect(jsonPath("$.itens[0].custoComponente").exists());
    }

    @Test
    void createsUpdatesAndSoftDeletesTechnicalSheetItemsThenAllowsActivation() throws Exception {
        String admin = login("admin@email.com");
        long pratoId = criarPratoInativo(admin, "Prato da ficha", "100.00");

        mockMvc.perform(post("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rendimento": 1,
                                  "modoPreparo": "Misturar os ingredientes e servir.",
                                  "itens": [
                                    {
                                      "ingredienteId": 1,
                                      "quantidade": 1.0000,
                                      "unidade": "UN",
                                      "fatorCorrecao": 1.0000
                                    },
                                    {
                                      "ingredienteId": 3,
                                      "quantidade": 10.0000,
                                      "unidade": "G",
                                      "fatorCorrecao": 1.0000
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.custoUnitario").value(1.9000))
                .andExpect(jsonPath("$.foodCostPercentual").value(1.90))
                .andExpect(jsonPath("$.classificacao").value("VERDE"))
                .andExpect(jsonPath("$.aviso").doesNotExist())
                .andExpect(jsonPath("$.itens.length()").value(2));

        mockMvc.perform(put("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rendimento": 1,
                                  "modoPreparo": "Finalizar e servir.",
                                  "itens": [
                                    {
                                      "ingredienteId": 1,
                                      "quantidade": 1.0000,
                                      "unidade": "UN",
                                      "fatorCorrecao": 30.0000
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modoPreparo").value("Finalizar e servir."))
                .andExpect(jsonPath("$.custoUnitario").value(45.0000))
                .andExpect(jsonPath("$.foodCostPercentual").value(45.00))
                .andExpect(jsonPath("$.classificacao").value("VERMELHO"))
                .andExpect(jsonPath("$.aviso", containsString("acima de 35%")))
                .andExpect(jsonPath("$.itens.length()").value(1));

        Long fichaId = jdbcTemplate.queryForObject(
                "SELECT id FROM ficha_tecnica WHERE prato_id = ?",
                Long.class,
                pratoId);
        Long ativos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ficha_tecnica_item
                WHERE ficha_tecnica_id = ? AND status = 'ATIVO'
                """, Long.class, fichaId);
        Long inativos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ficha_tecnica_item
                WHERE ficha_tecnica_id = ? AND status = 'INATIVO'
                """, Long.class, fichaId);
        org.assertj.core.api.Assertions.assertThat(ativos).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(inativos).isEqualTo(1);

        mockMvc.perform(put("/api/admin/pratos/{id}", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pratoJson("Prato da ficha", "100.00", "ATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        mockMvc.perform(get("/api/cardapio/{id}", pratoId))
                .andExpect(status().isOk());
    }

    @Test
    void validatesFichaBusinessRulesAndRestrictsItToAdmin() throws Exception {
        String admin = login("admin@email.com");
        String gerente = criarUsuarioInternoELogar(
                "Gerente sem Ficha",
                "gerente.ficha@teste.com",
                "GERENTE");
        long pratoId = criarPratoInativo(admin, "Prato validação ficha", "50.00");

        mockMvc.perform(get("/api/admin/pratos/1/ficha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fichaJson(
                                "{\"ingredienteId\":1,\"quantidade\":1,\"unidade\":\"UN\",\"fatorCorrecao\":0.9}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].mensagem", containsString("maior ou igual a 1")));

        mockMvc.perform(post("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fichaJson("""
                                {"ingredienteId":1,"quantidade":1,"unidade":"UN","fatorCorrecao":1},
                                {"ingredienteId":1,"quantidade":2,"unidade":"UN","fatorCorrecao":1}
                                """)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("somente uma vez")));

        mockMvc.perform(post("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fichaJson(
                                "{\"ingredienteId\":1,\"quantidade\":1,\"unidade\":\"G\",\"fatorCorrecao\":1}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("Não é possível converter")));

        jdbcTemplate.update("UPDATE ingrediente SET status = 'INATIVO' WHERE id = 13");
        mockMvc.perform(post("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fichaJson(
                                "{\"ingredienteId\":13,\"quantidade\":1,\"unidade\":\"UN\",\"fatorCorrecao\":1}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("está inativo")));
    }

    @Test
    void preventsDuplicateSheetsAndMakesOrdersIgnoreSoftDeletedComponents() throws Exception {
        String admin = login("admin@email.com");

        mockMvc.perform(post("/api/admin/pratos/1/ficha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fichaJson(
                                "{\"ingredienteId\":1,\"quantidade\":1,\"unidade\":\"UN\",\"fatorCorrecao\":1}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O prato já possui ficha técnica"));

        mockMvc.perform(put("/api/admin/pratos/1/ficha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fichaJson(
                                "{\"ingredienteId\":1,\"quantidade\":1,\"unidade\":\"UN\",\"fatorCorrecao\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1));

        jdbcTemplate.update("""
                UPDATE estoque_movimentacao
                SET quantidade = 1.0000
                WHERE ingrediente_id = 2 AND lote = 'SEED-INICIAL'
                """);
        String cliente = registrarCliente("cliente.ficha.pedido@teste.com");
        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"pratoId":1,"quantidade":1}],
                                  "enderecoEntrega": "Rua do Teste, 10",
                                  "formaPagamento": "PIX"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private long criarPratoInativo(String token, String nome, String preco) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pratoJson(nome, preco, "INATIVO")))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asLong();
    }

    private String pratoJson(String nome, String preco, String status) {
        return """
                {
                  "categoriaId": 1,
                  "nome": "%s",
                  "descricao": "Prato criado para testar ficha técnica",
                  "precoVenda": %s,
                  "tempoPreparoMin": 10,
                  "status": "%s"
                }
                """.formatted(nome, preco, status);
    }

    private String fichaJson(String itens) {
        return """
                {
                  "rendimento": 1,
                  "modoPreparo": "Preparar e servir.",
                  "itens": [%s]
                }
                """.formatted(itens);
    }

    private String criarUsuarioInternoELogar(String nome, String email, String perfil)
            throws Exception {
        jdbcTemplate.update("""
                INSERT INTO usuario (nome, email, senha_hash, perfil, status)
                SELECT ?, ?, senha_hash, ?, 'ATIVO'
                FROM usuario WHERE id = 1
                """, nome, email, perfil);
        return login(email);
    }

    private String registrarCliente(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Ficha",
                                  "email": "%s",
                                  "senha": "senha456",
                                  "telefone": "(11) 98888-7777",
                                  "endereco": "Rua do Cliente, 10"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token")
                .asText();
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"senha123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
