package br.edu.unasp.comandadigital.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class AdminCatalogoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesTheSrsRoleMatrixForCatalogAndIngredients() throws Exception {
        mockMvc.perform(get("/api/admin/categorias"))
                .andExpect(status().isUnauthorized());

        String cliente = registrarCliente("cliente.rbac@teste.com");
        mockMvc.perform(get("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isForbidden());

        String gerente = criarUsuarioInternoELogar(
                "Gerente Catálogo",
                "gerente.catalogo@teste.com",
                "GERENTE");
        mockMvc.perform(get("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isForbidden());

        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Catálogo",
                "cozinheiro.catalogo@teste.com",
                "COZINHEIRO");
        mockMvc.perform(get("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isForbidden());

        String admin = login("admin@email.com");
        mockMvc.perform(get("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void createsUpdatesListsAndSoftDeletesACategory() throws Exception {
        String admin = login("admin@email.com");

        MvcResult createResult = mockMvc.perform(post("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Sobremesas",
                                  "descricao": "Doces da casa",
                                  "ordem": 5,
                                  "status": "ATIVO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Sobremesas"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andReturn();
        long categoriaId = id(createResult);

        mockMvc.perform(put("/api/admin/categorias/{id}", categoriaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Sobremesas especiais",
                                  "descricao": "Doces preparados na casa",
                                  "ordem": 6,
                                  "status": "ATIVO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Sobremesas especiais"))
                .andExpect(jsonPath("$.ordem").value(6));

        mockMvc.perform(get("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .queryParam("status", "ATIVO")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.totalElementos").value(5));

        mockMvc.perform(post("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"lanches","ordem":20,"status":"ATIVO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("este nome")));

        mockMvc.perform(delete("/api/admin/categorias/{id}", categoriaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .queryParam("status", "INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].id", hasItem((int) categoriaId)));

        Long registros = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categoria WHERE id = ?",
                Long.class,
                categoriaId);
        String statusBanco = jdbcTemplate.queryForObject(
                "SELECT status FROM categoria WHERE id = ?",
                String.class,
                categoriaId);
        org.assertj.core.api.Assertions.assertThat(registros).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(statusBanco).isEqualTo("INATIVO");
    }

    @Test
    void appliesRn01AndCompletesTheDishCrudWithoutPhysicalDeletion() throws Exception {
        String admin = login("admin@email.com");
        String pratoJsonInativo = pratoJson("Brownie artesanal", "INATIVO");

        MvcResult createResult = mockMvc.perform(post("/api/admin/pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pratoJsonInativo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria.id").value(1))
                .andExpect(jsonPath("$.precoVenda").value(15.90))
                .andExpect(jsonPath("$.status").value("INATIVO"))
                .andReturn();
        long pratoId = id(createResult);

        mockMvc.perform(put("/api/admin/pratos/{id}", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pratoJson("Brownie artesanal", "ATIVO")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("ficha técnica completa")));

        jdbcTemplate.update("""
                INSERT INTO ficha_tecnica (prato_id, rendimento, modo_preparo)
                VALUES (?, 1, 'Assar, porcionar e servir.')
                """, pratoId);
        Long fichaId = jdbcTemplate.queryForObject(
                "SELECT id FROM ficha_tecnica WHERE prato_id = ?",
                Long.class,
                pratoId);
        jdbcTemplate.update("""
                INSERT INTO ficha_tecnica_item (
                    ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao
                ) VALUES (?, 1, 1.0000, 'UN', 1.0000)
                """, fichaId);

        mockMvc.perform(put("/api/admin/pratos/{id}", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pratoJson("Brownie artesanal", "ATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        mockMvc.perform(get("/api/cardapio/{id}", pratoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Brownie artesanal"));

        mockMvc.perform(delete("/api/admin/pratos/{id}", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cardapio/{id}", pratoId))
                .andExpect(status().isNotFound());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM prato WHERE id = ?",
                        String.class,
                        pratoId))
                .isEqualTo("INATIVO");
    }

    @Test
    void letsAManagerCompleteTheIngredientCrudAndProtectsUniqueSkus() throws Exception {
        String gerente = criarUsuarioInternoELogar(
                "Gerente Estoque",
                "gerente.estoque@teste.com",
                "GERENTE");

        MvcResult createResult = mockMvc.perform(post("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Farinha de trigo",
                                  "sku": "farinha-teste",
                                  "unidadePadrao": "G",
                                  "estoqueMinimo": 2500.0000,
                                  "custoUnitario": 0.0065,
                                  "status": "ATIVO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("FARINHA-TESTE"))
                .andExpect(jsonPath("$.unidadePadrao").value("G"))
                .andReturn();
        long ingredienteId = id(createResult);

        mockMvc.perform(put("/api/admin/ingredientes/{id}", ingredienteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Farinha especial",
                                  "sku": "FARINHA-TESTE",
                                  "unidadePadrao": "KG",
                                  "estoqueMinimo": 3.0000,
                                  "custoUnitario": 6.9000,
                                  "status": "ATIVO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Farinha especial"))
                .andExpect(jsonPath("$.custoUnitario").value(6.9000));

        mockMvc.perform(post("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Outro pão",
                                  "sku": "pao-brioche",
                                  "unidadePadrao": "UN",
                                  "estoqueMinimo": 1,
                                  "custoUnitario": 1,
                                  "status": "ATIVO"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("este SKU")));

        mockMvc.perform(delete("/api/admin/ingredientes/{id}", ingredienteId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .queryParam("status", "INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].id", hasItem((int) ingredienteId)));

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM ingrediente WHERE id = ?",
                        String.class,
                        ingredienteId))
                .isEqualTo("INATIVO");
    }

    @Test
    void returnsStandardValidationErrorsForInvalidAdminDtos() throws Exception {
        String admin = login("admin@email.com");

        mockMvc.perform(post("/api/admin/pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoriaId": 0,
                                  "nome": " ",
                                  "precoVenda": -1,
                                  "tempoPreparoMin": 0,
                                  "status": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("categoriaId")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nome")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("precoVenda")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("tempoPreparoMin")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("status")));
    }

    private String pratoJson(String nome, String status) {
        return """
                {
                  "categoriaId": 1,
                  "nome": "%s",
                  "descricao": "Brownie com chocolate",
                  "fotoUrl": "/images/brownie.jpg",
                  "precoVenda": 15.90,
                  "tempoPreparoMin": 12,
                  "status": "%s"
                }
                """.formatted(nome, status);
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
                                  "nome": "Cliente RBAC",
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

    private long id(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("id").asLong();
    }
}
