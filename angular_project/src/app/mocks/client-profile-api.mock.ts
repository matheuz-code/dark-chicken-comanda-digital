import { ClientProfileDto } from '../models/client-profile.model';

export const CLIENT_PROFILE_API_MOCK: ClientProfileDto = {
  id: 'client-1',
  name: 'Cliente Demonstração',
  email: 'cliente.demo@example.com',
  orders: [
    {
      id: 'ORD-1776872471933',
      createdAtIso: '2026-04-22T12:41:00.000Z',
      total: 25.9,
      status: 'aguardando',
      verificationCode: '8939',
      verificationCodeExpiresAtIso: '2026-04-22T14:41:00.000Z',
      items: [
        {
          id: 1,
          name: 'Burger Classico',
          quantity: 1,
          price: 25.9,
          image:
            'https://images.unsplash.com/photo-1561758033-d89a9ad46330?auto=format&fit=crop&w=900&q=80',
        },
      ],
    },
  ],
};
