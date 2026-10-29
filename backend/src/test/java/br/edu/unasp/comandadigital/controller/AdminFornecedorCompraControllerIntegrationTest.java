package br.edu.unasp.comandadigital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
class AdminFornecedorCompraControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void protegeFornecedoresEComprasConformeMatrizDePerfis() throws Exception {
        mockMvc.perform(get("/api/admin/fornecedores"))
                .andExpect(status().isUnauthorized());

        String cliente = registrarCliente("cliente.rbac.compras@teste.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Compras",
                "cozinheiro.compras@teste.com",
                "COZINHEIRO");
        String gerente = criarUsuarioInternoELogar(
                "Gerente Compras",
                "gerente.compras@teste.com",
                "GERENTE");

        mockMvc.perform(get("/api/admin/fornecedores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/fornecedores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@email.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void validaCnpjNormalizaEvitaDuplicidadeEFazSoftDelete() throws Exception {
        String admin = login("admin@email.com");

        mockMvc.perform(post("/api/admin/fornecedores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fornecedorJson("11.111.111/1111-11", "Fornecedor Inválido")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'cnpj')].mensagem")
                        .value(hasItem("O CNPJ informado é inválido")));

        long fornecedorId = criarFornecedor(
                admin,
                "11.222.333/0001-81",
                "Distribuidora SRS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cnpj FROM fornecedor WHERE id = ?",
                String.class,
                fornecedorId)).isEqualTo("11222333000181");

        mockMvc.perform(post("/api/admin/fornecedores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fornecedorJson("11222333000181", "Duplicado")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("CNPJ")));

        mockMvc.perform(delete("/api/admin/fornecedores/{id}", fornecedorId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM fornecedor WHERE id = ?",
                String.class,
                fornecedorId)).isEqualTo("INATIVO");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fornecedor WHERE id = ?",
                Long.class,
                fornecedorId)).isOne();
    }

    @Test
    void mantemCatalogoHistoricoECotacaoNormalizada() throws Exception {
        String admin = login("admin@email.com");
        long fornecedorKg = criarFornecedor(
                admin,
                "11.222.333/0001-81",
                "Fornecedor por Quilo");
        long fornecedorGrama = criarFornecedor(
                admin,
                "04.252.011/0001-10",
                "Fornecedor por Grama");
        long produtoKg = adicionarProduto(admin, fornecedorKg, 2, "35.0000", "KG");
        adicionarProduto(admin, fornecedorGrama, 2, "0.0400", "G");

        mockMvc.perform(get("/api/admin/cotacao/2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[0].fornecedorId").value((int) fornecedorKg))
                .andExpect(jsonPath("$.conteudo[0].preco").value(35.0))
                .andExpect(jsonPath("$.conteudo[0].precoNaUnidadePadrao").value(0.035))
                .andExpect(jsonPath("$.conteudo[0].unidadePadrao").value("G"))
                .andExpect(jsonPath("$.conteudo[1].precoNaUnidadePadrao").value(0.04));

        mockMvc.perform(put("/api/admin/fornecedores/{fornecedorId}/produtos/{produtoId}",
                        fornecedorKg,
                        produtoKg)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredienteId":2,"preco":32.0000,"unidadeVenda":"KG"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoNaUnidadePadrao").value(0.032));

        mockMvc.perform(get("/api/admin/fornecedores/{fornecedorId}/produtos/{produtoId}/historico-precos",
                        fornecedorKg,
                        produtoKg)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[0].preco").value(32.0))
                .andExpect(jsonPath("$.conteudo[0].usuarioNome").value("Administrador"));

        mockMvc.perform(delete("/api/admin/fornecedores/{fornecedorId}/produtos/{produtoId}",
                        fornecedorKg,
                        produtoKg)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/cotacao/2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].fornecedorId").value((int) fornecedorGrama));
    }

    @Test
    void executaCicloDaCompraERecebimentoAtualizaEstoqueCustosEPrecos() throws Exception {
        String gerente = criarUsuarioInternoELogar(
                "Gerente Recebimento",
                "gerente.recebimento@teste.com",
                "GERENTE");
        long fornecedorId = criarFornecedor(
                gerente,
                "11.222.333/0001-81",
                "Atacadista Recebimento");
        adicionarProduto(gerente, fornecedorId, 2, "35.0000", "KG");
        adicionarProduto(gerente, fornecedorId, 13, "4.0000", "UN");
        BigDecimal saldoCarneAntes = saldo(2);
        BigDecimal saldoLataAntes = saldo(13);

        long compraId = criarCompra(gerente, fornecedorId, """
                [
                  {"ingredienteId":2,"quantidade":2.0000,"precoUnitario":36.0000},
                  {"ingredienteId":13,"quantidade":10.0000,"precoUnitario":3.5000}
                ]
                """);

        mockMvc.perform(post("/api/admin/compras/{id}/enviar", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"))
                .andExpect(jsonPath("$.valorTotal").value(107.0));

        mockMvc.perform(post("/api/admin/compras/{id}/receber", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"ingredienteId":2,"lote":"LOTE-CARNE-26","validade":"2099-12-31"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEBIDO"));

        entityManager.flush();
        entityManager.clear();
        assertThat(saldo(2).subtract(saldoCarneAntes)).isEqualByComparingTo("2000.0000");
        assertThat(saldo(13).subtract(saldoLataAntes)).isEqualByComparingTo("10.0000");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT custo_unitario FROM ingrediente WHERE id = 2",
                BigDecimal.class)).isEqualByComparingTo("0.0360");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT custo_unitario FROM ingrediente WHERE id = 13",
                BigDecimal.class)).isEqualByComparingTo("3.5000");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM estoque_movimentacao
                WHERE pedido_compra_id = ? AND tipo = 'ENTRADA' AND motivo = 'COMPRA'
                """, Long.class, compraId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT lote FROM estoque_movimentacao
                WHERE pedido_compra_id = ? AND ingrediente_id = 2
                """, String.class, compraId)).isEqualTo("LOTE-CARNE-26");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT preco FROM fornecedor_produto
                WHERE fornecedor_id = ? AND ingrediente_id = 2
                """, BigDecimal.class, fornecedorId)).isEqualByComparingTo("36.0000");

        mockMvc.perform(post("/api/admin/compras/{id}/receber", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM estoque_movimentacao
                WHERE pedido_compra_id = ? AND motivo = 'COMPRA'
                """, Long.class, compraId)).isEqualTo(2L);

        mockMvc.perform(put("/api/admin/compras/{id}", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(fornecedorId,
                                "[{\"ingredienteId\":2,\"quantidade\":1,\"precoUnitario\":36}]")))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete("/api/admin/compras/{id}", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejeitaCompraDuplicadaForaDoCatalogoERecebimentoInvalido() throws Exception {
        String admin = login("admin@email.com");
        long fornecedorId = criarFornecedor(
                admin,
                "11.222.333/0001-81",
                "Fornecedor Regras");
        adicionarProduto(admin, fornecedorId, 2, "35.0000", "KG");

        mockMvc.perform(post("/api/admin/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(fornecedorId, """
                                [
                                  {"ingredienteId":2,"quantidade":1,"precoUnitario":35},
                                  {"ingredienteId":2,"quantidade":2,"precoUnitario":34}
                                ]
                                """)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("duplicados")));

        mockMvc.perform(post("/api/admin/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(fornecedorId,
                                "[{\"ingredienteId\":13,\"quantidade\":1,\"precoUnitario\":4}]")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("catálogo")));

        long compraId = criarCompra(
                admin,
                fornecedorId,
                "[{\"ingredienteId\":2,\"quantidade\":1,\"precoUnitario\":35}]" );
        mockMvc.perform(post("/api/admin/compras/{id}/receber", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("enviados")));
    }

    private long criarFornecedor(String token, String cnpj, String razaoSocial)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/fornecedores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fornecedorJson(cnpj, razaoSocial)))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long adicionarProduto(
            String token,
            long fornecedorId,
            long ingredienteId,
            String preco,
            String unidade) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/fornecedores/{id}/produtos", fornecedorId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredienteId":%d,"preco":%s,"unidadeVenda":"%s"}
                                """.formatted(ingredienteId, preco, unidade)))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long criarCompra(
            String token,
            long fornecedorId,
            String itensJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(compraJson(fornecedorId, itensJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn();
        return id(result);
    }

    private String fornecedorJson(String cnpj, String razaoSocial) {
        return """
                {
                  "razaoSocial":"%s",
                  "cnpj":"%s",
                  "telefone":"(11) 99999-0000",
                  "email":"compras@fornecedor.com",
                  "categoriasProdutos":"Carnes e bebidas",
                  "status":"ATIVO"
                }
                """.formatted(razaoSocial, cnpj);
    }

    private String compraJson(long fornecedorId, String itensJson) {
        return """
                {"fornecedorId":%d,"itens":%s}
                """.formatted(fornecedorId, itensJson);
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
                                  "nome":"Cliente Compras",
                                  "email":"%s",
                                  "senha":"senha456",
                                  "telefone":"(11) 98888-7777",
                                  "endereco":"Rua do Cliente, 26"
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

    private BigDecimal saldo(long ingredienteId) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE
                    WHEN tipo IN ('ENTRADA', 'ESTORNO') THEN quantidade
                    ELSE -quantidade
                END), 0)
                FROM estoque_movimentacao
                WHERE ingrediente_id = ?
                """, BigDecimal.class, ingredienteId);
    }

    private long id(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
