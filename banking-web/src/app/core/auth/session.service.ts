import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { SessionUser } from './session.models';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<SessionUser | null>(null);

  readonly user = this.currentUser.asReadonly();

  load(): Observable<SessionUser> {
    return this.http
      .get<SessionUser>('/web/me')
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  clear(): void {
    this.currentUser.set(null);
  }
}
