import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import type { PerfilUsuario } from './services/auth.service';

const guardedBy = (roles: PerfilUsuario[]) => ({
  canActivate: [authGuard, roleGuard],
  data: { roles },
});

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home').then((m) => m.Home),
  },
  {
    path: 'home',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: 'cardapio',
    loadComponent: () => import('./cardapio/cardapio').then((m) => m.Cardapio),
  },
  {
    path: 'empresa-login',
    loadComponent: () =>
      import('./empresa-login/empresa-login').then((m) => m.EmpresaLogin),
  },
  {
    path: 'admin',
    ...guardedBy(['ADMIN', 'GERENTE']),
    loadComponent: () =>
      import('./admin-dashboard/admin-dashboard').then((m) => m.AdminDashboard),
  },
  {
    path: 'admin/estoque',
    ...guardedBy(['ADMIN', 'GERENTE']),
    loadComponent: () =>
      import('./admin-inventory/admin-inventory').then((m) => m.AdminInventory),
  },
  {
    path: 'admin/pedidos',
    ...guardedBy(['ADMIN', 'GERENTE']),
    loadComponent: () =>
      import('./admin-orders/admin-orders').then((m) => m.AdminOrders),
  },
  {
    path: 'admin/produtos',
    ...guardedBy(['ADMIN']),
    loadComponent: () =>
      import('./admin-products/admin-products').then((m) => m.AdminProducts),
  },
  {
    path: 'admin/clientes',
    ...guardedBy(['ADMIN']),
    loadComponent: () =>
      import('./client_Area/client-area').then((m) => m.AdminCustomers),
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login').then((m) => m.Login),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'finalizar-pedido',
    ...guardedBy(['CLIENTE']),
    loadComponent: () =>
      import('./finalizar-pedido/finalizar-pedido').then((m) => m.FinalizarPedido),
  },
  {
    path: 'cliente/perfil',
    ...guardedBy(['CLIENTE']),
    loadComponent: () =>
      import('./cliente-perfil/cliente-perfil').then((m) => m.ClientePerfil),
  },
  {
    path: 'motoboy',
    ...guardedBy(['ADMIN']),
    loadComponent: () => import('./motoboy/motoboy').then((m) => m.Motoboy),
  },
  {
    path: 'cozinha',
    ...guardedBy(['ADMIN', 'COZINHEIRO']),
    loadComponent: () => import('./cozinha/cozinha').then((m) => m.Cozinha),
  },
  {
    path: 'acompanhar-pedido',
    ...guardedBy(['CLIENTE']),
    loadComponent: () =>
      import('./acompanhar-pedido/acompanhar-pedido').then(
        (m) => m.AcompanharPedido
      ),
  },
  {
    path: 'acompanhar-pedido/:id',
    ...guardedBy(['CLIENTE']),
    loadComponent: () =>
      import('./acompanhar-pedido/acompanhar-pedido').then(
        (m) => m.AcompanharPedido
      ),
  },
  {
    path: 'acesso-negado',
    loadComponent: () =>
      import('./acesso-negado/acesso-negado').then((m) => m.AcessoNegado),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
