import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
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
    this.items().reduce((sum, item) => sum + item.price * item.quantity, 0)
  );

  protected readonly checkoutForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    address: ['', [Validators.required, Validators.minLength(8)]],
    phone: ['', [Validators.required, Validators.minLength(10)]],
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
   * Confirma o pedido e prepara para futura integração com Firebase.
   * No futuro, aqui será feita a chamada para salvar o pedido no banco de dados Firebase.
   */
  protected async confirmOrder() {
    this.checkoutForm.markAllAsTouched();
    if (this.checkoutForm.invalid || !this.items().length) return;

    const { fullName, address, phone, paymentMethod } = this.checkoutForm.getRawValue();
    if (!fullName || !address || !phone || !paymentMethod) return;

    // Aqui será feita a integração com o Firebase futuramente
    // Exemplo:

    const confirmedOrder = await this.orderState.confirmOrder({
      customerName: fullName,
      address,
      phone,
      paymentMethod,
    });
    this.router.navigateByUrl(`/acompanhar-pedido/${confirmedOrder.code}`);
  }

  protected trackByItem(index: number, item: { id: number }) {
    return item.id;
  }
}
