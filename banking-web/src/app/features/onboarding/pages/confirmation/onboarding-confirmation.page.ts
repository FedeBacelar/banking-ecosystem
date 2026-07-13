import { Component } from '@angular/core';
import { LucideCheck, LucideDynamicIcon } from '@lucide/angular';

@Component({
  selector: 'nb-onboarding-confirmation-page',
  imports: [LucideDynamicIcon],
  host: { class: 'flex flex-1' },
  template: `
    <section class="flex w-full flex-1 items-center py-10 sm:py-16">
      <div class="mx-auto w-full max-w-lg px-5 sm:px-6">
        <div class="rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8">
          <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-positive-subtle text-positive">
            <svg [lucideIcon]="check" class="size-6" aria-hidden="true"></svg>
          </span>
          <p class="mt-5 text-xs font-semibold uppercase tracking-[0.16em] text-positive">Solicitud enviada</p>
          <h1 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink">Recibimos tu solicitud</h1>
          <p class="mt-3 text-base leading-7 text-ink-muted">La información se envió correctamente.</p>
        </div>
      </div>
    </section>
  `
})
export class OnboardingConfirmationPage {
  protected readonly check = LucideCheck;
}
