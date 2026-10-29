package br.edu.unasp.comandadigital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
class AdminEstoqueControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void protegeTodoOModuloComAMatrizDePerfisDoSrs() throws Exception {
        mockMvc.perform(get("/api/admin/estoque/saldo"))
                .andExpect(status().isUnauthorized());

        String cliente = registrarCliente("cliente.rbac.estoque@teste.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro RBAC Estoque",
                "cozinheiro.rbac.estoque@teste.com",
                "COZINHEIRO");
        String gerente = criarUsuarioInternoELogar(
                "Gerente RBAC Estoque",
                "gerente.rbac.estoque@teste.com",
                "GERENTE");

        mockMvc.perform(get("/api/admin/estoque/saldo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/estoque/alertas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/estoque/movimentacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/estoque/saldo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@email.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void retornaSaldoEmTempoRealEAlertaSomenteQuandoAbaixoDoMinimo() throws Exception {
        String gerente = criarUsuarioInternoELogar(
                "Gerente Alertas",
                "gerente.alertas.estoque@teste.com",
                "GERENTE");
        BigDecimal saldoAntes = saldo(1L);
        BigDecimal quantidadeSaida = saldoAntes.subtract(new BigDecimal("19.0000"));

        mockMvc.perform(get("/api/admin/estoque/saldo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .queryParam("status", "ATIVO")
                        .queryParam("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[?(@.ingredienteId == 1)].nome")
                        .value(hasItem("Pão brioche")))
                .andExpect(jsonPath("$.conteudo[?(@.ingredienteId == 1)].abaixoDoMinimo")
                        .value(hasItem(false)));

        registrarSaidaManual(
                gerente,
                1L,
                quantidadeSaida,
                "AJUSTE",
                "AJUSTE-ALERTA")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/estoque/alertas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .queryParam("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[?(@.ingredienteId == 1)].saldoAtual")
                        .value(hasItem(19.0)))
                .andExpect(jsonPath("$.conteudo[?(@.ingredienteId == 1)].estoqueMinimo")
                        .value(hasItem(20.0)))
                .andExpect(jsonPath("$.conteudo[?(@.ingredienteId == 1)].abaixoDoMinimo")
                        .value(hasItem(true)));
    }

    @Test
    void registraSaidaManualAuditavelEAtualizaOSaldo() throws Exception {
        String gerente = criarUsuarioInternoELogar(
                "Gerente Movimentação",
                "gerente.movimentacao@teste.com",
                "GERENTE");
        BigDecimal saldoAntes = saldo(2L);

        MvcResult result = registrarSaidaManual(
                gerente,
                2L,
                new BigDecimal("12.3456"),
                "QUEBRA",
                "  LOTE-QUEBRA-01  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingrediente.id").value(2))
                .andExpect(jsonPath("$.ingrediente.unidadePadrao").value("G"))
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andExpect(jsonPath("$.quantidade").value(12.3456))
                .andExpect(jsonPath("$.motivo").value("QUEBRA"))
                .andExpect(jsonPath("$.lote").value("LOTE-QUEBRA-01"))
                .andExpect(jsonPath("$.custoUnitario").value(0.0350))
                .andExpect(jsonPath("$.usuario.perfil").value("GERENTE"))
                .andExpect(jsonPath("$.pedidoId").doesNotExist())
                .andExpect(jsonPath("$.pedidoCompraId").doesNotExist())
                .andExpect(jsonPath("$.criadoEm").exists())
                .andReturn();
        long movimentacaoId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asLong();

        assertThat(saldoAntes.subtract(saldo(2L)))
                .isEqualByComparingTo("12.3456");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT usuario_id FROM estoque_movimentacao WHERE id = ?",
                Long.class,
                movimentacaoId)).isNotNull();

        mockMvc.perform(get("/api/admin/estoque/movimentacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente))
                        .queryParam("ingredienteId", "2")
                        .queryParam("tipo", "SAIDA")
                        .queryParam("motivo", "QUEBRA")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value((int) movimentacaoId))
                .andExpect(jsonPath("$.conteudo[0].lote").value("LOTE-QUEBRA-01"));
    }

    @Test
    void rejeitaSaldoNegativoMotivoAutomaticoIngredienteInativoEDtoInvalida() throws Exception {
        String admin = login("admin@email.com");
        BigDecimal saldoAntes = saldo(13L);
        long movimentosAntes = contar("""
                SELECT COUNT(*) FROM estoque_movimentacao
                WHERE ingrediente_id = 13 AND tipo = 'SAIDA'
                """);

        registrarSaidaManual(
                admin,
                13L,
                saldoAntes.add(BigDecimal.ONE),
                "DESPERDICIO",
                null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("Estoque insuficiente")));
        registrarSaidaManual(
                admin,
                13L,
                BigDecimal.ONE,
                "VENDA",
                null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("não é permitido")));

        mockMvc.perform(post("/api/admin/estoque/movimentacao")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredienteId":13,"quantidade":0,"motivo":"QUEBRA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("quantidade"));

        jdbcTemplate.update("UPDATE ingrediente SET status = 'INATIVO' WHERE id = 13");
        entityManager.clear();
        registrarSaidaManual(
                admin,
                13L,
                BigDecimal.ONE,
                "USO_INTERNO",
                null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("ingrediente inativo")));

        assertThat(saldo(13L)).isEqualByComparingTo(saldoAntes);
        assertThat(contar("""
                SELECT COUNT(*) FROM estoque_movimentacao
                WHERE ingrediente_id = 13 AND tipo = 'SAIDA'
                """)).isEqualTo(movimentosAntes);
    }

    @Test
    void exibeNoHistoricoABaixaAutomaticaVinculadaAoPedido() throws Exception {
        String cliente = registrarCliente("cliente.historico.estoque@teste.com");
        String admin = login("admin@email.com");
        long pedidoId = criarPedido(cliente, 5L, 1);

        alterarStatus(admin, pedidoId, "CONFIRMADO")
                .andExpect(status().isOk());
        alterarStatus(admin, pedidoId, "EM_PREPARO")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/estoque/movimentacoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .queryParam("ingredienteId", "13")
                        .queryParam("tipo", "SAIDA")
                        .queryParam("motivo", "VENDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].pedidoId").value((int) pedidoId))
                .andExpect(jsonPath("$.conteudo[0].lote").value("PEDIDO-" + pedidoId))
                .andExpect(jsonPath("$.conteudo[0].quantidade").value(1.0))
                .andExpect(jsonPath("$.conteudo[0].usuario.perfil").value("ADMIN"));
    }

    private org.springframework.test.web.servlet.ResultActions registrarSaidaManual(
            String token,
            long ingredienteId,
            BigDecimal quantidade,
            String motivo,
            String lote) throws Exception {
        String loteJson = lote == null ? "null" : "\"" + lote + "\"";
        return mockMvc.perform(post("/api/admin/estoque/movimentacao")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "ingredienteId": %d,
                          "quantidade": %s,
                          "motivo": "%s",
                          "lote": %s
                        }
                        """.formatted(
                        ingredienteId,
                        quantidade.toPlainString(),
                        motivo,
                        loteJson)));
    }

    private org.springframework.test.web.servlet.ResultActions alterarStatus(
            String token,
            long pedidoId,
            String novoStatus) throws Exception {
        return mockMvc.perform(patch("/api/admin/pedidos/{id}/status", pedidoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s"}
                        """.formatted(novoStatus)));
    }

    private long criarPedido(String token, long pratoId, int quantidade) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"pratoId":%d,"quantidade":%d}],
                                  "enderecoEntrega": "Rua do Histórico de Estoque, 25",
                                  "formaPagamento": "PIX"
                                }
                                """.formatted(pratoId, quantidade)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asLong();
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
                                  "nome": "Cliente Estoque Admin",
                                  "email": "%s",
                                  "senha": "senha456",
                                  "telefone": "(11) 96666-5555",
                                  "endereco": "Rua do Cliente, 25"
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

    private long contar(String sql) {
        Long resultado = jdbcTemplate.queryForObject(sql, Long.class);
        return resultado == null ? 0 : resultado;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
