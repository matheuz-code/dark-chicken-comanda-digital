package br.edu.unasp.comandadigital.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CardapioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void publiclyListsOnlySafeCardDataWithPagination() throws Exception {
        mockMvc.perform(get("/api/cardapio").queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.totalElementos").value(5))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.conteudo[0].fotoUrl").exists())
                .andExpect(jsonPath("$.conteudo[0].categoria.id").exists())
                .andExpect(jsonPath("$.conteudo[0].status").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].custoUnitario").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].fichaTecnica").doesNotExist());
    }

    @Test
    void filtersThePublicMenuByCategory() throws Exception {
        mockMvc.perform(get("/api/cardapio").queryParam("categoriaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[*].categoria.id", everyItem(is(1))));
    }

    @Test
    void returnsThePublicDishDetailsWithoutInternalCostData() throws Exception {
        mockMvc.perform(get("/api/cardapio/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("X-Burger"))
                .andExpect(jsonPath("$.tempoPreparoMin").value(20))
                .andExpect(jsonPath("$.fotoUrl").exists())
                .andExpect(jsonPath("$.categoria.nome").value("Lanches"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.custoUnitario").doesNotExist())
                .andExpect(jsonPath("$.fichaTecnica").doesNotExist());
    }

    @Test
    void neverPublishesInactiveDishesOrDishesFromInactiveCategories() throws Exception {
        jdbcTemplate.update("UPDATE prato SET status = 'INATIVO' WHERE id = 5");
        jdbcTemplate.update("UPDATE categoria SET status = 'INATIVO' WHERE id = 1");

        mockMvc.perform(get("/api/cardapio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));

        mockMvc.perform(get("/api/cardapio/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/cardapio/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void validatesIdentifiersAndReturnsAStandardizedNotFoundError() throws Exception {
        mockMvc.perform(get("/api/cardapio/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/cardapio/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Prato ativo não encontrado"));
    }
}
