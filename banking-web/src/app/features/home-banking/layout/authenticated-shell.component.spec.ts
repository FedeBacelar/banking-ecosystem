import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SessionService } from '../../../core/auth/session.service';
import { AuthenticatedShellComponent } from './authenticated-shell.component';

describe('AuthenticatedShellComponent', () => {
  beforeEach(() => {
    document.cookie = 'NB-XSRF-TOKEN=csrf%2Btoken; path=/';
  });

  afterEach(() => {
    document.cookie = 'NB-XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('posts logout through a top-level form with the materialized CSRF token', async () => {
    const user = signal({
      username: 'cliente',
      displayName: 'Cliente Nerva'
    });

    await TestBed.configureTestingModule({
      imports: [AuthenticatedShellComponent],
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: { user: user.asReadonly() } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(AuthenticatedShellComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const form = element.querySelector<HTMLFormElement>('form');
    const csrf = element.querySelector<HTMLInputElement>('input[name="_csrf"]');

    expect(form?.getAttribute('action')).toBe('/web/logout');
    expect(form?.getAttribute('method')).toBe('post');
    expect(csrf?.value).toBe('csrf+token');
    expect(element.textContent).toContain('Cliente Nerva');
  });
});
