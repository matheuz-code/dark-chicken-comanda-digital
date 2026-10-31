ALTER TABLE pedido
    ADD COLUMN idempotency_key VARCHAR(100) NULL,
    ADD COLUMN idempotency_request_hash VARCHAR(64) NULL,
    ADD CONSTRAINT uk_pedido_cliente_idempotency UNIQUE (cliente_id, idempotency_key);
