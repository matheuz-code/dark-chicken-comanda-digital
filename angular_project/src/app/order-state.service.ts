import { Injectable, signal } from '@angular/core';
import type { StatusPedido } from './models/admin.model';
import { AuthService } from './services/auth.service';
import { ClientProfileStateService } from './client-profile-state.service';
import { OrderPersistenceService } from './order-persistence.service';
import { OrderStatus } from './models/client-profile.model';

export interface OrderItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  image: string;
  notes?: string;
}

export type PaymentMethod = 'pix' | 'cartao' | 'dinheiro';
export type OrderType = 'delivery' | 'retirada' | 'balcao';
export type KitchenOrderStatus = 'novo' | 'em_preparo' | 'finalizado';
export type DeliveryOrderStatus = 'aguardando_motoboy' | 'em_rota' | 'entregue';

export interface ConfirmedOrder {
  backendStatus?: StatusPedido;
  code: string;
  items: OrderItem[];
  customerName: string;
  address: string;
  phone: string;
  paymentMethod: PaymentMethod;
  orderType: OrderType;
  sourceChannel: 'site' | 'ifood' | 'whatsapp' | 'telefone' | 'outro';
  total: number;
  currentStep: number;
  kitchenStatus: KitchenOrderStatus;
  deliveryStatus: DeliveryOrderStatus;
  createdAtIso: string;
  estimatedPrepMinutes: number;
  startedAtIso?: string;
  finishedAtIso?: string;
}

@Injectable({ providedIn: 'root' })
export class OrderStateService {
  private sessionVersion = 0;

  constructor(
    private readonly orderPersistence: OrderPersistenceService,
    private readonly clientProfileState: ClientProfileStateService,
    private readonly auth: AuthService,
  ) {
    this.auth.sessaoAlterada.subscribe(() => {
      this.sessionVersion++;
      this.refreshing = undefined;
      this.orders.set([]);
      this.confirmedOrder.set(null);
      this.syncProfile();
      if (!this.auth.usuario()) this.setItems([]);
      void this.refresh().catch(() => {});
    });
    this.restoreDraft();
    void this.refresh().catch(() => {});
  }

  readonly items = signal<OrderItem[]>([]);
  readonly confirmedOrder = signal<ConfirmedOrder | null>(null);
  readonly orders = signal<ConfirmedOrder[]>([]);

  private restoreDraft(): void {
    try {
      const saved: unknown = JSON.parse(sessionStorage.getItem('dark-chicken.cart-draft') || '[]');
      if (
        Array.isArray(saved) &&
        saved.length <= 50 &&
        saved.every(
          (item) =>
            item &&
            Number.isSafeInteger(item.id) &&
            item.id > 0 &&
            Number.isInteger(item.quantity) &&
            item.quantity >= 1 &&
            item.quantity <= 100 &&
            Number.isFinite(item.price) &&
            item.price >= 0 &&
            typeof item.name === 'string' &&
            typeof item.image === 'string' &&
            (!item.notes || typeof item.notes === 'string'),
        )
      )
        this.items.set(saved);
    } catch {
      /* Rascunho inválido não é tratado como pedido confirmado. */
    }
  }

  setItems(items: OrderItem[]) {
    this.items.set(items);
    try {
      sessionStorage.setItem('dark-chicken.cart-draft', JSON.stringify(items));
    } catch {
      /* Rascunho permanece em memória. */
    }
  }

  /** Pedido confirmado somente após resposta da API; nenhum pedido local é criado. */
  async confirmOrder(payload: {
    customerName: string;
    address: string;
    phone: string;
    paymentMethod: PaymentMethod;
    orderType?: OrderType;
  }): Promise<ConfirmedOrder> {
    const confirmedOrder = await this.orderPersistence.create(this.items(), payload);
    this.orders.update((orders) => [
      confirmedOrder,
      ...orders.filter((order) => order.code !== confirmedOrder.code),
    ]);
    this.syncProfile();
    this.confirmedOrder.set(confirmedOrder);

    this.setItems([]);
    return confirmedOrder;
  }

  /** A ação de teste da cozinha agora abre o fluxo real do cliente, sem inserir pedidos locais. */
  async updateKitchenStatus(code: string, kitchenStatus: KitchenOrderStatus): Promise<void> {
    await this.setBackendStatus(code, kitchenStatus === 'em_preparo' ? 'EM_PREPARO' : 'PRONTO');
  }

  async confirmReceipt(code: string): Promise<void> {
    await this.setBackendStatus(code, 'CONFIRMADO');
  }

  async updateDeliveryStatus(code: string, deliveryStatus: DeliveryOrderStatus): Promise<void> {
    await this.setBackendStatus(
      code,
      deliveryStatus === 'entregue' ? 'FINALIZADO' : 'SAIU_ENTREGA',
    );
  }

  private async setBackendStatus(code: string, status: StatusPedido): Promise<void> {
    const updated = await this.orderPersistence.updateStatus(code, status);
    this.orders.update((orders) => orders.map((order) => (order.code === code ? updated : order)));
    this.confirmedOrder.set(updated);
    this.syncProfile();
  }

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  private refreshing?: Promise<void>;

  refresh(): Promise<void> {
    if (this.refreshing) return this.refreshing;
    this.loading.set(true);
    this.loadError.set(null);
    const version = this.sessionVersion;
    const request = this.orderPersistence
      .loadConfirmedOrders()
      .then((orders) => {
        if (version !== this.sessionVersion) return;
        this.orders.set(orders);
        this.confirmedOrder.set(orders[0] ?? null);
        this.syncProfile();
      })
      .catch((error) => {
        if (version !== this.sessionVersion) return;
        this.orders.set([]);
        this.confirmedOrder.set(null);
        this.syncProfile();
        this.loadError.set(
          'Não foi possível consultar os pedidos na API. Nenhum histórico local foi usado como substituto.',
        );
        throw error;
      })
      .finally(() => {
        if (version === this.sessionVersion) {
          this.loading.set(false);
          this.refreshing = undefined;
        }
      });
    this.refreshing = request;
    return this.refreshing;
  }

  async refreshOrder(code: string): Promise<void> {
    const order = await this.orderPersistence.loadById(code);
    this.orders.update((orders) => [order, ...orders.filter((item) => item.code !== code)]);
    this.confirmedOrder.set(order);
    this.syncProfile();
  }

  private syncProfile(): void {
    this.clientProfileState.setApiOrders(this.orders());
  }

  getOrderByCode(code: string): ConfirmedOrder | undefined {
    return this.orders().find((order) => order.code === code);
  }

  clear() {
    this.setItems([]);
    this.confirmedOrder.set(null);
  }

  private estimatePrepMinutes(items: OrderItem[]): number {
    const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
    return Math.max(15, totalItems * 8);
  }

  private mapKitchenStatusToStep(
    status: KitchenOrderStatus,
    deliveryStatus: DeliveryOrderStatus,
  ): number {
    if (status === 'novo') return 0;
    if (status === 'em_preparo') return 2;
    return this.mapDeliveryStatusToStep(status, deliveryStatus);
  }

  private mapKitchenStatusToClientStatus(status: KitchenOrderStatus): OrderStatus {
    if (status === 'novo') return 'aguardando';
    if (status === 'em_preparo') return 'em_preparo';
    return 'pronto';
  }

  private mapDeliveryStatusToStep(
    kitchenStatus: KitchenOrderStatus,
    deliveryStatus: DeliveryOrderStatus,
  ): number {
    if (kitchenStatus === 'novo') return 0;
    if (kitchenStatus === 'em_preparo') return 2;
    if (deliveryStatus === 'em_rota') return 4;
    if (deliveryStatus === 'entregue') return 5;
    return 3;
  }

  private mapDeliveryStatusToClientStatus(status: DeliveryOrderStatus): OrderStatus {
    if (status === 'entregue') return 'entregue';
    return 'pronto';
  }

  private normalizeOrder(order: ConfirmedOrder): ConfirmedOrder {
    const deliveryStatus =
      order.deliveryStatus ??
      (order.currentStep >= 5
        ? 'entregue'
        : order.currentStep >= 4
          ? 'em_rota'
          : 'aguardando_motoboy');

    return {
      ...order,
      deliveryStatus,
      currentStep: this.mapDeliveryStatusToStep(order.kitchenStatus, deliveryStatus),
    };
  }
}
