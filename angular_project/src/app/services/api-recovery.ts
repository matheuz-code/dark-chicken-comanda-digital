import { HttpErrorResponse } from '@angular/common/http';
import {
  defer,
  MonoTypeOperatorFunction,
  retry,
  throwError,
  timeout,
  TimeoutError,
  timer,
} from 'rxjs';

/** Somente para leituras idempotentes. Nunca aplicar a criação/alteração de pedidos. */
export function recoverRead<T>(
  onAttempt: (attempt: number) => void = () => {},
): MonoTypeOperatorFunction<T> {
  return (source) =>
    defer(() => {
      let attempt = 0;
      return defer(() => {
        onAttempt(++attempt);
        return source.pipe(timeout(10_000));
      }).pipe(
        retry({
          count: 11,
          delay: (error) => (transientReadError(error) ? timer(5_000) : throwError(() => error)),
        }),
      );
    });
}

export function transientReadError(error: unknown): boolean {
  return (
    error instanceof TimeoutError ||
    (error instanceof HttpErrorResponse && [0, 502, 503, 504].includes(error.status))
  );
}

export function readFailureMessage(error: unknown): string {
  if (transientReadError(error)) {
    return 'A API não ficou disponível no prazo de até 3 minutos. O servidor ou o banco podem estar indisponíveis. Tente novamente mais tarde.';
  }
  return 'A API retornou uma resposta inválida ou recusou a consulta. Tentar novamente não corrige automaticamente esse problema.';
}
