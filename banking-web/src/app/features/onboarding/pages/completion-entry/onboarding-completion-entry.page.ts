import { DOCUMENT } from '@angular/common';
import { Component, InjectionToken, inject } from '@angular/core';

export const ONBOARDING_COMPLETION_LOGIN_URL =
  '/web/auth/login/onboarding-completion';

export interface OnboardingCompletionNavigator {
  replace(url: string): void;
}

export const ONBOARDING_COMPLETION_NAVIGATOR =
  new InjectionToken<OnboardingCompletionNavigator>(
    'ONBOARDING_COMPLETION_NAVIGATOR',
    {
      providedIn: 'root',
      factory: () => {
        const document = inject(DOCUMENT);
        return {
          replace: (url: string) => document.defaultView?.location.replace(url)
        };
      }
    }
  );

@Component({
  selector: 'nb-onboarding-completion-entry-page',
  host: { class: 'flex flex-1' },
  template: `
    <section class="flex w-full flex-1 items-center py-10 sm:py-16" aria-live="polite">
      <div class="mx-auto w-full max-w-lg px-5 text-center sm:px-6">
        <span class="nb-spinner mx-auto block text-action" aria-hidden="true"></span>
        <h1 class="mt-5 text-2xl font-semibold tracking-[-0.02em] text-ink">
          Continuando con tu acceso…
        </h1>
        <p class="mt-3 text-sm leading-6 text-ink-muted">
          Si la pantalla no cambia,
          <a
            [href]="loginUrl"
            class="font-semibold text-brand underline underline-offset-4"
          >continuá desde acá</a>.
        </p>
      </div>
    </section>
  `
})
export class OnboardingCompletionEntryPage {
  private readonly navigator = inject(ONBOARDING_COMPLETION_NAVIGATOR);

  protected readonly loginUrl = ONBOARDING_COMPLETION_LOGIN_URL;

  constructor() {
    this.navigator.replace(ONBOARDING_COMPLETION_LOGIN_URL);
  }
}
