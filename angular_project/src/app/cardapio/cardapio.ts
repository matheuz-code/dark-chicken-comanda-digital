import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  PratoCardapioResponse,
  PratoDetalheResponse,
} from '../models/cardapio.model';
import { OrderStateService } from '../order-state.service';
import { CardapioService } from '../services/cardapio.service';

type CategoryId = number | 'todos';

interface CategoryOption {
  id: CategoryId;
  label: string;
}

interface MenuItem {
  id: number;
  name: string;
  description: string;
  image: string;
  price: number;
  categoryId: number;
  categoryName: string;
}

interface CartItem {
  item: MenuItem;
  quantity: number;
}

@Component({
  selector: 'app-cardapio',
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './cardapio.html',
  styleUrl: './cardapio.css',
})
export class Cardapio implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly orderState = inject(OrderStateService);
  private readonly cardapioService = inject(CardapioService);
  private readonly menuItems = signal<MenuItem[]>([]);
  private menuRequestSequence = 0;
  private detailsRequestSequence = 0;
  private detailsDishId: number | null = null;

  protected readonly categories = signal<CategoryOption[]>([{ id: 'todos', label: 'Todos' }]);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly detailsOpen = signal(false);
  protected readonly detailsLoading = signal(false);
  protected readonly detailsError = signal<string | null>(null);
  protected readonly selectedDish = signal<PratoDetalheResponse | null>(null);

  protected readonly isManagementView = computed(
    () => this.route.snapshot.queryParamMap.get('modo') === 'gerenciar'
  );

  protected searchTerm = '';

  protected activeCategory: CategoryId = 'todos';
  protected cart: CartItem[] = [];

  ngOnInit(): void {
    this.loadMenu('todos');
  }

  protected get filteredItems(): MenuItem[] {
    const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');
    const query = normalize(this.searchTerm.trim());
    return this.menuItems().filter(item => normalize(item.name + ' ' + item.description).includes(query));
  }

  protected get cartCount(): number {
    return this.cart.reduce((sum, cartItem) => sum + cartItem.quantity, 0);
  }

  protected get totalPrice(): number {
    return this.cart.reduce((sum, cartItem) => sum + cartItem.item.price * cartItem.quantity, 0);
  }

  protected setCategory(category: CategoryId): void {
    if (this.activeCategory === category) return;
    this.activeCategory = category;
    this.loadMenu(category);
  }

  protected retryMenu(): void {
    this.loadMenu(this.activeCategory);
  }

  protected addToCart(item: MenuItem): void {
    const existing = this.cart.find((cartItem) => cartItem.item.id === item.id);
    if (existing) {
      existing.quantity += 1;
      this.cart = [...this.cart];
      return;
    }
    this.cart = [...this.cart, { item, quantity: 1 }];
  }

  protected increaseQuantity(itemId: number): void {
    this.cart = this.cart.map((cartItem) =>
      cartItem.item.id === itemId ? { ...cartItem, quantity: cartItem.quantity + 1 } : cartItem
    );
  }

  protected decreaseQuantity(itemId: number): void {
    this.cart = this.cart
      .map((cartItem) =>
        cartItem.item.id === itemId
          ? { ...cartItem, quantity: Math.max(cartItem.quantity - 1, 0) }
          : cartItem
      )
      .filter((cartItem) => cartItem.quantity > 0);
  }

  protected removeFromCart(itemId: number): void {
    this.cart = this.cart.filter((cartItem) => cartItem.item.id !== itemId);
  }

  protected finalizeOrder(): void {
    this.orderState.setItems(
      this.cart.map((cartItem) => ({
        id: cartItem.item.id,
        name: cartItem.item.name,
        image: cartItem.item.image,
        price: cartItem.item.price,
        quantity: cartItem.quantity,
      }))
    );
    void this.router.navigateByUrl('/finalizar-pedido');
  }

  protected goToCheckout(): void {
    this.finalizeOrder();
  }

  protected goToProfile(): void {
    void this.router.navigateByUrl('/cliente/perfil');
  }

  protected goHome(): void {
    if (this.isManagementView()) {
      void this.router.navigateByUrl('/cozinha');
      return;
    }
    void this.router.navigateByUrl('/');
  }

  protected openDishDetails(id: number): void {
    this.detailsDishId = id;
    this.detailsOpen.set(true);
    this.loadDetails(id);
  }

  protected retryDetails(): void {
    if (this.detailsDishId !== null) {
      this.loadDetails(this.detailsDishId);
    }
  }

  protected closeDetails(): void {
    this.detailsRequestSequence += 1;
    this.detailsOpen.set(false);
    this.detailsLoading.set(false);
    this.detailsError.set(null);
    this.selectedDish.set(null);
    this.detailsDishId = null;
  }

  protected addDetailedDishToCart(dish: PratoDetalheResponse): void {
    this.addToCart(this.mapDish(dish));
    this.closeDetails();
  }

  protected trackByCategory(_index: number, category: CategoryOption): CategoryId {
    return category.id;
  }

  protected trackByProduct(_index: number, item: MenuItem): number {
    return item.id;
  }

  protected trackByCartItem(_index: number, cartItem: CartItem): number {
    return cartItem.item.id;
  }

  private loadMenu(category: CategoryId): void {
    const requestId = ++this.menuRequestSequence;
    this.isLoading.set(true);
    this.loadError.set(null);

    this.cardapioService
      .listar({
        categoriaId: category === 'todos' ? undefined : category,
        pagina: 0,
        tamanho: 100,
      })
      .subscribe({
        next: (response) => {
          if (requestId !== this.menuRequestSequence) return;

          this.menuItems.set(response.conteudo.map((dish) => this.mapDish(dish)));
          if (category === 'todos') {
            this.categories.set(this.extractCategories(response.conteudo));
          }
          this.isLoading.set(false);
        },
        error: () => {
          if (requestId !== this.menuRequestSequence) return;

          this.menuItems.set([]);
          this.loadError.set(
            'Não foi possível carregar o cardápio. Verifique se a API está em execução e tente novamente.'
          );
          this.isLoading.set(false);
        },
      });
  }

  private loadDetails(id: number): void {
    const requestId = ++this.detailsRequestSequence;
    this.selectedDish.set(null);
    this.detailsError.set(null);
    this.detailsLoading.set(true);

    this.cardapioService.obterPorId(id).subscribe({
      next: (dish) => {
        if (requestId !== this.detailsRequestSequence) return;

        this.selectedDish.set(dish);
        this.detailsLoading.set(false);
      },
      error: () => {
        if (requestId !== this.detailsRequestSequence) return;

        this.detailsError.set('Não foi possível carregar os detalhes deste prato.');
        this.detailsLoading.set(false);
      },
    });
  }

  private extractCategories(dishes: PratoCardapioResponse[]): CategoryOption[] {
    const categoriesById = new Map<number, string>();
    for (const dish of dishes) {
      categoriesById.set(dish.categoria.id, dish.categoria.nome);
    }

    return [
      { id: 'todos', label: 'Todos' },
      ...Array.from(categoriesById, ([id, label]) => ({ id, label })),
    ];
  }

  private mapDish(dish: PratoCardapioResponse): MenuItem {
    return {
      id: dish.id,
      name: dish.nome,
      description: dish.descricao ?? 'Descrição não informada.',
      image: dish.fotoUrl ?? '/images/prato-sem-foto.svg',
      price: dish.precoVenda,
      categoryId: dish.categoria.id,
      categoryName: dish.categoria.nome,
    };
  }
}
