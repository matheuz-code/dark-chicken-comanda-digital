import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, PerfilUsuario } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const perfisPermitidos = (route.data['roles'] ?? []) as PerfilUsuario[];

  if (!authService.estaAutenticado()) {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url },
    });
  }

  return perfisPermitidos.length === 0 || authService.temAlgumPerfil(perfisPermitidos)
    ? true
    : router.createUrlTree(['/acesso-negado'], {
        queryParams: { returnUrl: state.url },
      });
};
