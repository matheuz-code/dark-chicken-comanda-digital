-- Administrador inicial. Senha em texto puro para acesso: senha123
INSERT INTO usuario (id, nome, email, senha_hash, perfil, telefone, endereco, status)
VALUES (
    1,
    'Administrador',
    'admin@email.com',
    '$2a$12$CMrugK2ki0r7ckAFXnkGbeF8Lt2/Pxp6aeEUityQF9JbtRrqPLMA2',
    'ADMIN',
    NULL,
    NULL,
    'ATIVO'
);

INSERT INTO categoria (id, nome, descricao, ordem, status)
VALUES
    (1, 'Lanches', 'Hambúrgueres e sanduíches preparados na hora', 1, 'ATIVO'),
    (2, 'Porções', 'Acompanhamentos e porções para compartilhar', 2, 'ATIVO'),
    (3, 'Açaí', 'Açaí e complementos', 3, 'ATIVO'),
    (4, 'Bebidas', 'Bebidas geladas', 4, 'ATIVO');

INSERT INTO ingrediente (
    id, nome, sku, unidade_padrao, estoque_minimo, custo_unitario, status
)
VALUES
    (1, 'Pão brioche', 'PAO-BRIOCHE', 'UN', 20.0000, 1.5000, 'ATIVO'),
    (2, 'Carne bovina', 'CARNE-BOVINA', 'G', 5000.0000, 0.0350, 'ATIVO'),
    (3, 'Queijo prato', 'QUEIJO-PRATO', 'G', 2000.0000, 0.0400, 'ATIVO'),
    (4, 'Alface', 'ALFACE', 'G', 500.0000, 0.0180, 'ATIVO'),
    (5, 'Tomate', 'TOMATE', 'G', 1000.0000, 0.0120, 'ATIVO'),
    (6, 'Molho especial', 'MOLHO-ESPECIAL', 'G', 500.0000, 0.0250, 'ATIVO'),
    (7, 'Batata', 'BATATA', 'G', 5000.0000, 0.0080, 'ATIVO'),
    (8, 'Óleo vegetal', 'OLEO-VEGETAL', 'ML', 2000.0000, 0.0090, 'ATIVO'),
    (9, 'Sal', 'SAL', 'G', 500.0000, 0.0030, 'ATIVO'),
    (10, 'Polpa de açaí', 'POLPA-ACAI', 'G', 3000.0000, 0.0250, 'ATIVO'),
    (11, 'Banana', 'BANANA', 'G', 1000.0000, 0.0070, 'ATIVO'),
    (12, 'Granola', 'GRANOLA', 'G', 1000.0000, 0.0300, 'ATIVO'),
    (13, 'Refrigerante lata', 'REFRIGERANTE-LATA', 'UN', 24.0000, 4.0000, 'ATIVO');

INSERT INTO prato (
    id, categoria_id, nome, descricao, foto_url, preco_venda, tempo_preparo_min, status
)
VALUES
    (1, 1, 'X-Burger', 'Pão brioche, hambúrguer bovino, queijo e molho especial', NULL, 24.90, 20, 'ATIVO'),
    (2, 1, 'X-Salada', 'Pão brioche, hambúrguer bovino, queijo, alface, tomate e molho especial', NULL, 27.90, 22, 'ATIVO'),
    (3, 2, 'Batata frita', 'Porção individual de batatas fritas crocantes', NULL, 18.90, 15, 'ATIVO'),
    (4, 3, 'Açaí completo', 'Açaí com banana e granola', NULL, 22.90, 10, 'ATIVO'),
    (5, 4, 'Refrigerante lata', 'Refrigerante gelado em lata', NULL, 7.00, 1, 'ATIVO');

INSERT INTO ficha_tecnica (id, prato_id, rendimento, modo_preparo)
VALUES
    (1, 1, 1, 'Grelhar o hambúrguer, derreter o queijo e montar no pão com o molho especial.'),
    (2, 2, 1, 'Grelhar o hambúrguer, derreter o queijo e montar no pão com vegetais higienizados e molho.'),
    (3, 3, 1, 'Cortar e porcionar as batatas, fritar no óleo quente, escorrer e finalizar com sal.'),
    (4, 4, 1, 'Porcionar o açaí e finalizar com banana fatiada e granola.'),
    (5, 5, 1, 'Separar uma lata refrigerada e conferir a validade antes da expedição.');

INSERT INTO ficha_tecnica_item (
    id, ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao
)
VALUES
    (1, 1, 1, 1.0000, 'UN', 1.0000),
    (2, 1, 2, 180.0000, 'G', 1.0500),
    (3, 1, 3, 40.0000, 'G', 1.0000),
    (4, 1, 6, 20.0000, 'G', 1.0000),
    (5, 2, 1, 1.0000, 'UN', 1.0000),
    (6, 2, 2, 180.0000, 'G', 1.0500),
    (7, 2, 3, 40.0000, 'G', 1.0000),
    (8, 2, 4, 30.0000, 'G', 1.1000),
    (9, 2, 5, 50.0000, 'G', 1.0500),
    (10, 2, 6, 20.0000, 'G', 1.0000),
    (11, 3, 7, 300.0000, 'G', 1.1500),
    (12, 3, 8, 40.0000, 'ML', 1.0000),
    (13, 3, 9, 5.0000, 'G', 1.0000),
    (14, 4, 10, 300.0000, 'G', 1.0000),
    (15, 4, 11, 80.0000, 'G', 1.1000),
    (16, 4, 12, 30.0000, 'G', 1.0000),
    (17, 5, 13, 1.0000, 'UN', 1.0000);

-- Estoque inicial para permitir os testes funcionais dos pedidos nas próximas fases.
INSERT INTO estoque_movimentacao (
    id, ingrediente_id, tipo, quantidade, motivo, lote, validade,
    custo_unitario, pedido_compra_id, pedido_id, usuario_id
)
VALUES
    (1, 1, 'ENTRADA', 100.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 1.5000, NULL, NULL, 1),
    (2, 2, 'ENTRADA', 20000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0350, NULL, NULL, 1),
    (3, 3, 'ENTRADA', 5000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0400, NULL, NULL, 1),
    (4, 4, 'ENTRADA', 2000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0180, NULL, NULL, 1),
    (5, 5, 'ENTRADA', 5000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0120, NULL, NULL, 1),
    (6, 6, 'ENTRADA', 3000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0250, NULL, NULL, 1),
    (7, 7, 'ENTRADA', 20000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0080, NULL, NULL, 1),
    (8, 8, 'ENTRADA', 10000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0090, NULL, NULL, 1),
    (9, 9, 'ENTRADA', 3000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0030, NULL, NULL, 1),
    (10, 10, 'ENTRADA', 15000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0250, NULL, NULL, 1),
    (11, 11, 'ENTRADA', 5000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0070, NULL, NULL, 1),
    (12, 12, 'ENTRADA', 5000.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 0.0300, NULL, NULL, 1),
    (13, 13, 'ENTRADA', 120.0000, 'AJUSTE', 'SEED-INICIAL', NULL, 4.0000, NULL, NULL, 1);
