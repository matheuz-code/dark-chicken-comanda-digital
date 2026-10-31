package br.edu.unasp.comandadigital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
class AdminFullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void executaFluxoAdministrativoCompletoSemPersistirDadosDoEnsaio() throws Exception {
        String admin = login("admin@email.com", "senha123");

        mockMvc.perform(post("/api/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Gerente Fluxo Completo",
                                  "email":"gerente.fluxo@teste.com",
                                  "senha":"senha456",
                                  "perfil":"GERENTE",
                                  "telefone":"(11) 99999-0000",
                                  "endereco":"Rua Gerencial, 30",
                                  "status":"ATIVO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("GERENTE"));
        String gerente = login("gerente.fluxo@teste.com", "senha456");

        long categoriaId = id(mockMvc.perform(post("/api/admin/categorias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Categoria Fluxo Completo",
                                  "descricao":"Categoria criada pelo teste ponta a ponta",
                                  "ordem":90,
                                  "status":"ATIVO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        long ingredienteId = id(mockMvc.perform(post("/api/admin/ingredientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Ingrediente Fluxo Completo",
                                  "sku":"FLUXO-COMPLETO",
                                  "unidadePadrao":"G",
                                  "estoqueMinimo":100.0000,
                                  "custoUnitario":0.0100,
                                  "status":"ATIVO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        long pratoId = id(mockMvc.perform(post("/api/admin/pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pratoJson(categoriaId, "INATIVO")))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(post("/api/admin/pratos/{id}/ficha", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rendimento":1,
                                  "modoPreparo":"Porcionar o ingrediente e finalizar o prato.",
                                  "itens":[{
                                    "ingredienteId":%d,
                                    "quantidade":10.0000,
                                    "unidade":"G",
                                    "fatorCorrecao":1.0000
                                  }]
                                }
                                """.formatted(ingredienteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.custoUnitario").value(0.1));

        mockMvc.perform(put("/api/admin/pratos/{id}", pratoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pratoJson(categoriaId, "ATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
        mockMvc.perform(get("/api/cardapio/{id}", pratoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Prato Fluxo Completo"));

        long fornecedorId = id(mockMvc.perform(post("/api/admin/fornecedores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razaoSocial":"Fornecedor Fluxo Completo",
                                  "cnpj":"11.222.333/0001-81",
                                  "telefone":"(11) 99999-0000",
                                  "email":"fluxo@fornecedor.com",
                                  "categoriasProdutos":"Ingrediente de teste",
                                  "status":"ATIVO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(post("/api/admin/fornecedores/{id}/produtos", fornecedorId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredienteId":%d,"preco":10.0000,"unidadeVenda":"KG"}
                                """.formatted(ingredienteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precoNaUnidadePadrao").value(0.01));

        long compraId = id(mockMvc.perform(post("/api/admin/compras")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fornecedorId":%d,
                                  "itens":[{
                                    "ingredienteId":%d,
                                    "quantidade":1.0000,
                                    "precoUnitario":10.0000
                                  }]
                                }
                                """.formatted(fornecedorId, ingredienteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn());
        mockMvc.perform(post("/api/admin/compras/{id}/enviar", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"));
        mockMvc.perform(post("/api/admin/compras/{id}/receber", compraId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEBIDO"));
        assertSaldo(ingredienteId, "1000.0000");

        String cliente = registrarCliente("cliente.fluxo.completo@teste.com");
        long pedidoId = id(mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens":[{"pratoId":%d,"quantidade":1}],
                                  "enderecoEntrega":"Rua do Fluxo Completo, 100",
                                  "formaPagamento":"PIX"
                                }
                                """.formatted(pratoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(20.0))
                .andReturn());

        mockMvc.perform(get("/api/admin/pedidos/{id}", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.email").value("cliente.fluxo.completo@teste.com"))
                .andExpect(jsonPath("$.itens[0].nomePrato").value("Prato Fluxo Completo"));
        mockMvc.perform(get("/api/admin/dashboard/top-pratos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        alterarStatus(admin, pedidoId, "CONFIRMADO");
        alterarStatus(admin, pedidoId, "EM_PREPARO");
        assertSaldo(ingredienteId, "990.0000");
        alterarStatus(admin, pedidoId, "PRONTO");
        alterarStatus(admin, pedidoId, "SAIU_ENTREGA");
        alterarStatus(admin, pedidoId, "FINALIZADO");

        mockMvc.perform(get("/api/pedidos/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"))
                .andExpect(jsonPath("$.timeline.length()").value(6));

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM estoque_movimentacao WHERE pedido_id = ? AND motivo = 'VENDA'",
                Long.class,
                pedidoId)).isEqualTo(1L);
    }

    private void alterarStatus(String token, long pedidoId, String statusPedido) throws Exception {
        mockMvc.perform(patch("/api/admin/pedidos/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s","observacao":"Fluxo administrativo completo"}
                                """.formatted(statusPedido)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(statusPedido));
    }

    private void assertSaldo(long ingredienteId, String esperado) {
        BigDecimal saldo = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE
                    WHEN tipo IN ('ENTRADA', 'ESTORNO') THEN quantidade
                    ELSE -quantidade
                END), 0)
                FROM estoque_movimentacao
                WHERE ingrediente_id = ?
                """, BigDecimal.class, ingredienteId);
        org.assertj.core.api.Assertions.assertThat(saldo).isEqualByComparingTo(esperado);
    }

    private String pratoJson(long categoriaId, String status) {
        return """
                {
                  "categoriaId":%d,
                  "nome":"Prato Fluxo Completo",
                  "descricao":"Prato criado no teste administrativo ponta a ponta",
                  "fotoUrl":"/images/fluxo-completo.jpg",
                  "precoVenda":20.00,
                  "tempoPreparoMin":10,
                  "status":"%s"
                }
                """.formatted(categoriaId, status);
    }

    private String registrarCliente(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Cliente Fluxo Completo",
                                  "email":"%s",
                                  "senha":"senha456",
                                  "telefone":"(11) 98888-7777",
                                  "endereco":"Rua Cliente, 100"
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

    private long id(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asLong();
    }

    private String token(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
