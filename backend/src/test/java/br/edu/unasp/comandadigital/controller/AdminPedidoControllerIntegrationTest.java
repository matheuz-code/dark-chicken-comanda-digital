package br.edu.unasp.comandadigital.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AdminPedidoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void protectsAdminOrdersWithTheRequiredRoleMatrix() throws Exception {
        mockMvc.perform(get("/api/admin/pedidos"))
                .andExpect(status().isUnauthorized());

        String cliente = registrarCliente("cliente.admin.pedido@teste.com");
        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isForbidden());

        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Pedidos",
                "cozinheiro.pedidos@teste.com",
                "COZINHEIRO");
        String gerente = criarUsuarioInternoELogar(
                "Gerente Pedidos",
                "gerente.pedidos@teste.com",
                "GERENTE");

        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gerente)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@email.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void listsWithPaginationAndFiltersAndReturnsASafeDetailedTimeline() throws Exception {
        long totalAntes = contar("SELECT COUNT(*) FROM pedido");
        long ifoodAntes = contar("SELECT COUNT(*) FROM pedido WHERE canal = 'IFOOD'");
        String clienteA = registrarCliente("cliente.lista.a@teste.com");
        String clienteB = registrarCliente("cliente.lista.b@teste.com");
        long pedidoA1 = criarPedido(clienteA, 1, 2);
        long pedidoA2 = criarPedido(clienteA, 5, 1);
        criarPedido(clienteB, 5, 3);
        jdbcTemplate.update("UPDATE pedido SET canal = 'IFOOD' WHERE id = ?", pedidoA2);
        entityManager.clear();
        String admin = login("admin@email.com");

        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(1))
                .andExpect(jsonPath("$.totalElementos").value(totalAntes + 3))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].cliente.email").exists())
                .andExpect(jsonPath("$.conteudo[0].quantidadeItens").exists());

        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .queryParam("canal", "IFOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(ifoodAntes + 1))
                .andExpect(jsonPath("$.conteudo[?(@.id == " + pedidoA2 + ")].canal")
                        .value(hasItem("IFOOD")));

        mockMvc.perform(get("/api/admin/pedidos/{id}", pedidoA1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.email").value("cliente.lista.a@teste.com"))
                .andExpect(jsonPath("$.cliente.senha").doesNotExist())
                .andExpect(jsonPath("$.cliente.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].nomePrato").value("X-Burger"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].subtotal").value(49.80))
                .andExpect(jsonPath("$.timeline.length()").value(1))
                .andExpect(jsonPath("$.timeline[0].usuario.perfil").value("CLIENTE"));
    }

    @Test
    void completesTheOrderedLifecycleWithAdminCookAndManager() throws Exception {
        String cliente = registrarCliente("cliente.ciclo@teste.com");
        long pedidoId = criarPedido(cliente, 1, 1);
        String admin = login("admin@email.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Ciclo",
                "cozinheiro.ciclo@teste.com",
                "COZINHEIRO");
        String gerente = criarUsuarioInternoELogar(
                "Gerente Ciclo",
                "gerente.ciclo@teste.com",
                "GERENTE");

        alterarStatus(admin, pedidoId, "CONFIRMADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
        alterarStatus(cozinheiro, pedidoId, "EM_PREPARO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PREPARO"));
        alterarStatus(cozinheiro, pedidoId, "PRONTO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTO"));
        alterarStatus(admin, pedidoId, "SAIU_ENTREGA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAIU_ENTREGA"));
        alterarStatus(gerente, pedidoId, "FINALIZADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"))
                .andExpect(jsonPath("$.timeline.length()").value(6))
                .andExpect(jsonPath("$.timeline[5].usuario.perfil").value("GERENTE"));

        org.assertj.core.api.Assertions.assertThat(contar(
                        "SELECT COUNT(*) FROM pedido_status_historico WHERE pedido_id = " + pedidoId))
                .isEqualTo(6);

        mockMvc.perform(get("/api/pedidos/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"))
                .andExpect(jsonPath("$.timeline.length()").value(6));

        mockMvc.perform(get("/api/admin/pedidos/{id}", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidOrUnauthorizedStatusChangesWithoutMutatingTheOrder() throws Exception {
        String cliente = registrarCliente("cliente.status.admin@teste.com");
        long pedidoId = criarPedido(cliente, 5, 1);
        String admin = login("admin@email.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Restrito",
                "cozinheiro.restrito@teste.com",
                "COZINHEIRO");

        alterarStatus(admin, pedidoId, "PRONTO")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("Transição de status inválida")));
        alterarStatus(cozinheiro, pedidoId, "CONFIRMADO")
                .andExpect(status().isForbidden());
        alterarStatus(admin, pedidoId, "CANCELADO")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("endpoint de cancelamento")));

        mockMvc.perform(patch("/api/admin/pedidos/{id}/status", pedidoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("status"));

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM pedido WHERE id = ?",
                        String.class,
                        pedidoId))
                .isEqualTo("RECEBIDO");
        org.assertj.core.api.Assertions.assertThat(contar(
                        "SELECT COUNT(*) FROM pedido_status_historico WHERE pedido_id = " + pedidoId))
                .isEqualTo(1);
    }

    @Test
    void appliesRn04AndRecordsTheCancellationReasonAndPaymentRefund() throws Exception {
        String cliente = registrarCliente("cliente.cancelamento@teste.com");
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Cancelamento",
                "cozinheiro.cancelamento@teste.com",
                "COZINHEIRO");
        String gerente = criarUsuarioInternoELogar(
                "Gerente Cancelamento",
                "gerente.cancelamento@teste.com",
                "GERENTE");
        String admin = login("admin@email.com");

        long pedidoAntesPreparo = criarPedido(cliente, 5, 1);
        cancelar(cozinheiro, pedidoAntesPreparo, "Cliente informou endereço incorreto")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.statusPagamento").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelamento")
                        .value("Cliente informou endereço incorreto"))
                .andExpect(jsonPath("$.timeline.length()").value(2));

        long pedidoEmPreparo = criarPedido(cliente, 1, 1);
        alterarStatus(admin, pedidoEmPreparo, "CONFIRMADO").andExpect(status().isOk());
        alterarStatus(cozinheiro, pedidoEmPreparo, "EM_PREPARO").andExpect(status().isOk());

        cancelar(cozinheiro, pedidoEmPreparo, "Tentativa não autorizada")
                .andExpect(status().isForbidden());
        cancelar(gerente, pedidoEmPreparo, "Problema operacional")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.statusPagamento").value("CANCELADO"))
                .andExpect(jsonPath("$.timeline.length()").value(4))
                .andExpect(jsonPath("$.timeline[3].usuario.perfil").value("GERENTE"));

        cancelar(gerente, pedidoEmPreparo, "Segunda tentativa")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("O pedido já está cancelado"));
    }

    @Test
    void limitsCookListingsToPendingKitchenStatuses() throws Exception {
        long visiveisAntes = contar("""
                SELECT COUNT(*) FROM pedido
                WHERE status IN ('RECEBIDO','CONFIRMADO','EM_PREPARO','PRONTO')
                """);
        String cliente = registrarCliente("cliente.cozinha.lista@teste.com");
        criarPedido(cliente, 5, 1);
        long finalizado = criarPedido(cliente, 5, 1);
        jdbcTemplate.update("UPDATE pedido SET status = 'FINALIZADO' WHERE id = ?", finalizado);
        entityManager.clear();
        String cozinheiro = criarUsuarioInternoELogar(
                "Cozinheiro Lista",
                "cozinheiro.lista@teste.com",
                "COZINHEIRO");

        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(visiveisAntes + 1));

        mockMvc.perform(get("/api/admin/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro))
                        .queryParam("status", "FINALIZADO"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/pedidos/{id}", finalizado)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cozinheiro)))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions alterarStatus(
            String token,
            long pedidoId,
            String statusPedido) throws Exception {
        return mockMvc.perform(patch("/api/admin/pedidos/{id}/status", pedidoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s","observacao":"Atualização do teste"}
                        """.formatted(statusPedido)));
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

    private long criarPedido(String token, long pratoId, int quantidade) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"pratoId":%d,"quantidade":%d}],
                                  "enderecoEntrega": "Rua do Pedido Administrativo, 10",
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
                                  "nome": "Cliente Pedido Admin",
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

    private long contar(String sql) {
        Long resultado = jdbcTemplate.queryForObject(sql, Long.class);
        return resultado == null ? 0 : resultado;
    }
}
