import { HttpErrorResponse } from '@angular/common/http';
import { defer, NEVER, of, throwError } from 'rxjs';
import { recoverRead } from './api-recovery';

describe('Leituras durante indisponibilidade', () => {
  afterEach(() => vi.useRealTimers());
  it('repete 503 e recupera uma leitura real sem fabricar dados', () => {
    vi.useFakeTimers();
    let calls = 0;
    const values: unknown[] = [];
    defer(() =>
      ++calls === 1
        ? throwError(() => new HttpErrorResponse({ status: 503 }))
        : of({ conteudo: [] }),
    )
      .pipe(recoverRead())
      .subscribe((value) => values.push(value));
    vi.advanceTimersByTime(5_000);
    expect(calls).toBe(2);
    expect(values).toEqual([{ conteudo: [] }]);
  });
  it('não repete acesso recusado', () => {
    let calls = 0;
    const error = vi.fn();
    defer(() => {
      calls++;
      return throwError(() => new HttpErrorResponse({ status: 403 }));
    })
      .pipe(recoverRead())
      .subscribe({ error });
    expect(calls).toBe(1);
    expect(error).toHaveBeenCalledOnce();
  });
  it('encerra após 12 tentativas mesmo quando cada leitura nunca responde', () => {
    vi.useFakeTimers();
    const attempts: number[] = [];
    const error = vi.fn();
    NEVER.pipe(recoverRead((attempt) => attempts.push(attempt))).subscribe({ error });
    vi.advanceTimersByTime(180_000);
    expect(attempts).toHaveLength(12);
    expect(error).toHaveBeenCalledOnce();
  });
  it('cancelamento interrompe novas tentativas', () => {
    vi.useFakeTimers();
    const attempt = vi.fn();
    const subscription = NEVER.pipe(recoverRead(attempt)).subscribe();
    subscription.unsubscribe();
    vi.advanceTimersByTime(180_000);
    expect(attempt).toHaveBeenCalledOnce();
  });
});
