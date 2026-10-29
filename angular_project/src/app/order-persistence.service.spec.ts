import { TestBed } from '@angular/core/testing';
import { ConfirmedOrder } from './order-state.service';
import { OrderPersistenceService } from './order-persistence.service';

describe('OrderPersistenceService', () => {
  let service: OrderPersistenceService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(OrderPersistenceService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('deve persistir temporariamente os pedidos no armazenamento local', async () => {
    const order = confirmedOrder();

    await service.saveConfirmedOrder(order);

    expect(await service.loadConfirmedOrders()).toEqual([order]);
    expect(await service.loadLastConfirmedOrder()).toEqual(order);
  });
});

function confirmedOrder(): ConfirmedOrder {
  return {
    code: 'ORD-1234567890',
    items: [
      {
        id: 1,
        name: 'X-Burger',
        price: 25.9,
        quantity: 1,
        image: 'https://example.com/burger.jpg',
      },
    ],
    customerName: 'Cliente Teste',
    address: 'Rua Teste, 10',
    phone: '11999999999',
    paymentMethod: 'pix',
    orderType: 'delivery',
    sourceChannel: 'site',
    total: 25.9,
    currentStep: 0,
    kitchenStatus: 'novo',
    deliveryStatus: 'aguardando_motoboy',
    createdAtIso: '2026-08-25T12:00:00.000Z',
    estimatedPrepMinutes: 20,
  };
}
