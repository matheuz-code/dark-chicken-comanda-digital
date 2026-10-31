package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.CanalPedido;
import br.edu.unasp.comandadigital.entity.enums.FormaPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedido")
public class Pedido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalPedido canal = CanalPedido.SITE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.RECEBIDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "endereco_entrega", nullable = false, length = 500)
    private String enderecoEntrega;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "idempotency_request_hash", length = 64)
    private String idempotencyRequestHash;

    protected Pedido() {
    }

    public static Pedido novoPedidoSite(
            Usuario cliente,
            FormaPagamento formaPagamento,
            String enderecoEntrega,
            String observacoes,
            String idempotencyKey,
            String idempotencyRequestHash) {
        Pedido pedido = new Pedido();
        pedido.cliente = cliente;
        pedido.canal = CanalPedido.SITE;
        pedido.status = StatusPedido.RECEBIDO;
        // O checkout é demonstrativo: nenhum valor foi processado neste momento.
        pedido.statusPagamento = StatusPagamento.PENDENTE;
        pedido.formaPagamento = formaPagamento;
        pedido.valorTotal = BigDecimal.ZERO;
        pedido.enderecoEntrega = enderecoEntrega;
        pedido.observacoes = observacoes;
        pedido.idempotencyKey = idempotencyKey;
        pedido.idempotencyRequestHash = idempotencyRequestHash;
        return pedido;
    }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }
    public CanalPedido getCanal() { return canal; }
    public void setCanal(CanalPedido canal) { this.canal = canal; }
    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }
    public StatusPagamento getStatusPagamento() { return statusPagamento; }
    public void setStatusPagamento(StatusPagamento statusPagamento) { this.statusPagamento = statusPagamento; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public String getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(String enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getIdempotencyRequestHash() { return idempotencyRequestHash; }
}
