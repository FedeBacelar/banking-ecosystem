import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { LucideDynamicIcon, LucideInfo } from '@lucide/angular';

import { NervaLogoComponent } from '../../../shared/brand/nerva-logo.component';

@Component({
  selector: 'nb-onboarding-shell',
  imports: [LucideDynamicIcon, NervaLogoComponent, RouterLink, RouterOutlet],
  template: `
    <div class="flex min-h-screen flex-col bg-canvas">
      <header class="border-b border-line bg-surface">
        <div class="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-5 sm:px-6">
          <a routerLink="/" aria-label="Nerva Banking, ir al inicio" class="rounded-lg">
            <nb-nerva-logo />
          </a>
          <a
            href="/web/auth/login/home"
            class="text-sm font-semibold text-brand underline-offset-4 hover:underline"
          >
            Ya tengo una cuenta
          </a>
        </div>
      </header>

      <aside class="border-b border-action/20 bg-action-soft" aria-label="Aviso sobre este sitio">
        <div class="mx-auto flex w-full max-w-6xl items-start gap-2.5 px-5 py-3 text-xs leading-5 text-ink sm:items-center sm:px-6">
          <svg [lucideIcon]="circleInfo" class="mt-0.5 size-4 shrink-0 text-action sm:mt-0" aria-hidden="true"></svg>
          <p>
            <strong>Proyecto académico:</strong> esta solicitud no abre una cuenta real.
            Usá únicamente datos y documentos de prueba.
          </p>
        </div>
      </aside>

      <main id="contenido-principal" class="flex flex-1 flex-col">
        <router-outlet />
      </main>

      <footer class="border-t border-line bg-surface">
        <div class="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-5 py-6 text-xs text-ink-muted sm:px-6">
          <span>Nerva Banking · Proyecto académico</span>
          <a routerLink="/" class="font-semibold text-brand underline-offset-4 hover:underline">Volver al inicio</a>
        </div>
      </footer>
    </div>
  `
})
export class OnboardingShellComponent {
  protected readonly circleInfo = LucideInfo;
}
