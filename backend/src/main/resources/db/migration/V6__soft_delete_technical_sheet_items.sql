-- RN-006: ingredientes removidos da ficha técnica devem ser desativados,
-- nunca apagados fisicamente.
ALTER TABLE ficha_tecnica_item
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' AFTER fator_correcao,
    ADD CONSTRAINT chk_ficha_tecnica_item_status
        CHECK (status IN ('ATIVO', 'INATIVO'));

CREATE INDEX idx_ficha_item_ficha_status
    ON ficha_tecnica_item (ficha_tecnica_id, status);
