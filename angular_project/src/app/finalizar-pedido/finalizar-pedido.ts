import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { OrderStateService } from '../order-state.service';

type PaymentMethod = 'pix' | 'cartao' | 'dinheiro';

@Component({
  selector: 'app-finalizar-pedido',
  imports: [CommonModule, ReactiveFormsModule, RouterModule, CurrencyPipe],
  templateUrl: './finalizar-pedido.html',
  styleUrl: './finalizar-pedido.css',
})
export class FinalizarPedido {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly orderState = inject(OrderStateService);

  protected readonly items = this.orderState.items;
  protected readonly total = computed(() =>
    this.items().reduce((sum, item) => sum + item.price * item.quantity, 0),
  );

  private readonly user = inject(AuthService).usuario();

  protected readonly checkoutForm = this.fb.group({
    fullName: [this.user?.nome ?? ''],
    address: [this.user?.endereco ?? '', [Validators.required, Validators.minLength(8)]],
    phone: [this.user?.telefone ?? ''],
    paymentMethod: ['pix' as PaymentMethod, [Validators.required]],
  });

  protected backToCart() {
    this.router.navigateByUrl('/cardapio');
  }

  protected selectPayment(method: PaymentMethod) {
    this.checkoutForm.patchValue({ paymentMethod: method });
  }

  protected onPhoneInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const onlyDigits = input.value.replace(/\D/g, '').slice(0, 11);
    input.value = onlyDigits;
    this.checkoutForm.patchValue({ phone: onlyDigits }, { emitEvent: false });
  }

  /**
   * Confirma pela API Spring Boot; preço, identificador e status são definidos pelo servidor.
   */
  protected submitting = false;
  protected submitError: string | null = null;

  protected async confirmOrder() {
    if (this.submitting) return;
    this.checkoutForm.markAllAsTouched();
    if (this.checkoutForm.invalid || !this.items().length) return;

    const { fullName, address, phone, paymentMethod } = this.checkoutForm.getRawValue();
    if (!address || !paymentMethod) return;

    this.submitting = true;
    this.submitError = null;
    try {
      const confirmedOrder = await this.orderState.confirmOrder({
        customerName: fullName ?? '',
        address,
        phone: phone ?? '',
        paymentMethod,
      });
      await this.router.navigateByUrl(`/acompanhar-pedido/${confirmedOrder.code}`);
    } catch {
      this.submitError =
        'Não foi possível confirmar a resposta da API. Consulte seu histórico antes de repetir para evitar um pedido duplicado.';
    } finally {
      this.submitting = false;
    }
  }

  protected trackByItem(index: number, item: { id: number }) {
    return item.id;
  }
}
