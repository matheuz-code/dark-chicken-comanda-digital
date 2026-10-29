-- RF-006: preserva a forma escolhida no checkout com pagamento simulado.
ALTER TABLE pedido
    ADD COLUMN forma_pagamento VARCHAR(20) NOT NULL DEFAULT 'PIX' AFTER status_pagamento,
    ADD CONSTRAINT chk_pedido_forma_pagamento
        CHECK (forma_pagamento IN ('PIX', 'CARTAO', 'DINHEIRO'));
