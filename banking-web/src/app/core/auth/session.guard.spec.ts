import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter
} from '@angular/router';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';

import { sessionGuard } from './session.guard';
import { SessionService } from './session.service';

describe('sessionGuard', () => {
  const session = {
    load: vi.fn(),
    clear: vi.fn()
  };

  beforeEach(() => {
    session.load.mockReset();
    session.clear.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: session }
      ]
    });
  });

  it('allows access after loading the session', async () => {
    session.load.mockReturnValue(
      of({ username: 'cliente', displayName: 'Cliente Nerva' })
    );

    const result = await runGuard();

    expect(result).toBe(true);
  });

  it('redirects an anonymous session to the expired-session page', async () => {
    session.load.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401 }))
    );

    const result = await runGuard();

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe(
      '/sesion-expirada'
    );
    expect(session.clear).toHaveBeenCalledOnce();
  });

  it('redirects unexpected failures to the neutral error page', async () => {
    session.load.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 503 }))
    );

    const result = await runGuard();

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/error');
  });

  function runGuard(): Promise<boolean | UrlTree> {
    const result = TestBed.runInInjectionContext(() =>
      sessionGuard(
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot
      )
    );
    return firstValueFrom(result as Observable<boolean | UrlTree>);
  }
});
