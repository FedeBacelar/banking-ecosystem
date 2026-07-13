import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { LucideDynamicIcon, LucideInfo } from '@lucide/angular';

import { NervaLogoComponent } from '../brand/nerva-logo.component';

@Component({
  selector: 'nb-public-shell',
  imports: [LucideDynamicIcon, NervaLogoComponent, RouterLink, RouterOutlet],
  template: `
    <div class="flex min-h-screen flex-col bg-canvas">
      <header
        class="sticky top-0 z-40 border-b border-line/80 bg-surface/95 backdrop-blur"
      >
        <div
          class="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-5 sm:px-6"
        >
          <a
            routerLink="/"
            aria-label="Nerva Banking, ir al inicio"
            class="rounded-lg"
          >
            <nb-nerva-logo />
          </a>

          <a
            href="/web/auth/login/home"
            class="inline-flex min-h-10 items-center justify-center rounded-lg bg-brand px-4 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-brand-strong"
          >
            Ingresar
          </a>
        </div>
      </header>

      <aside class="border-b border-action/20 bg-action-soft" aria-label="Aviso sobre este sitio">
        <div
          class="mx-auto flex w-full max-w-6xl items-start gap-2.5 px-5 py-3 text-xs leading-5 text-ink sm:items-center sm:px-6"
        >
          <svg
            [lucideIcon]="circleInfo"
            class="mt-0.5 size-4 shrink-0 text-action sm:mt-0"
            aria-hidden="true"
          ></svg>
          <p>
            <strong>Proyecto académico:</strong> Nerva Banking no es una entidad
            financiera y no opera con dinero real. No ingreses datos personales,
            bancarios ni contraseñas reales.
          </p>
        </div>
      </aside>

      <main id="contenido-principal" class="flex flex-1 flex-col">
        <router-outlet />
      </main>

      <footer class="border-t border-line bg-surface">
        <div
          class="mx-auto flex w-full max-w-6xl flex-col gap-5 px-5 py-8 sm:flex-row sm:items-end sm:justify-between sm:px-6"
        >
          <div>
            <nb-nerva-logo />
            <p class="mt-3 max-w-sm text-sm text-ink-muted">
              Nerva Banking · Proyecto académico
            </p>
          </div>
          <p class="text-xs text-ink-muted">
            © {{ year }} Nerva Banking
          </p>
        </div>
      </footer>
    </div>
  `
})
export class PublicShellComponent {
  protected readonly circleInfo = LucideInfo;
  protected readonly year = new Date().getFullYear();
}
