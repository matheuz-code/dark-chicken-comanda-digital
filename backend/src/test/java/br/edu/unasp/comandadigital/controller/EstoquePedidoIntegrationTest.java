package br.edu.unasp.comandadigital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
class EstoquePedidoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void baixaAoConfirmarESemDuplicarAoEntrarEmPreparo() throws Exception {
        String cliente = registrarCliente("cliente.baixa.estoque@teste.com");
        String admin = login("admin@email.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Baixa",
                "cozinheiro.baixa@teste.com",
                "COZINHEIRO");
        long pedidoId = criarPedido(
                cliente,
                """
                [
                  {"pratoId":1,"quantidade":1},
                  {"pratoId":2,"quantidade":1}
                ]
                """);

        alterarStatus(admin, pedidoId, "CONFIRMADO")
                .andExpect(status().isOk());
        assertThat(quantidadesMovimentadas(pedidoId, "SAIDA", "VENDA")).hasSize(6);
        alterarStatus(cozinheiro, pedidoId, "EM_PREPARO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PREPARO"));

        Map<Long, BigDecimal> saidas = quantidadesMovimentadas(pedidoId, "SAIDA", "VENDA");
        assertThat(saidas).hasSize(6);
        assertThat(saidas.get(1L)).isEqualByComparingTo("2.0000");
        assertThat(saidas.get(2L)).isEqualByComparingTo("378.0000");
        assertThat(saidas.get(3L)).isEqualByComparingTo("80.0000");
        assertThat(saidas.get(4L)).isEqualByComparingTo("33.0000");
        assertThat(saidas.get(5L)).isEqualByComparingTo("52.5000");
        assertThat(saidas.get(6L)).isEqualByComparingTo("40.0000");
        assertThat(contar("""
                SELECT COUNT(*)
                FROM estoque_movimentacao movimentacao
                JOIN usuario ON usuario.id = movimentacao.usuario_id
                WHERE movimentacao.pedido_id = %d
                  AND movimentacao.tipo = 'SAIDA'
                  AND movimentacao.motivo = 'VENDA'
                  AND usuario.perfil = 'ADMIN'
                """.formatted(pedidoId))).isEqualTo(6);

        alterarStatus(admin, pedidoId, "EM_PREPARO")
                .andExpect(status().isUnprocessableEntity());
        assertThat(quantidadesMovimentadas(pedidoId, "SAIDA", "VENDA"))
                .containsExactlyEntriesOf(saidas);
    }

    @Test
    void mantemPedidoEEstoqueInalteradosQuandoFaltaIngrediente() throws Exception {
        String cliente = registrarCliente("cliente.estoque.insuficiente.preparo@teste.com");
        String admin = login("admin@email.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Sem Estoque",
                "cozinheiro.sem.estoque@teste.com",
                "COZINHEIRO");
        long pedidoId = criarPedido(
                cliente,
                "[{\"pratoId\":1,\"quantidade\":1}]");
        jdbcTemplate.update(
                "UPDATE estoque_movimentacao SET quantidade = 188.0000 WHERE id = 2");
        entityManager.clear();

        alterarStatus(admin, pedidoId, "CONFIRMADO")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("Carne bovina")))
                .andExpect(jsonPath("$.mensagem", containsString("necessário 189")))
                .andExpect(jsonPath("$.mensagem", containsString("disponível 188")));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM pedido WHERE id = ?",
                String.class,
                pedidoId)).isEqualTo("RECEBIDO");
        assertThat(contar("SELECT COUNT(*) FROM pedido_status_historico WHERE pedido_id = "
                + pedidoId)).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM estoque_movimentacao WHERE pedido_id = "
                + pedidoId)).isZero();
    }

    @Test
    void estornaExatamenteAsSaidasAoCancelarDepoisDoInicioDoPreparo() throws Exception {
        String cliente = registrarCliente("cliente.estorno.estoque@teste.com");
        String admin = login("admin@email.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Estorno",
                "cozinheiro.estorno@teste.com",
                "COZINHEIRO");
        String gerente = criarUsuarioInternoELogar(
                "Gerente Estorno",
                "gerente.estorno@teste.com",
                "GERENTE");
        List<Long> ingredientes = List.of(1L, 2L, 3L, 6L);
        Map<Long, BigDecimal> saldosAntes = saldos(ingredientes);
        long pedidoId = criarPedido(
                cliente,
                "[{\"pratoId\":1,\"quantidade\":2}]");

        alterarStatus(admin, pedidoId, "CONFIRMADO").andExpect(status().isOk());
        alterarStatus(cozinheiro, pedidoId, "EM_PREPARO").andExpect(status().isOk());

        Map<Long, BigDecimal> saidas = quantidadesMovimentadas(pedidoId, "SAIDA", "VENDA");
        Map<Long, BigDecimal> saldosDepoisBaixa = saldos(ingredientes);
        for (Long ingredienteId : ingredientes) {
            assertThat(saldosAntes.get(ingredienteId)
                    .subtract(saldosDepoisBaixa.get(ingredienteId)))
                    .isEqualByComparingTo(saidas.get(ingredienteId));
        }

        cancelar(cozinheiro, pedidoId, "Tentativa sem permissão")
                .andExpect(status().isForbidden());
        assertThat(quantidadesMovimentadas(pedidoId, "ESTORNO", "ESTORNO"))
                .isEmpty();

        cancelar(gerente, pedidoId, "Cancelamento operacional")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.statusPagamento").value("ESTORNADO"));

        Map<Long, BigDecimal> estornos = quantidadesMovimentadas(
                pedidoId,
                "ESTORNO",
                "ESTORNO");
        assertThat(estornos).containsExactlyEntriesOf(saidas);
        assertThat(saldos(ingredientes)).containsExactlyEntriesOf(saldosAntes);

        cancelar(gerente, pedidoId, "Segunda tentativa")
                .andExpect(status().isUnprocessableEntity());
        assertThat(quantidadesMovimentadas(pedidoId, "ESTORNO", "ESTORNO"))
                .containsExactlyEntriesOf(estornos);
    }

    @Test
    void cancelamentoAntesDoPreparoNaoCriaMovimentacaoDeEstoque() throws Exception {
        String cliente = registrarCliente("cliente.cancelamento.sem.baixa@teste.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Cancelamento Antecipado",
                "cozinheiro.cancelamento.antecipado@teste.com",
                "COZINHEIRO");
        long pedidoId = criarPedido(
                cliente,
                "[{\"pratoId\":5,\"quantidade\":1}]");

        cancelar(cozinheiro, pedidoId, "Solicitação do cliente")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assertThat(contar("SELECT COUNT(*) FROM estoque_movimentacao WHERE pedido_id = "
                + pedidoId)).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions alterarStatus(
            String token,
            long pedidoId,
            String novoStatus) throws Exception {
        return mockMvc.perform(patch("/api/admin/pedidos/{id}/status", pedidoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s","observacao":"Atualização do teste de estoque"}
                        """.formatted(novoStatus)));
    }

    private org.springframework.test.web.servlet.ResultActions cancelar(
            String token,
            long pedidoId,
            String motivo) throws Exception {
        return mockMvc.perform(patch("/api/admin/pedidos/{id}/cancelar", pedidoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"motivo":"%s"}
                        """.formatted(motivo)));
    }

    private long criarPedido(String token, String itensJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": %s,
                                  "enderecoEntrega": "Rua do Teste de Estoque, 24",
                                  "formaPagamento": "PIX"
                                }
                                """.formatted(itensJson)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id")
                .asLong();
    }

    private Map<Long, BigDecimal> quantidadesMovimentadas(
            long pedidoId,
            String tipo,
            String motivo) {
        return jdbcTemplate.query("""
                SELECT ingrediente_id, quantidade
                FROM estoque_movimentacao
                WHERE pedido_id = ? AND tipo = ? AND motivo = ?
                ORDER BY ingrediente_id
                """, resultSet -> {
            Map<Long, BigDecimal> quantidades = new LinkedHashMap<>();
            while (resultSet.next()) {
                quantidades.put(
                        resultSet.getLong("ingrediente_id"),
                        resultSet.getBigDecimal("quantidade"));
            }
            return quantidades;
        }, pedidoId, tipo, motivo);
    }

    private Map<Long, BigDecimal> saldos(List<Long> ingredienteIds) {
        Map<Long, BigDecimal> resultado = new LinkedHashMap<>();
        for (Long ingredienteId : ingredienteIds) {
            resultado.put(ingredienteId, jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(CASE
                        WHEN tipo IN ('ENTRADA', 'ESTORNO') THEN quantidade
                        ELSE -quantidade
                    END), 0)
                    FROM estoque_movimentacao
                    WHERE ingrediente_id = ?
                    """, BigDecimal.class, ingredienteId));
        }
        return resultado;
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
                                  "nome": "Cliente Estoque Pedido",
                                  "email": "%s",
                                  "senha": "senha456",
                                  "telefone": "(11) 97777-6666",
                                  "endereco": "Rua do Cliente, 24"
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

    private long contar(String sql) {
        Long resultado = jdbcTemplate.queryForObject(sql, Long.class);
        return resultado == null ? 0 : resultado;
    }
}
