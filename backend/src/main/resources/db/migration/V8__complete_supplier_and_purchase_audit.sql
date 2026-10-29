-- RN06: catálogo e itens de compra também usam exclusão lógica.
ALTER TABLE fornecedor_produto
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' AFTER unidade_venda,
    ADD CONSTRAINT chk_fornecedor_produto_status
        CHECK (status IN ('ATIVO', 'INATIVO'));

CREATE INDEX idx_fornecedor_produto_fornecedor_status
    ON fornecedor_produto (fornecedor_id, status);

ALTER TABLE pedido_compra_item
    ADD COLUMN unidade_compra VARCHAR(10) NOT NULL DEFAULT 'UN' AFTER preco_unitario,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' AFTER subtotal,
    ADD CONSTRAINT chk_compra_item_unidade
        CHECK (unidade_compra IN ('G', 'ML', 'UN', 'KG', 'L')),
    ADD CONSTRAINT chk_compra_item_status
        CHECK (status IN ('ATIVO', 'INATIVO'));

CREATE INDEX idx_compra_item_compra_status
    ON pedido_compra_item (pedido_compra_id, status);

-- RF-026: o histórico precisa preservar também a unidade à qual o preço se refere.
ALTER TABLE fornecedor_produto_preco_historico
    ADD COLUMN unidade_venda VARCHAR(10) NOT NULL DEFAULT 'UN' AFTER preco,
    ADD CONSTRAINT chk_preco_historico_unidade
        CHECK (unidade_venda IN ('G', 'ML', 'UN', 'KG', 'L'));

-- RF-025/RN05: impede uma segunda entrada do mesmo ingrediente para a mesma compra.
ALTER TABLE estoque_movimentacao
    ADD CONSTRAINT uk_mov_compra_ingrediente_tipo_motivo
        UNIQUE (pedido_compra_id, ingrediente_id, tipo, motivo);
