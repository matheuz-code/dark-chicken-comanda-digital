import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  PedidoAdmin,
  PedidoAdminDetalhe,
  StatusPedido,
} from '../models/admin.model';
import { AdminApiService } from '../services/admin-api.service';

const PROXIMO_STATUS: Partial<Record<StatusPedido, StatusPedido>> = {
  RECEBIDO: 'CONFIRMADO',
  CONFIRMADO: 'EM_PREPARO',
  EM_PREPARO: 'PRONTO',
  PRONTO: 'SAIU_ENTREGA',
  SAIU_ENTREGA: 'FINALIZADO',
};

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-orders.html',
  styleUrl: './admin-orders.css',
})
export class AdminOrders implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly router = inject(Router);

  protected pedidos: PedidoAdmin[] = [];
  protected detalhe?: PedidoAdminDetalhe;
  protected statusFiltro: StatusPedido | '' = '';
  protected motivoCancelamento = '';
  protected loading = true;
  protected saving = false;
  protected message = '';
  protected errorMessage = '';
  protected readonly statusDisponiveis: Array<StatusPedido | ''> = [
    '', 'RECEBIDO', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO',
    'SAIU_ENTREGA', 'FINALIZADO', 'CANCELADO',
  ];

  ngOnInit(): void {
    this.carregar();
  }

  protected voltar(): void {
    void this.router.navigateByUrl('/admin');
  }

  protected carregar(): void {
    this.loading = true;
    this.errorMessage = '';
    this.api.listarPedidos(this.statusFiltro).subscribe({
      next: (pagina) => {
        this.pedidos = pagina.conteudo;
        this.loading = false;
        if (this.detalhe) this.abrirDetalhe(this.detalhe.id);
      },
      error: (error) => this.tratarErro(error, 'Não foi possível carregar os pedidos.'),
    });
  }

  protected abrirDetalhe(id: number): void {
    this.api.detalharPedido(id).subscribe({
      next: (detalhe) => {
        this.detalhe = detalhe;
        this.motivoCancelamento = '';
      },
      error: (error) => this.tratarErro(error, 'Não foi possível detalhar o pedido.'),
    });
  }

  protected fecharDetalhe(): void {
    this.detalhe = undefined;
  }

  protected proximoStatus(status: StatusPedido): StatusPedido | undefined {
    return PROXIMO_STATUS[status];
  }

  protected avancar(): void {
    if (!this.detalhe) return;
    const proximo = this.proximoStatus(this.detalhe.status);
    if (!proximo) return;
    this.saving = true;
    this.api.atualizarStatusPedido(this.detalhe.id, proximo).subscribe({
      next: (detalhe) => this.operacaoConcluida(detalhe, `Pedido avançado para ${proximo}.`),
      error: (error) => this.tratarErro(error, 'Não foi possível atualizar o pedido.'),
    });
  }

  protected cancelar(): void {
    if (!this.detalhe || this.motivoCancelamento.trim().length < 3) return;
    this.saving = true;
    this.api.cancelarPedido(this.detalhe.id, this.motivoCancelamento.trim()).subscribe({
      next: (detalhe) => this.operacaoConcluida(detalhe, this.mensagemCancelamento(detalhe)),
      error: (error) => this.tratarErro(error, 'Não foi possível cancelar o pedido.'),
    });
  }

  protected statusLabel(status: string): string {
    return status.replaceAll('_', ' ');
  }

  private operacaoConcluida(detalhe: PedidoAdminDetalhe, mensagem: string): void {
    this.detalhe = detalhe;
    this.message = mensagem;
    this.errorMessage = '';
    this.saving = false;
    this.api.listarPedidos(this.statusFiltro).subscribe((pagina) => {
      this.pedidos = pagina.conteudo;
    });
  }

  private mensagemCancelamento(detalhe: PedidoAdminDetalhe): string {
    return detalhe.statusPagamento === 'ESTORNADO'
      ? 'Pedido cancelado e pagamento estornado.'
      : 'Pedido cancelado; nenhum pagamento foi confirmado.';
  }

  private tratarErro(error: HttpErrorResponse, fallback: string): void {
    this.loading = false;
    this.saving = false;
    this.errorMessage = error.error?.mensagem ?? fallback;
  }
}
