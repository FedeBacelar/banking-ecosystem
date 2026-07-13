import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideDynamicIcon, LucideMail } from '@lucide/angular';

import { OnboardingDraftStore } from '../../data-access/onboarding-draft.store';

@Component({
  selector: 'nb-onboarding-check-email-page',
  imports: [LucideDynamicIcon, RouterLink],
  host: { class: 'flex flex-1' },
  template: `
    <section class="flex w-full flex-1 items-center py-10 sm:py-16">
      <div class="mx-auto w-full max-w-lg px-5 sm:px-6">
        <div class="rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8">
          <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-action-soft text-action">
            <svg [lucideIcon]="mail" class="size-6" aria-hidden="true"></svg>
          </span>
          <h1 class="mt-5 text-3xl font-semibold tracking-[-0.03em] text-ink" tabindex="-1">
            Revisá tu correo
          </h1>
          <p class="mt-3 text-base leading-7 text-ink-muted">
            Buscá un mensaje de Nerva Banking con el enlace para continuar.
          </p>
          @if (maskedEmail()) {
            <p class="mt-2 font-semibold text-ink">{{ maskedEmail() }}</p>
          }
          <p class="mt-5 rounded-xl bg-surface-subtle px-4 py-3 text-sm text-ink-muted">
            Si no lo encontrás, revisá la carpeta de correo no deseado.
          </p>
          <a
            routerLink="/onboarding"
            class="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl px-4 text-sm font-semibold text-brand underline-offset-4 hover:underline"
          >
            Usar otro correo
          </a>
        </div>
      </div>
    </section>
  `
})
export class OnboardingCheckEmailPage {
  private readonly store = inject(OnboardingDraftStore);
  protected readonly mail = LucideMail;

  protected maskedEmail(): string | null {
    const email = this.store.email();
    if (!email) {
      return null;
    }
    const [local = '', domain = ''] = email.split('@');
    const firstCharacter = local.slice(0, 1);
    return `${firstCharacter}${'*'.repeat(Math.max(3, local.length - 1))}@${domain}`;
  }
}
