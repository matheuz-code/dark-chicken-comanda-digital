import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';
import {
  ConfirmedOrder,
  KitchenOrderStatus,
  OrderStateService,
  OrderType,
} from '../order-state.service';

type KitchenTab = 'novos' | 'em_preparo' | 'finalizados';

@Component({
  selector: 'app-cozinha',
  imports: [CommonModule, RouterModule, CurrencyPipe, DatePipe],
  templateUrl: './cozinha.html',
  styleUrl: './cozinha.css',
})
export class Cozinha implements OnInit, OnDestroy {
  private timer?: ReturnType<typeof setInterval>;
  protected readonly auth = inject(AuthService);
  protected actionError: string | null = null;
  protected busy = false;
  ngOnInit(): void {
    this.refresh();
    this.timer = setInterval(() => {
      if (document.visibilityState === 'visible') this.refresh();
    }, 15000);
  }
  ngOnDestroy(): void {
    clearInterval(this.timer);
  }
  protected refresh(): void {
    void this.orderState.refresh().catch(() => {});
  }
  protected canConfirm(): boolean {
    return ['ADMIN', 'GERENTE'].includes(this.auth.usuario()?.perfil || '');
  }
  protected async confirmReceipt(code: string): Promise<void> {
    await this.act(() => this.orderState.confirmReceipt(code));
  }
  private async act(action: () => Promise<void>): Promise<boolean> {
    if (this.busy) return false;
    this.busy = true;
    this.actionError = null;
    try {
      await action();
      return true;
    } catch {
      this.actionError =
        'A API não confirmou a alteração. Atualize a lista antes de tentar novamente.';
      return false;
    } finally {
      this.busy = false;
    }
  }
  private readonly router = inject(Router);
  protected readonly orderState = inject(OrderStateService);

  protected readonly activeTab = signal<KitchenTab>('novos');

  protected readonly orders = computed(() =>
    this.orderState
      .orders()
      .filter((order) => order.backendStatus !== 'CANCELADO')
      .sort((a, b) => new Date(b.createdAtIso).getTime() - new Date(a.createdAtIso).getTime()),
  );

  protected readonly newOrders = computed(() =>
    this.orders().filter((order) => order.kitchenStatus === 'novo'),
  );
  protected readonly preparingOrders = computed(() =>
    this.orders().filter((order) => order.kitchenStatus === 'em_preparo'),
  );
  protected readonly finishedOrders = computed(() =>
    this.orders().filter((order) => order.kitchenStatus === 'finalizado'),
  );

  protected readonly totalRevenue = computed(() =>
    this.orders().reduce((sum, order) => sum + order.total, 0),
  );
  protected readonly deliveredCount = computed(() => this.finishedOrders().length);
  protected readonly highlightedOrders = computed(() => {
    if (this.activeTab() === 'novos') return this.newOrders();
    if (this.activeTab() === 'em_preparo') return this.preparingOrders();
    return this.finishedOrders();
  });

  protected goBack() {
    this.router.navigateByUrl('/');
  }

  protected goToProducts() {
    this.router.navigate(['/cardapio'], { queryParams: { modo: 'gerenciar' } });
  }

  protected async createDemoOrder() {
    await this.router.navigateByUrl('/cardapio');
  }

  protected setTab(tab: KitchenTab) {
    this.activeTab.set(tab);
  }

  protected async startPreparation(code: string) {
    if (await this.act(() => this.orderState.updateKitchenStatus(code, 'em_preparo')))
      this.activeTab.set('em_preparo');
  }

  protected async finishOrder(code: string) {
    if (await this.act(() => this.orderState.updateKitchenStatus(code, 'finalizado')))
      this.activeTab.set('finalizados');
  }

  protected getOrderTypeLabel(orderType: OrderType): string {
    if (orderType === 'retirada') return 'Retirada';
    if (orderType === 'balcao') return 'Balcao';
    return 'Delivery';
  }

  protected getStatusLabel(status: KitchenOrderStatus): string {
    if (status === 'novo') return 'Novo pedido';
    if (status === 'em_preparo') return 'Em preparo';
    return 'Finalizado';
  }

  protected getStatusTone(status: KitchenOrderStatus): string {
    if (status === 'novo') return 'chip chip--new';
    if (status === 'em_preparo') return 'chip chip--prep';
    return 'chip chip--done';
  }

  protected getCurrentTabLabel(): string {
    if (this.activeTab() === 'novos') return 'novos';
    if (this.activeTab() === 'em_preparo') return 'em preparo';
    return 'finalizados';
  }

  protected trackByOrder(index: number, order: ConfirmedOrder) {
    return order.code;
  }
}
