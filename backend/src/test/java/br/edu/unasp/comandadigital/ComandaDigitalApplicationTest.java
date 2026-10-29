package br.edu.unasp.comandadigital;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ComandaDigitalApplicationTest {

    private static final List<String> DOMAIN_TABLES = List.of(
            "categoria",
            "estoque_movimentacao",
            "ficha_tecnica",
            "ficha_tecnica_item",
            "fornecedor",
            "fornecedor_produto",
            "fornecedor_produto_preco_historico",
            "ingrediente",
            "pedido",
            "pedido_compra",
            "pedido_compra_item",
            "pedido_item",
            "pedido_status_historico",
            "prato",
            "usuario");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsContextAndValidatesAllDomainTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                """,
                String.class);

        assertThat(tables).containsAll(DOMAIN_TABLES);
    }

    @Test
    void loadsTheRequiredSeedData() {
        Integer categories = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categoria", Integer.class);
        Integer ingredients = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ingrediente", Integer.class);
        Integer dishes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prato", Integer.class);
        Integer dishesWithTechnicalSheet = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prato p INNER JOIN ficha_tecnica ft ON ft.prato_id = p.id",
                Integer.class);
        Integer dishesWithPhoto = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prato WHERE foto_url IS NOT NULL AND foto_url <> ''",
                Integer.class);
        Integer initialStockEntries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM estoque_movimentacao WHERE lote = 'SEED-INICIAL'",
                Integer.class);

        String adminPasswordHash = jdbcTemplate.queryForObject(
                "SELECT senha_hash FROM usuario WHERE email = 'admin@email.com'",
                String.class);

        assertThat(categories).isEqualTo(4);
        assertThat(ingredients).isEqualTo(13);
        assertThat(dishes).isEqualTo(5);
        assertThat(dishesWithTechnicalSheet).isEqualTo(5);
        assertThat(dishesWithPhoto).isEqualTo(5);
        assertThat(initialStockEntries).isEqualTo(13);
        assertThat(new BCryptPasswordEncoder().matches("senha123", adminPasswordHash)).isTrue();
    }

    @Test
    void alignsTheSchemaWithTheFunctionalRequirementsFromTheSrs() {
        Integer alignmentColumns = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND (
                    (table_name = 'pedido' AND column_name IN ('canal', 'status_pagamento'))
                    OR (table_name = 'fornecedor' AND column_name = 'categorias_produtos')
                  )
                """,
                Integer.class);

        String stockReasonConstraint = jdbcTemplate.queryForObject(
                """
                SELECT check_clause
                FROM information_schema.check_constraints
                WHERE constraint_schema = DATABASE()
                  AND constraint_name = 'chk_movimentacao_motivo'
                """,
                String.class);

        assertThat(alignmentColumns).isEqualTo(3);
        assertThat(stockReasonConstraint)
                .contains("QUEBRA")
                .contains("USO_INTERNO");
    }
}
