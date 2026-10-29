import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token();
  const isAuthenticationRequest = /\/auth\/(login|register)$/.test(
    request.url.split('?')[0]
  );
  const authenticatedRequest =
    token && !isAuthenticationRequest
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !isAuthenticationRequest
      ) {
        const returnUrl = router.url;
        authService.logout();
        void router.navigate(['/login'], {
          queryParams:
            returnUrl && returnUrl !== '/login' ? { returnUrl } : undefined,
        });
      }
      return throwError(() => error);
    })
  );
};
