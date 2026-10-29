import { Injectable, computed, signal } from '@angular/core';
import { CLIENT_PROFILE_API_MOCK } from './mocks/client-profile-api.mock';
import { ClientOrder, ClientProfileDto, OrderStatus } from './models/client-profile.model';

@Injectable({ providedIn: 'root' })
export class ClientProfileStateService {
  private readonly profile = signal(this.fromApiDto(CLIENT_PROFILE_API_MOCK));

  readonly clientName = computed(() => this.profile().name);
  readonly clientEmail = computed(() => this.profile().email);

  readonly orders = computed(() =>
    [...this.profile().orders].sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())
  );

  readonly totalOrders = computed(() => this.orders().length);
  readonly totalSpent = computed(() => this.orders().reduce((sum, order) => sum + order.total, 0));
  readonly averageTicket = computed(() => {
    const count = this.totalOrders();
    if (!count) return 0;
    return this.totalSpent() / count;
  });
  readonly lastOrderDate = computed(() => this.orders()[0]?.createdAt ?? null);

  readonly frequencyLabel = computed(() => {
    const count = this.totalOrders();
    if (count <= 1) return 'Cliente Novo';
    if (count <= 5) return 'Recorrente';
    return 'Fiel';
  });

  readonly vipLabel = computed(() => {
    const spent = this.totalSpent();
    if (spent >= 1000) return 'VIP Gold';
    if (spent >= 400) return 'VIP Silver';
    return 'Regular';
  });

  readonly daysFromLastActivity = computed(() => {
    const lastOrder = this.lastOrderDate();
    if (!lastOrder) return 0;
    const diffMs = Date.now() - lastOrder.getTime();
    return Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));
  });

  readonly activeOrdersCount = computed(
    () => this.orders().filter((order) => this.isActive(order.status)).length
  );

  getOrderById(orderId: string): ClientOrder | undefined {
    return this.orders().find((order) => order.id === orderId);
  }

  isActive(status: OrderStatus): boolean {
    return status === 'aguardando' || status === 'em_preparo' || status === 'pronto';
  }

  getStatusLabel(status: OrderStatus): string {
    if (status === 'aguardando') return 'Aguardando';
    if (status === 'em_preparo') return 'Em preparo';
    if (status === 'pronto') return 'Pronto para entrega';
    if (status === 'entregue') return 'Entregue';
    return 'Cancelado';
  }

  addOrder(order: {
    id: string;
    total: number;
    items: ClientOrder['items'];
    status: OrderStatus;
    createdAt: Date;
  }) {
    const generatedCode = this.generateVerificationCode();
    const nextOrder: ClientOrder = {
      id: order.id,
      total: order.total,
      status: order.status,
      createdAt: order.createdAt,
      items: order.items,
      verificationCode: this.isActive(order.status) ? generatedCode : undefined,
      verificationCodeExpiresAtIso: this.isActive(order.status)
        ? new Date(Date.now() + 1000 * 60 * 90).toISOString()
        : undefined,
    };

    this.profile.update((current) => ({
      ...current,
      orders: [nextOrder, ...current.orders],
    }));
  }

  updateOrderStatus(orderId: string, status: OrderStatus) {
    this.profile.update((current) => {
      const nextOrders = current.orders.map((order, index) => {
        if (order.id !== orderId) {
          return order;
        }

        if (status === 'entregue' || status === 'cancelado') {
          return {
            ...order,
            status,
            verificationCode: undefined,
            verificationCodeExpiresAtIso: undefined,
          };
        }

        return {
          ...order,
          status,
        };
      });

      return { ...current, orders: nextOrders };
    });
  }

  private fromApiDto(dto: ClientProfileDto): { name: string; email: string; orders: ClientOrder[] } {
    return {
      name: dto.name,
      email: dto.email,
      orders: dto.orders.map((order) => ({
        ...order,
        createdAt: new Date(order.createdAtIso),
      })),
    };
  }

  private generateVerificationCode(): string {
    return `${Math.floor(1000 + Math.random() * 9000)}`;
  }
}
