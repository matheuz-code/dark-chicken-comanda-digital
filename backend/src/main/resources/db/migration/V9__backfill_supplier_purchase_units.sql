-- Preserva a semântica de registros criados antes de as unidades serem auditadas.
UPDATE pedido_compra_item item
JOIN pedido_compra pedido ON pedido.id = item.pedido_compra_id
JOIN fornecedor_produto produto
    ON produto.fornecedor_id = pedido.fornecedor_id
   AND produto.ingrediente_id = item.ingrediente_id
SET item.unidade_compra = produto.unidade_venda;

UPDATE fornecedor_produto_preco_historico historico
JOIN fornecedor_produto produto
    ON produto.id = historico.fornecedor_produto_id
SET historico.unidade_venda = produto.unidade_venda;
