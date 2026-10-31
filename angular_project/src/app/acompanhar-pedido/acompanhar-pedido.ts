import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ClientProfileStateService } from '../client-profile-state.service';
import { ConfirmedOrder, OrderStateService } from '../order-state.service';

interface Step {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-acompanhar-pedido',
  imports: [CommonModule],
  templateUrl: './acompanhar-pedido.html',
  styleUrl: './acompanhar-pedido.css',
})
export class AcompanharPedido implements OnInit, OnDestroy {
  private timer?: ReturnType<typeof setInterval>;
  protected error: string | null = null;
  ngOnInit(): void {
    this.reload();
    this.timer = setInterval(() => {
      if (document.visibilityState === 'visible') this.reload();
    }, 15000);
  }
  ngOnDestroy(): void {
    clearInterval(this.timer);
  }
  private pending = false;
  private reload(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id || this.pending) return;
    this.pending = true;
    void this.orderState
      .refreshOrder(id)
      .then(() => {
        this.error = null;
      })
      .catch(() => {
        this.error = 'Não foi possível atualizar este pedido pela API.';
      })
      .finally(() => {
        this.pending = false;
      });
  }
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly orderState = inject(OrderStateService);
  private readonly profileState = inject(ClientProfileStateService);

  protected readonly order = computed<ConfirmedOrder | null>(() => {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (!orderId) {
      return this.orderState.confirmedOrder();
    }

    const stateOrder = this.orderState.getOrderByCode(orderId);
    if (stateOrder) {
      return stateOrder;
    }

    return null;
  });
  protected readonly steps: Step[] = [
    { label: 'Pedido Recebido', icon: '🕒' },
    { label: 'Aceito', icon: '✅' },
    { label: 'Em Preparo', icon: '👨‍🍳' },
    { label: 'Pronto', icon: '📦' },
    { label: 'Saiu para Entrega', icon: '🛵' },
    { label: 'Entregue', icon: '🏁' },
  ];

  protected readonly progressPercent = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return 0;
    return ((currentOrder.currentStep + 1) / this.steps.length) * 100;
  });

  protected readonly progressText = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return '0 de 6 etapas concluidas';
    if (currentOrder.backendStatus === 'CANCELADO') return 'Pedido cancelado';
    return `${currentOrder.currentStep + 1} de ${this.steps.length} etapas concluidas`;
  });

  protected readonly paymentLabel = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return '-';
    return this.mapPaymentLabel(currentOrder.paymentMethod);
  });

  protected readonly deliveryFee = 0;
  protected readonly subtotal = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return 0;
    return currentOrder.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  });

  protected goBack() {
    this.router.navigateByUrl('/cliente/perfil');
  }

  protected trackByStep(index: number, step: Step) {
    return step.label;
  }

  protected trackByItem(index: number, item: { id: number }) {
    return item.id;
  }

  protected isStepDone(index: number, order: ConfirmedOrder | null): boolean {
    return !!order && index <= order.currentStep;
  }

  private mapPaymentLabel(method: ConfirmedOrder['paymentMethod']) {
    if (method === 'pix') return 'Pix';
    if (method === 'cartao') return 'Cartao';
    return 'Dinheiro';
  }

  private mapStatusToStep(
    status: 'aguardando' | 'em_preparo' | 'pronto' | 'entregue' | 'cancelado',
  ): number {
    if (status === 'aguardando') return 0;
    if (status === 'em_preparo') return 2;
    if (status === 'pronto') return 3;
    if (status === 'entregue') return 5;
    return 0;
  }
}
