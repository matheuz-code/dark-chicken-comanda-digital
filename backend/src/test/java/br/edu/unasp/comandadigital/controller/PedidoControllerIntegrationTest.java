package br.edu.unasp.comandadigital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

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
class PedidoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void requiresAClientJwtForEveryOrderEndpoint() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("PIX", "{\"pratoId\":5,\"quantidade\":1}")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/pedidos/1")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/pedidos/meus"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/pedidos/1/status"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("PIX", "{\"pratoId\":5,\"quantidade\":1}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createsAValidatedPaidSiteOrderUsingCurrentDatabasePrices() throws Exception {
        String token = registrarCliente("cliente.pedido@teste.com");
        long pedidosAntes = contar("SELECT COUNT(*) FROM pedido");

        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"pratoId": 1, "quantidade": 2, "observacoes": "Sem molho"},
                                    {"pratoId": 5, "quantidade": 1}
                                  ],
                                  "enderecoEntrega": "Rua da Entrega, 200",
                                  "formaPagamento": "PIX",
                                  "observacoes": "Tocar a campainha"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/pedidos/")))
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.statusPagamento").value("PAGO"))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.valorTotal").value(56.80))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].nomePrato").value("X-Burger"))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(24.90))
                .andExpect(jsonPath("$.itens[0].subtotal").value(49.80))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        long pedidoId = body.path("id").asLong();
        Map<String, Object> pedido = jdbcTemplate.queryForMap("""
                SELECT canal, status, status_pagamento, forma_pagamento, valor_total
                FROM pedido WHERE id = ?
                """, pedidoId);

        assertThat(contar("SELECT COUNT(*) FROM pedido")).isEqualTo(pedidosAntes + 1);
        assertThat(pedido.get("canal")).isEqualTo("SITE");
        assertThat(pedido.get("status")).isEqualTo("RECEBIDO");
        assertThat(pedido.get("status_pagamento")).isEqualTo("PAGO");
        assertThat(pedido.get("forma_pagamento")).isEqualTo("PIX");
        assertThat((BigDecimal) pedido.get("valor_total"))
                .isEqualByComparingTo("56.80");
        assertThat(contar("SELECT COUNT(*) FROM pedido_item WHERE pedido_id = " + pedidoId))
                .isEqualTo(2);
        assertThat(contar("SELECT COUNT(*) FROM pedido_status_historico WHERE pedido_id = " + pedidoId))
                .isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM estoque_movimentacao WHERE pedido_id = " + pedidoId))
                .isZero();
    }

    @Test
    void validatesTheCheckoutContractBeforeCallingBusinessRules() throws Exception {
        String token = registrarCliente("cliente.validacao@teste.com");

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [],
                                  "enderecoEntrega": " ",
                                  "formaPagamento": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos[*].campo", hasItems(
                        "itens", "enderecoEntrega", "formaPagamento")));

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("PIX", "{\"pratoId\":0,\"quantidade\":0}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItems(
                        "itens[0].pratoId", "itens[0].quantidade")));
    }

    @Test
    void rejectsDuplicateAndUnavailableDishesWithUnprocessableEntity() throws Exception {
        String token = registrarCliente("cliente.prato@teste.com");

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("CARTAO", """
                                {"pratoId":5,"quantidade":1},
                                {"pratoId":5,"quantidade":2}
                                """)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("somente uma vez")));

        jdbcTemplate.update("UPDATE prato SET status = 'INATIVO' WHERE id = 5");

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("DINHEIRO", "{\"pratoId\":5,\"quantidade\":1}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("não está disponível")));
    }

    @Test
    void rejectsADishWithoutACompleteTechnicalSheet() throws Exception {
        String token = registrarCliente("cliente.ficha@teste.com");
        jdbcTemplate.update("DELETE FROM ficha_tecnica_item WHERE ficha_tecnica_id = 5");
        jdbcTemplate.update("DELETE FROM ficha_tecnica WHERE id = 5");

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("PIX", "{\"pratoId\":5,\"quantidade\":1}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("não possui ficha técnica")));
    }

    @Test
    void rejectsInsufficientStockAfterConvertingTheTechnicalSheetUnit() throws Exception {
        String token = registrarCliente("cliente.estoque@teste.com");
        jdbcTemplate.update("""
                UPDATE ficha_tecnica_item
                SET quantidade = 0.1800, unidade = 'KG'
                WHERE ficha_tecnica_id = 1 AND ingrediente_id = 2
                """);
        jdbcTemplate.update("""
                UPDATE estoque_movimentacao
                SET quantidade = 188.0000
                WHERE ingrediente_id = 2 AND lote = 'SEED-INICIAL'
                """);

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("PIX", "{\"pratoId\":1,\"quantidade\":1}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.mensagem", containsString("Carne bovina")))
                .andExpect(jsonPath("$.mensagem", containsString("necessário 189 G")))
                .andExpect(jsonPath("$.mensagem", containsString("disponível 188 G")));
    }

    @Test
    void paginatesOnlyTheAuthenticatedClientsOwnOrders() throws Exception {
        String tokenA = registrarCliente("cliente.a@teste.com");
        String tokenB = registrarCliente("cliente.b@teste.com");
        criarPedido(tokenA, "PIX", "{\"pratoId\":5,\"quantidade\":1}");
        criarPedido(tokenA, "CARTAO", "{\"pratoId\":1,\"quantidade\":2}");
        criarPedido(tokenB, "DINHEIRO", "{\"pratoId\":5,\"quantidade\":3}");

        mockMvc.perform(get("/api/pedidos/meus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].quantidadeItens").value(2));
    }

    @Test
    void exposesTheTimelineOnlyToTheOrderOwner() throws Exception {
        String tokenA = registrarCliente("cliente.status.a@teste.com");
        String tokenB = registrarCliente("cliente.status.b@teste.com");
        long pedidoId = criarPedido(
                tokenA,
                "PIX",
                "{\"pratoId\":5,\"quantidade\":1}");

        mockMvc.perform(get("/api/pedidos/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedidoId))
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.statusPagamento").value("PAGO"))
                .andExpect(jsonPath("$.timeline.length()").value(1))
                .andExpect(jsonPath("$.timeline[0].status").value("RECEBIDO"))
                .andExpect(jsonPath("$.timeline[0].registradoEm").exists());

        mockMvc.perform(get("/api/pedidos/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem", containsString("cliente autenticado")));
    }

    @Test
    void detalheDoPedidoPreservaContratoESomenteDonoPodeLer() throws Exception {
        String owner = registrarCliente("owner.detalhe@example.invalid");
        String other = registrarCliente("other.detalhe@example.invalid");
        long id = criarPedido(owner, "PIX", "{\"pratoId\":5,\"quantidade\":2}");
        mockMvc.perform(get("/api/pedidos/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.valorTotal").value(14.0))
                .andExpect(jsonPath("$.criadoEm").isString())
                .andExpect(jsonPath("$.itens[0].pratoId").value(5))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(7.0));
        mockMvc.perform(get("/api/pedidos/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pedidos/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAdmin()))
                .andExpect(status().isForbidden());
    }

    private String registrarCliente(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente de Teste",
                                  "email": "%s",
                                  "senha": "senha456",
                                  "telefone": "(11) 98888-7777",
                                  "endereco": "Rua do Cadastro, 100"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token")
                .asText();
    }

    private String loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@email.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token")
                .asText();
    }

    private long criarPedido(String token, String formaPagamento, String itensJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(formaPagamento, itensJson)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asLong();
    }

    private String pedidoJson(String formaPagamento, String itensJson) {
        return """
                {
                  "itens": [%s],
                  "enderecoEntrega": "Rua da Entrega, 200",
                  "formaPagamento": "%s"
                }
                """.formatted(itensJson, formaPagamento);
    }

    private long contar(String sql) {
        Long resultado = jdbcTemplate.queryForObject(sql, Long.class);
        return resultado == null ? 0 : resultado;
    }
}
