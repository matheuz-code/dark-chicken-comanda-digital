-- RF-006: o checkout possui pagamento simulado e deve registrar o pedido como pago.
-- RF-019: a listagem administrativa de pedidos deve permitir filtro por canal.
ALTER TABLE pedido
    ADD COLUMN canal VARCHAR(20) NOT NULL DEFAULT 'SITE' AFTER cliente_id,
    ADD COLUMN status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' AFTER status,
    ADD CONSTRAINT chk_pedido_canal
        CHECK (canal IN ('SITE', 'IFOOD', 'WHATSAPP', 'TELEFONE', 'OUTRO')),
    ADD CONSTRAINT chk_pedido_status_pagamento
        CHECK (status_pagamento IN ('PENDENTE', 'PAGO', 'CANCELADO', 'ESTORNADO'));

CREATE INDEX idx_pedido_canal_created ON pedido (canal, created_at);
CREATE INDEX idx_pedido_status_pagamento ON pedido (status_pagamento);

-- RF-021: categorias de produtos atendidas pelo fornecedor.
ALTER TABLE fornecedor
    ADD COLUMN categorias_produtos VARCHAR(500) NULL AFTER email;

-- RF-030: a regra funcional de perdas inclui também QUEBRA e USO_INTERNO.
ALTER TABLE estoque_movimentacao
    DROP CHECK chk_movimentacao_motivo;

ALTER TABLE estoque_movimentacao
    ADD CONSTRAINT chk_movimentacao_motivo CHECK (motivo IN (
        'COMPRA', 'VENDA', 'DESPERDICIO', 'VENCIMENTO', 'QUEBRA',
        'USO_INTERNO', 'AJUSTE', 'ESTORNO'
    ));

-- RF-007/RF-020: preserva a timeline e as datas das transições do pedido.
CREATE TABLE pedido_status_historico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    usuario_id BIGINT NULL,
    observacao VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_pedido_status_historico PRIMARY KEY (id),
    CONSTRAINT fk_pedido_status_historico_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_status_historico_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pedido_status_historico_status CHECK (status IN (
        'RECEBIDO', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO',
        'SAIU_ENTREGA', 'FINALIZADO', 'CANCELADO'
    ))
) ENGINE = InnoDB;

CREATE INDEX idx_pedido_status_historico_timeline
    ON pedido_status_historico (pedido_id, created_at);

-- RF-026: registra cada preço praticado no catálogo do fornecedor.
CREATE TABLE fornecedor_produto_preco_historico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fornecedor_produto_id BIGINT NOT NULL,
    preco DECIMAL(14, 4) NOT NULL,
    usuario_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_fornecedor_produto_preco_historico PRIMARY KEY (id),
    CONSTRAINT fk_preco_historico_fornecedor_produto
        FOREIGN KEY (fornecedor_produto_id) REFERENCES fornecedor_produto (id) ON DELETE RESTRICT,
    CONSTRAINT fk_preco_historico_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT chk_preco_historico_preco CHECK (preco >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_preco_historico_produto_data
    ON fornecedor_produto_preco_historico (fornecedor_produto_id, created_at);
