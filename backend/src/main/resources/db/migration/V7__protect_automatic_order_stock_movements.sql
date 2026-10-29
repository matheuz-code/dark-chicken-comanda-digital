-- RF/RN de baixa automática: cada ingrediente pode gerar somente uma saída
-- de venda e um estorno por pedido. Pedidos nulos continuam aceitando
-- múltiplas movimentações manuais por causa da semântica de NULL do MySQL.
ALTER TABLE estoque_movimentacao
    ADD CONSTRAINT uk_mov_pedido_ingrediente_tipo_motivo
        UNIQUE (pedido_id, ingrediente_id, tipo, motivo);
