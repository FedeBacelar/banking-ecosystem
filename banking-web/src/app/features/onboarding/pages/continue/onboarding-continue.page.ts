import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideCircleAlert, LucideDynamicIcon } from '@lucide/angular';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../data-access/onboarding-api.service';
import { OnboardingDraftStore } from '../../data-access/onboarding-draft.store';
import { MagicLinkError, magicLinkError } from '../../data-access/onboarding-error';

const ERROR_COPY: Record<MagicLinkError, string> = {
  invalid: 'Este enlace no es válido.',
  expired: 'Este enlace ya venció.',
  used: 'Este enlace ya fue usado.',
  unavailable: 'No pudimos validar el enlace. Intentá nuevamente.'
};

@Component({
  selector: 'nb-onboarding-continue-page',
  imports: [LucideDynamicIcon, RouterLink],
  host: { class: 'flex flex-1' },
  template: `
    <section class="flex w-full flex-1 items-center py-10 sm:py-16">
      <div class="mx-auto w-full max-w-lg px-5 sm:px-6">
        <div class="rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8" aria-live="polite">
          @if (isLoading()) {
            <span class="nb-spinner mx-auto block text-action" aria-hidden="true"></span>
            <h1 class="mt-5 text-2xl font-semibold text-ink">Estamos validando tu enlace</h1>
            <p class="mt-2 text-sm text-ink-muted">Esto puede demorar unos segundos.</p>
          } @else if (errorKind()) {
            <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-danger-subtle text-danger">
              <svg [lucideIcon]="circleAlert" class="size-6" aria-hidden="true"></svg>
            </span>
            <h1 class="mt-5 text-2xl font-semibold text-ink">No pudimos continuar</h1>
            <p class="mt-3 text-base text-ink-muted">{{ errorCopy() }}</p>

            @if (errorKind() === 'unavailable' && token) {
              <button
                type="button"
                (click)="consume()"
                class="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white hover:bg-brand-strong"
              >
                Intentar nuevamente
              </button>
            } @else {
              <a
                routerLink="/onboarding"
                class="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white hover:bg-brand-strong"
              >
                Solicitar un nuevo enlace
              </a>
            }
          }
        </div>
      </div>
    </section>
  `
})
export class OnboardingContinuePage {
  private readonly api = inject(OnboardingApiService);
  private readonly router = inject(Router);
  private readonly store = inject(OnboardingDraftStore);

  protected readonly circleAlert = LucideCircleAlert;
  protected readonly isLoading = signal(true);
  protected readonly errorKind = signal<MagicLinkError | null>(null);
  protected token: string | null;

  constructor() {
    this.token = new URLSearchParams(window.location.hash.slice(1)).get('token');
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
    if (!this.token) {
      this.isLoading.set(false);
      this.errorKind.set('invalid');
      return;
    }
    this.consume();
  }

  protected errorCopy(): string {
    return ERROR_COPY[this.errorKind() ?? 'unavailable'];
  }

  protected consume(): void {
    if (!this.token) {
      return;
    }
    this.isLoading.set(true);
    this.errorKind.set(null);
    this.api
      .consumeMagicLink(this.token)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (access) => {
          this.token = null;
          if (access.nextAction === 'CONTINUE_APPLICATION') {
            this.store.accessGranted.set(true);
            void this.router.navigate(['/onboarding/solicitud']);
            return;
          }
          void this.router.navigate(['/onboarding/solicitud-enviada']);
        },
        error: (error: unknown) => {
          const kind = magicLinkError(error);
          if (kind !== 'unavailable') {
            this.token = null;
          }
          this.errorKind.set(kind);
        }
      });
  }
}
