import { Injectable, signal } from '@angular/core';
import { ClientProfileStateService } from './client-profile-state.service';
import { OrderPersistenceService } from './order-persistence.service';
import { OrderStatus } from './models/client-profile.model';

export interface OrderItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  image: string;
}

export type PaymentMethod = 'pix' | 'cartao' | 'dinheiro';
export type OrderType = 'delivery' | 'retirada' | 'balcao';
export type KitchenOrderStatus = 'novo' | 'em_preparo' | 'finalizado';
export type DeliveryOrderStatus = 'aguardando_motoboy' | 'em_rota' | 'entregue';

export interface ConfirmedOrder {
  code: string;
  items: OrderItem[];
  customerName: string;
  address: string;
  phone: string;
  paymentMethod: PaymentMethod;
  orderType: OrderType;
  sourceChannel: 'site';
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
  private readonly hasLoadedOrders = signal(false);

  constructor(
    private readonly orderPersistence: OrderPersistenceService,
    private readonly clientProfileState: ClientProfileStateService
  ) {
    void this.hydrateOrders();
  }

  readonly items = signal<OrderItem[]>([]);
  readonly confirmedOrder = signal<ConfirmedOrder | null>(null);
  readonly orders = signal<ConfirmedOrder[]>([]);

  setItems(items: OrderItem[]) {
    this.items.set(items);
  }

  /** Confirma o pedido localmente até a integração da fase de pedidos da API. */
  async confirmOrder(payload: {
    customerName: string;
    address: string;
    phone: string;
    paymentMethod: PaymentMethod;
    orderType?: OrderType;
  }): Promise<ConfirmedOrder> {
    await this.ensureOrdersLoaded();
    const checkoutItems = this.items();
    const total = checkoutItems.reduce((sum, item) => sum + item.price * item.quantity, 0);

    const code = `ORD-${Math.floor(1000000000 + Math.random() * 9000000000)}`;
    const confirmedOrder: ConfirmedOrder = {
      code,
      items: checkoutItems,
      customerName: payload.customerName,
      address: payload.address,
      phone: payload.phone,
      paymentMethod: payload.paymentMethod,
      orderType: payload.orderType ?? 'delivery',
      sourceChannel: 'site',
      total,
      currentStep: 0,
      kitchenStatus: 'novo',
      deliveryStatus: 'aguardando_motoboy',
      createdAtIso: new Date().toISOString(),
      estimatedPrepMinutes: this.estimatePrepMinutes(checkoutItems),
    };

    await this.orderPersistence.saveConfirmedOrder(confirmedOrder);
    this.orders.set([confirmedOrder, ...this.orders().filter((order) => order.code !== code)]);
    this.clientProfileState.addOrder({
      id: confirmedOrder.code,
      total: confirmedOrder.total,
      items: confirmedOrder.items,
      status: 'aguardando',
      createdAt: new Date(),
    });

    this.confirmedOrder.set(confirmedOrder);

    this.items.set([]);
    return confirmedOrder;
  }

  async generateMockOrder(): Promise<ConfirmedOrder> {
    await this.ensureOrdersLoaded();
    const demoItems: OrderItem[] = [
      {
        id: 1,
        name: 'Burger Classico',
        price: 25.9,
        quantity: 2,
        image:
          'https://images.unsplash.com/photo-1561758033-d89a9ad46330?auto=format&fit=crop&w=900&q=80',
      },
      {
        id: 3,
        name: 'Batata Frita',
        price: 12.9,
        quantity: 1,
        image:
          'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=900&q=80',
      },
    ];
    const total = demoItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const confirmedOrder: ConfirmedOrder = {
      code: `ORD-${Math.floor(1000000000 + Math.random() * 9000000000)}`,
      items: demoItems,
      customerName: 'Pedido de Teste',
      address: 'Rua das Palmeiras, 120',
      phone: '11999998888',
      paymentMethod: 'pix',
      orderType: 'delivery',
      sourceChannel: 'site',
      total,
      currentStep: 0,
      kitchenStatus: 'novo',
      deliveryStatus: 'aguardando_motoboy',
      createdAtIso: new Date().toISOString(),
      estimatedPrepMinutes: this.estimatePrepMinutes(demoItems),
    };

    this.orders.set([confirmedOrder, ...this.orders()]);
    this.confirmedOrder.set(confirmedOrder);
    this.clientProfileState.addOrder({
      id: confirmedOrder.code,
      total: confirmedOrder.total,
      items: confirmedOrder.items,
      status: 'aguardando',
      createdAt: new Date(),
    });
    await this.orderPersistence.saveConfirmedOrders(this.orders());
    return confirmedOrder;
  }

  async updateKitchenStatus(code: string, kitchenStatus: KitchenOrderStatus): Promise<void> {
    await this.ensureOrdersLoaded();
    const nextOrders = this.orders().map((order) => {
      if (order.code !== code) return order;

      const timestamps =
        kitchenStatus === 'em_preparo'
          ? { startedAtIso: order.startedAtIso ?? new Date().toISOString() }
          : kitchenStatus === 'finalizado'
            ? { finishedAtIso: new Date().toISOString() }
            : {};

      return {
        ...order,
        kitchenStatus,
        currentStep: this.mapKitchenStatusToStep(kitchenStatus, order.deliveryStatus),
        ...timestamps,
      };
    });

    this.orders.set(nextOrders);
    const currentOrder = nextOrders.find((order) => order.code === code) ?? null;
    if (currentOrder) {
      this.confirmedOrder.set(currentOrder);
      this.clientProfileState.updateOrderStatus(code, this.mapKitchenStatusToClientStatus(kitchenStatus));
    }
    await this.orderPersistence.saveConfirmedOrders(nextOrders);
  }

  async updateDeliveryStatus(code: string, deliveryStatus: DeliveryOrderStatus): Promise<void> {
    await this.ensureOrdersLoaded();
    const nextOrders = this.orders().map((order) => {
      if (order.code !== code) return order;

      return {
        ...order,
        deliveryStatus,
        currentStep: this.mapDeliveryStatusToStep(order.kitchenStatus, deliveryStatus),
      };
    });

    this.orders.set(nextOrders);
    const currentOrder = nextOrders.find((order) => order.code === code) ?? null;
    if (currentOrder) {
      this.confirmedOrder.set(currentOrder);
      this.clientProfileState.updateOrderStatus(code, this.mapDeliveryStatusToClientStatus(deliveryStatus));
    }
    await this.orderPersistence.saveConfirmedOrders(nextOrders);
  }

  getOrderByCode(code: string): ConfirmedOrder | undefined {
    return this.orders().find((order) => order.code === code);
  }

  clear() {
    this.items.set([]);
    this.confirmedOrder.set(null);
  }

  private async hydrateOrders() {
    if (this.hasLoadedOrders()) return;

    const persistedOrders = (await this.orderPersistence.loadConfirmedOrders()).map((order) =>
      this.normalizeOrder(order)
    );
    const latestOrder = await this.orderPersistence.loadLastConfirmedOrder();
    this.orders.set(persistedOrders);
    this.confirmedOrder.set(latestOrder ? this.normalizeOrder(latestOrder) : null);
    this.hasLoadedOrders.set(true);
  }

  private async ensureOrdersLoaded() {
    if (this.hasLoadedOrders()) return;
    await this.hydrateOrders();
  }

  private estimatePrepMinutes(items: OrderItem[]): number {
    const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
    return Math.max(15, totalItems * 8);
  }

  private mapKitchenStatusToStep(status: KitchenOrderStatus, deliveryStatus: DeliveryOrderStatus): number {
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
    deliveryStatus: DeliveryOrderStatus
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
