import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { SessionService } from './session.service';

export const sessionGuard: CanActivateChildFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  return session.load().pipe(
    map(() => true),
    catchError((error: unknown) => {
      session.clear();
      const destination =
        error instanceof HttpErrorResponse && error.status === 401
          ? '/sesion-expirada'
          : '/error';
      return of(router.createUrlTree([destination]));
    })
  );
};
