import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, forkJoin, of, switchMap } from 'rxjs';
import { AuthService } from './services/auth.service';
import { environment } from '../environments/environment';
import type { ConfirmedOrder, PaymentMethod } from './order-state.service';
import type { StatusPedido } from './models/admin.model';

export interface PedidoResponse {
  id: number;
  status: StatusPedido;
  valorTotal: number;
  enderecoEntrega: string;
  formaPagamento: 'PIX' | 'CARTAO' | 'DINHEIRO';
  criadoEm: string;
  canal?: 'SITE' | 'IFOOD' | 'WHATSAPP' | 'TELEFONE' | 'OUTRO';
  cliente?: { nome: string; telefone?: string };
  itens: Array<{
    pratoId: number;
    nomePrato: string;
    fotoUrl?: string;
    quantidade: number;
    precoUnitario: number;
    observacoes?: string;
  }>;
}

/** Pedidos confirmados vêm exclusivamente da API; dados locais antigos não são apagados nem importados. */
@Injectable({ providedIn: 'root' })
export class OrderPersistenceService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = environment.apiUrl;

  async loadConfirmedOrders(): Promise<ConfirmedOrder[]> {
    const user = this.auth.usuario();
    if (!user) return [];
    const path = user.perfil === 'CLIENTE' ? '/pedidos/meus' : '/admin/pedidos';
    const result = await firstValueFrom(
      this.http
        .get<{ conteudo: Array<{ id: number }> }>(`${this.base}${path}`, { params: { size: '50' } })
        .pipe(
          switchMap((page) =>
            page.conteudo.length
              ? forkJoin(page.conteudo.map((order) => this.detail(order.id)))
              : of([]),
          ),
        ),
    );
    this.assertSameUser(user.id);
    return result;
  }

  async loadLastConfirmedOrder(): Promise<ConfirmedOrder | null> {
    return (await this.loadConfirmedOrders())[0] ?? null;
  }

  private detail(id: number) {
    const userId = this.auth.usuario()?.id;
    const prefix = this.auth.usuario()?.perfil === 'CLIENTE' ? '/pedidos' : '/admin/pedidos';
    return this.http.get<PedidoResponse>(`${this.base}${prefix}/${id}`).pipe(
      switchMap((order) => {
        this.assertSameUser(userId);
        return of(this.fromApi(order));
      }),
    );
  }

  async loadById(id: string): Promise<ConfirmedOrder> {
    if (!/^\d+$/.test(id)) throw new Error('Identificador de pedido inválido.');
    return firstValueFrom(this.detail(Number(id)));
  }

  async create(
    items: ConfirmedOrder['items'],
    payload: { address: string; paymentMethod: PaymentMethod },
  ): Promise<ConfirmedOrder> {
    if (this.auth.usuario()?.perfil !== 'CLIENTE')
      throw new Error('Entre como cliente para confirmar o pedido.');
    const userId = this.auth.usuario()?.id;
    const body = {
      itens: items.map((item) => ({
        pratoId: item.id,
        quantidade: item.quantity,
        observacoes: item.notes || null,
      })),
      enderecoEntrega: payload.address,
      formaPagamento: payload.paymentMethod.toUpperCase(),
    };
    // Sem retry: repetir um POST após perda de resposta pode duplicar um pedido.
    const result = await firstValueFrom(
      this.http.post<PedidoResponse>(`${this.base}/pedidos`, body),
    );
    this.assertSameUser(userId);
    return this.fromApi(result);
  }

  async updateStatus(code: string, status: StatusPedido): Promise<ConfirmedOrder> {
    if (!/^\d+$/.test(code)) throw new Error('Identificador de pedido inválido.');
    const userId = this.auth.usuario()?.id;
    const result = await firstValueFrom(
      this.http.patch<PedidoResponse>(`${this.base}/admin/pedidos/${code}/status`, { status }),
    );
    this.assertSameUser(userId);
    return this.fromApi(result);
  }

  private assertSameUser(id: number | undefined): void {
    if (id === undefined || this.auth.usuario()?.id !== id)
      throw new Error('A sessão mudou durante a consulta. Entre novamente.');
  }

  private fromApi(order: PedidoResponse): ConfirmedOrder {
    const steps: Record<StatusPedido, number> = {
      RECEBIDO: 0,
      CONFIRMADO: 1,
      EM_PREPARO: 2,
      PRONTO: 3,
      SAIU_ENTREGA: 4,
      FINALIZADO: 5,
      CANCELADO: -1,
    };
    if (
      !order ||
      !Number.isSafeInteger(order.id) ||
      order.id <= 0 ||
      !Object.hasOwn(steps, order.status) ||
      !['PIX', 'CARTAO', 'DINHEIRO'].includes(order.formaPagamento) ||
      !Number.isFinite(order.valorTotal) ||
      order.valorTotal < 0 ||
      typeof order.criadoEm !== 'string' ||
      !Number.isFinite(Date.parse(order.criadoEm)) ||
      typeof order.enderecoEntrega !== 'string' ||
      !Array.isArray(order.itens) ||
      order.itens.some(
        (item) =>
          !item ||
          !Number.isSafeInteger(item.pratoId) ||
          item.pratoId <= 0 ||
          typeof item.nomePrato !== 'string' ||
          !Number.isInteger(item.quantidade) ||
          item.quantidade <= 0 ||
          !Number.isFinite(item.precoUnitario) ||
          item.precoUnitario < 0,
      )
    ) {
      throw new Error('Resposta de pedido incompatível com a API.');
    }
    return {
      code: String(order.id),
      backendStatus: order.status,
      items: order.itens.map((item) => ({
        id: item.pratoId,
        name: item.nomePrato,
        price: Number(item.precoUnitario),
        quantity: item.quantidade,
        image: item.fotoUrl || '/images/prato-sem-foto.svg',
        notes: item.observacoes || '',
      })),
      customerName: order.cliente?.nome || this.auth.usuario()?.nome || '',
      phone: order.cliente?.telefone || this.auth.usuario()?.telefone || '',
      address: order.enderecoEntrega,
      paymentMethod: order.formaPagamento.toLowerCase() as PaymentMethod,
      orderType: 'delivery',
      sourceChannel: (order.canal || 'SITE').toLowerCase() as ConfirmedOrder['sourceChannel'],
      total: Number(order.valorTotal),
      createdAtIso: order.criadoEm,
      currentStep: steps[order.status],
      kitchenStatus:
        order.status === 'EM_PREPARO'
          ? 'em_preparo'
          : steps[order.status] >= 3
            ? 'finalizado'
            : 'novo',
      deliveryStatus:
        order.status === 'FINALIZADO'
          ? 'entregue'
          : order.status === 'SAIU_ENTREGA'
            ? 'em_rota'
            : 'aguardando_motoboy',
      estimatedPrepMinutes: 0,
    };
  }
}
