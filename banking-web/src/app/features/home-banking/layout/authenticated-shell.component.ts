import { DOCUMENT } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LucideDynamicIcon, LucideLogOut } from '@lucide/angular';

import { SessionService } from '../../../core/auth/session.service';
import { readCookie } from '../../../core/security/cookie';
import { NervaLogoComponent } from '../../../shared/brand/nerva-logo.component';

@Component({
  selector: 'nb-authenticated-shell',
  imports: [LucideDynamicIcon, NervaLogoComponent, RouterOutlet],
  template: `
    <div class="flex min-h-screen flex-col bg-canvas">
      <header class="border-b border-line bg-surface">
        <div
          class="mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-4 px-5 sm:px-6"
        >
          <nb-nerva-logo />

          <div class="flex min-w-0 items-center gap-3 sm:gap-5">
            <p class="hidden min-w-0 text-right leading-tight sm:block">
              <span class="block truncate text-sm font-semibold text-ink">
                {{ displayName() }}
              </span>
              <span class="mt-1 block text-xs text-ink-muted">Sesión iniciada</span>
            </p>

            <form action="/web/logout" method="post">
              <input type="hidden" name="_csrf" [value]="csrfToken" />
              <button
                type="submit"
                class="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-line bg-surface px-3 text-sm font-semibold text-brand transition-colors hover:bg-surface-subtle sm:px-4"
                aria-label="Cerrar sesión"
              >
                <svg [lucideIcon]="logOut" class="size-4" aria-hidden="true"></svg>
                <span class="hidden sm:inline">Cerrar sesión</span>
              </button>
            </form>
          </div>
        </div>
      </header>

      <main id="contenido-principal" class="flex flex-1">
        <router-outlet />
      </main>
    </div>
  `
})
export class AuthenticatedShellComponent {
  private readonly document = inject(DOCUMENT);
  private readonly session = inject(SessionService);

  protected readonly logOut = LucideLogOut;
  protected readonly csrfToken = readCookie(
    this.document.cookie,
    'NB-XSRF-TOKEN'
  );
  protected readonly displayName = computed(
    () =>
      this.session.user()?.displayName ||
      this.session.user()?.username ||
      'Tu cuenta'
  );
}
