export type OrderStatus = 'aguardando' | 'em_preparo' | 'pronto' | 'entregue' | 'cancelado';

export interface ClientProfileDto {
  id: string;
  name: string;
  email: string;
  orders: ClientOrderDto[];
}

export interface ClientOrderDto {
  id: string;
  createdAtIso: string;
  total: number;
  status: OrderStatus;
  items: ClientOrderItemDto[];
  verificationCode?: string;
  verificationCodeExpiresAtIso?: string;
}

export interface ClientOrderItemDto {
  id: number;
  name: string;
  quantity: number;
  price: number;
  image: string;
}

export interface ClientOrder extends Omit<ClientOrderDto, 'createdAtIso'> {
  createdAt: Date;
}
