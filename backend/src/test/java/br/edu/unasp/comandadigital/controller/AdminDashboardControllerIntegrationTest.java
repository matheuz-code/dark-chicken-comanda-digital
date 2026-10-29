package br.edu.unasp.comandadigital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
class AdminDashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void protegeDashboardParaAdminEGerente() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        String cliente = registrarCliente("cliente.rbac.dashboard@teste.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Dashboard",
                "cozinheiro.dashboard@teste.com",
                "COZINHEIRO");
        String gerente = criarUsuarioInternoELogar(
                "Gerente Dashboard",
                "gerente.dashboard@teste.com",
                "GERENTE");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/dashboard/top-pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/dashboard/top-pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@email.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void calculaKpisDoDiaEFoodCostPonderadoEExcluiEstorno() throws Exception {
        String cliente = registrarCliente("cliente.kpi.dashboard@teste.com");
        String admin = login("admin@email.com");
        criarPedido(cliente, 5, 2);
        long pedidoCancelado = criarPedido(cliente, 1, 1);
        cancelarPedido(admin, pedidoCancelado);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.faturamento").value(14.0))
                .andExpect(jsonPath("$.totalPedidos").value(1))
                .andExpect(jsonPath("$.ticketMedio").value(14.0))
                .andExpect(jsonPath("$.custoIngredientesEstimado").value(8.0))
                .andExpect(jsonPath("$.foodCostMedioPercentual").value(57.14));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status_pagamento FROM pedido WHERE id = ?",
                String.class,
                pedidoCancelado)).isEqualTo("ESTORNADO");
    }

    @Test
    void retornaTopCincoOrdenadoPorQuantidadeEValidaLimite() throws Exception {
        String cliente = registrarCliente("cliente.top.dashboard@teste.com");
        String admin = login("admin@email.com");
        criarPedido(cliente, 5, 3);
        criarPedido(cliente, 1, 1);

        mockMvc.perform(get("/api/admin/dashboard/top-pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pratoId").value(5))
                .andExpect(jsonPath("$[0].pratoNome").value("Refrigerante lata"))
                .andExpect(jsonPath("$[0].quantidadeVendida").value(3))
                .andExpect(jsonPath("$[0].faturamento").value(21.0))
                .andExpect(jsonPath("$[1].pratoId").value(1));

        mockMvc.perform(get("/api/admin/dashboard/top-pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .queryParam("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    private long criarPedido(
            String token,
            long pratoId,
            int quantidade) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens":[{"pratoId":%d,"quantidade":%d}],
                                  "enderecoEntrega":"Rua do Dashboard, 27",
                                  "formaPagamento":"PIX"
                                }
                                """.formatted(pratoId, quantidade)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asLong();
    }

    private void cancelarPedido(String token, long pedidoId) throws Exception {
        mockMvc.perform(patch("/api/admin/pedidos/{id}/cancelar", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Cancelado para validar o dashboard"}
                                """))
                .andExpect(status().isOk());
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
                                  "nome":"Cliente Dashboard",
                                  "email":"%s",
                                  "senha":"senha456",
                                  "telefone":"(11) 97777-2727",
                                  "endereco":"Rua do Cliente, 27"
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
