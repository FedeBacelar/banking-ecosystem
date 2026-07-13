import {
  Component,
  ElementRef,
  afterNextRender,
  inject,
  viewChild
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideCircleCheck,
  LucideClock3,
  LucideDynamicIcon,
  LucideTriangleAlert
} from '@lucide/angular';

import { NervaLogoComponent } from '../../../../shared/brand/nerva-logo.component';

type SessionStatusKind = 'error' | 'expired' | 'closed';

const STATUS_CONTENT = {
  error: {
    icon: LucideTriangleAlert,
    iconClasses: 'bg-danger-subtle text-danger',
    title: 'No pudimos iniciar sesión',
    description:
      'No se pudo validar tu ingreso. Intentá nuevamente. Si el problema continúa, probá más tarde.',
    action: 'Intentar nuevamente'
  },
  expired: {
    icon: LucideClock3,
    iconClasses: 'bg-caution-subtle text-caution',
    title: 'Necesitás volver a ingresar',
    description:
      'Tu sesión ya no está activa. Ingresá nuevamente para continuar.',
    action: 'Ingresar nuevamente'
  },
  closed: {
    icon: LucideCircleCheck,
    iconClasses: 'bg-positive-subtle text-positive',
    title: 'Sesión cerrada',
    description:
      'Cerraste tu sesión. Para volver a usar Nerva Banking, ingresá nuevamente.',
    action: 'Ingresar nuevamente'
  }
} as const;

@Component({
  selector: 'nb-session-status-page',
  imports: [LucideDynamicIcon, NervaLogoComponent, RouterLink],
  template: `
    <div class="flex min-h-screen flex-col bg-canvas">
      <header class="border-b border-line bg-surface">
        <div class="mx-auto flex h-16 w-full max-w-6xl items-center px-5 sm:px-6">
          <a routerLink="/" aria-label="Nerva Banking, ir al inicio" class="rounded-lg">
            <nb-nerva-logo />
          </a>
        </div>
      </header>

      <main
        id="contenido-principal"
        class="flex flex-1 items-center justify-center px-5 py-12 sm:px-6"
      >
        <section
          class="w-full max-w-lg text-center sm:rounded-3xl sm:border sm:border-line sm:bg-surface sm:p-10 sm:shadow-floating"
          aria-live="polite"
          aria-labelledby="status-title"
        >
          <span
            class="mx-auto flex size-14 items-center justify-center rounded-2xl"
            [class]="content.iconClasses"
          >
            <svg [lucideIcon]="content.icon" class="size-7" aria-hidden="true"></svg>
          </span>

          <h1
            #statusTitle
            id="status-title"
            tabindex="-1"
            style="outline: none"
            class="mt-6 text-pretty text-3xl font-semibold tracking-tight text-ink"
          >
            {{ content.title }}
          </h1>
          <p class="mx-auto mt-4 max-w-md text-sm leading-6 text-ink-muted sm:text-base">
            {{ content.description }}
          </p>

          <div class="mt-8 flex flex-col items-center gap-4">
            <a
              href="/web/auth/login/home"
              class="inline-flex min-h-12 w-full items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white transition-colors hover:bg-brand-strong sm:w-auto sm:min-w-52"
            >
              {{ content.action }}
            </a>
            <a
              routerLink="/"
              class="rounded-md text-sm font-semibold text-brand underline-offset-4 hover:underline"
            >
              Volver al inicio
            </a>
          </div>
        </section>
      </main>

      <footer class="border-t border-line bg-surface px-5 py-5 text-center sm:px-6">
        <p class="mx-auto max-w-3xl text-xs leading-5 text-ink-muted">
          Proyecto académico: Nerva Banking no es una entidad financiera y no
          opera con dinero real. No ingreses datos personales, bancarios ni
          contraseñas reales.
        </p>
      </footer>
    </div>
  `
})
export class SessionStatusPage {
  private readonly route = inject(ActivatedRoute);
  private readonly statusTitle =
    viewChild.required<ElementRef<HTMLHeadingElement>>('statusTitle');

  protected readonly content = STATUS_CONTENT[this.readKind()];

  constructor() {
    afterNextRender(() => this.statusTitle().nativeElement.focus());
  }

  private readKind(): SessionStatusKind {
    const kind = this.route.snapshot.data['statusKind'];
    return kind === 'expired' || kind === 'closed' ? kind : 'error';
  }
}
