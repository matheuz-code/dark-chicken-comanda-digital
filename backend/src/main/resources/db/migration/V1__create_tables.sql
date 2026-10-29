CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    senha_hash VARCHAR(60) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    telefone VARCHAR(30) NULL,
    endereco VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('ADMIN', 'GERENTE', 'COZINHEIRO', 'CLIENTE')),
    CONSTRAINT chk_usuario_status CHECK (status IN ('ATIVO', 'INATIVO'))
) ENGINE = InnoDB;

CREATE TABLE categoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500) NULL,
    ordem INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_categoria PRIMARY KEY (id),
    CONSTRAINT chk_categoria_ordem CHECK (ordem >= 0),
    CONSTRAINT chk_categoria_status CHECK (status IN ('ATIVO', 'INATIVO'))
) ENGINE = InnoDB;

CREATE TABLE prato (
    id BIGINT NOT NULL AUTO_INCREMENT,
    categoria_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(1000) NULL,
    foto_url VARCHAR(1000) NULL,
    preco_venda DECIMAL(12, 2) NOT NULL,
    tempo_preparo_min INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INATIVO',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_prato PRIMARY KEY (id),
    CONSTRAINT fk_prato_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id) ON DELETE RESTRICT,
    CONSTRAINT chk_prato_preco CHECK (preco_venda >= 0),
    CONSTRAINT chk_prato_tempo CHECK (tempo_preparo_min > 0),
    CONSTRAINT chk_prato_status CHECK (status IN ('ATIVO', 'INATIVO', 'PAUSADO'))
) ENGINE = InnoDB;

CREATE INDEX idx_prato_categoria_status ON prato (categoria_id, status);

CREATE TABLE ingrediente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    unidade_padrao VARCHAR(10) NOT NULL,
    estoque_minimo DECIMAL(14, 4) NOT NULL DEFAULT 0,
    custo_unitario DECIMAL(14, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ingrediente PRIMARY KEY (id),
    CONSTRAINT uk_ingrediente_sku UNIQUE (sku),
    CONSTRAINT chk_ingrediente_unidade CHECK (unidade_padrao IN ('G', 'ML', 'UN', 'KG', 'L')),
    CONSTRAINT chk_ingrediente_estoque_minimo CHECK (estoque_minimo >= 0),
    CONSTRAINT chk_ingrediente_custo CHECK (custo_unitario >= 0),
    CONSTRAINT chk_ingrediente_status CHECK (status IN ('ATIVO', 'INATIVO'))
) ENGINE = InnoDB;

CREATE TABLE ficha_tecnica (
    id BIGINT NOT NULL AUTO_INCREMENT,
    prato_id BIGINT NOT NULL,
    rendimento INT NOT NULL,
    modo_preparo TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ficha_tecnica PRIMARY KEY (id),
    CONSTRAINT uk_ficha_tecnica_prato UNIQUE (prato_id),
    CONSTRAINT fk_ficha_tecnica_prato FOREIGN KEY (prato_id) REFERENCES prato (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ficha_tecnica_rendimento CHECK (rendimento > 0)
) ENGINE = InnoDB;

CREATE TABLE ficha_tecnica_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ficha_tecnica_id BIGINT NOT NULL,
    ingrediente_id BIGINT NOT NULL,
    quantidade DECIMAL(14, 4) NOT NULL,
    unidade VARCHAR(10) NOT NULL,
    fator_correcao DECIMAL(10, 4) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ficha_tecnica_item PRIMARY KEY (id),
    CONSTRAINT uk_ficha_item_ingrediente UNIQUE (ficha_tecnica_id, ingrediente_id),
    CONSTRAINT fk_ficha_item_ficha FOREIGN KEY (ficha_tecnica_id) REFERENCES ficha_tecnica (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ficha_item_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ficha_item_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_ficha_item_unidade CHECK (unidade IN ('G', 'ML', 'UN', 'KG', 'L')),
    CONSTRAINT chk_ficha_item_fator CHECK (fator_correcao >= 1)
) ENGINE = InnoDB;

CREATE INDEX idx_ficha_item_ingrediente ON ficha_tecnica_item (ingrediente_id);

CREATE TABLE fornecedor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    razao_social VARCHAR(180) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    telefone VARCHAR(30) NULL,
    email VARCHAR(180) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_fornecedor PRIMARY KEY (id),
    CONSTRAINT uk_fornecedor_cnpj UNIQUE (cnpj),
    CONSTRAINT chk_fornecedor_status CHECK (status IN ('ATIVO', 'INATIVO'))
) ENGINE = InnoDB;

CREATE TABLE fornecedor_produto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fornecedor_id BIGINT NOT NULL,
    ingrediente_id BIGINT NOT NULL,
    preco DECIMAL(14, 4) NOT NULL,
    unidade_venda VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_fornecedor_produto PRIMARY KEY (id),
    CONSTRAINT uk_fornecedor_produto UNIQUE (fornecedor_id, ingrediente_id),
    CONSTRAINT fk_fornecedor_produto_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_fornecedor_produto_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id) ON DELETE RESTRICT,
    CONSTRAINT chk_fornecedor_produto_preco CHECK (preco >= 0),
    CONSTRAINT chk_fornecedor_produto_unidade CHECK (unidade_venda IN ('G', 'ML', 'UN', 'KG', 'L'))
) ENGINE = InnoDB;

CREATE INDEX idx_fornecedor_produto_ingrediente ON fornecedor_produto (ingrediente_id);

CREATE TABLE pedido_compra (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fornecedor_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    valor_total DECIMAL(14, 2) NOT NULL DEFAULT 0,
    usuario_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_pedido_compra PRIMARY KEY (id),
    CONSTRAINT fk_pedido_compra_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_compra_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pedido_compra_status CHECK (status IN ('RASCUNHO', 'ENVIADO', 'RECEBIDO', 'CANCELADO')),
    CONSTRAINT chk_pedido_compra_valor CHECK (valor_total >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_pedido_compra_status_created ON pedido_compra (status, created_at);
CREATE INDEX idx_pedido_compra_fornecedor ON pedido_compra (fornecedor_id);

CREATE TABLE pedido_compra_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_compra_id BIGINT NOT NULL,
    ingrediente_id BIGINT NOT NULL,
    quantidade DECIMAL(14, 4) NOT NULL,
    preco_unitario DECIMAL(14, 4) NOT NULL,
    subtotal DECIMAL(14, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_pedido_compra_item PRIMARY KEY (id),
    CONSTRAINT uk_pedido_compra_item UNIQUE (pedido_compra_id, ingrediente_id),
    CONSTRAINT fk_compra_item_compra FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra (id) ON DELETE RESTRICT,
    CONSTRAINT fk_compra_item_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id) ON DELETE RESTRICT,
    CONSTRAINT chk_compra_item_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_compra_item_preco CHECK (preco_unitario >= 0),
    CONSTRAINT chk_compra_item_subtotal CHECK (subtotal >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_compra_item_ingrediente ON pedido_compra_item (ingrediente_id);

CREATE TABLE pedido (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEBIDO',
    valor_total DECIMAL(14, 2) NOT NULL,
    endereco_entrega VARCHAR(500) NOT NULL,
    observacoes VARCHAR(1000) NULL,
    motivo_cancelamento VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_pedido PRIMARY KEY (id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pedido_status CHECK (status IN (
        'RECEBIDO', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO', 'SAIU_ENTREGA', 'FINALIZADO', 'CANCELADO'
    )),
    CONSTRAINT chk_pedido_valor CHECK (valor_total >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_pedido_cliente_created ON pedido (cliente_id, created_at);
CREATE INDEX idx_pedido_status_created ON pedido (status, created_at);

CREATE TABLE pedido_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    prato_id BIGINT NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(12, 2) NOT NULL,
    observacoes VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_pedido_item PRIMARY KEY (id),
    CONSTRAINT fk_pedido_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE RESTRICT,
    CONSTRAINT fk_pedido_item_prato FOREIGN KEY (prato_id) REFERENCES prato (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pedido_item_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_pedido_item_preco CHECK (preco_unitario >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_pedido_item_pedido ON pedido_item (pedido_id);
CREATE INDEX idx_pedido_item_prato ON pedido_item (prato_id);

CREATE TABLE estoque_movimentacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ingrediente_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    quantidade DECIMAL(14, 4) NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    lote VARCHAR(100) NULL,
    validade DATE NULL,
    custo_unitario DECIMAL(14, 4) NULL,
    pedido_compra_id BIGINT NULL,
    pedido_id BIGINT NULL,
    usuario_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_estoque_movimentacao PRIMARY KEY (id),
    CONSTRAINT fk_movimentacao_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacao_compra FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacao_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT chk_movimentacao_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA', 'ESTORNO')),
    CONSTRAINT chk_movimentacao_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_movimentacao_motivo CHECK (motivo IN (
        'COMPRA', 'VENDA', 'DESPERDICIO', 'VENCIMENTO', 'AJUSTE', 'ESTORNO'
    )),
    CONSTRAINT chk_movimentacao_custo CHECK (custo_unitario IS NULL OR custo_unitario >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_movimentacao_ingrediente_created ON estoque_movimentacao (ingrediente_id, created_at);
CREATE INDEX idx_movimentacao_pedido ON estoque_movimentacao (pedido_id);
CREATE INDEX idx_movimentacao_compra ON estoque_movimentacao (pedido_compra_id);
