import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { email, form, FormField, maxLength, required } from '@angular/forms/signals';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../data-access/onboarding-api.service';
import { OnboardingDraftStore } from '../../data-access/onboarding-draft.store';

@Component({
  selector: 'nb-onboarding-start-page',
  imports: [FormField],
  host: { class: 'flex flex-1' },
  template: `
    <section class="flex w-full flex-1 items-center py-10 sm:py-16">
      <div class="mx-auto w-full max-w-lg px-5 sm:px-6">
        <div class="rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-8">
          <p class="text-xs font-semibold uppercase tracking-[0.16em] text-action">Abrir una cuenta</p>
          <h1 class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink" tabindex="-1">
            Empezá tu solicitud
          </h1>
          <p class="mt-3 text-base leading-7 text-ink-muted">
            Ingresá tu correo y te enviamos un enlace para continuar.
          </p>

          <form class="mt-8" novalidate (submit)="submit($event)">
            <label for="onboarding-email" class="block text-sm font-semibold text-ink">
              Correo electrónico
            </label>
            <input
              id="onboarding-email"
              type="email"
              inputmode="email"
              autocomplete="email"
              [formField]="emailForm.email"
              [attr.aria-describedby]="emailForm.email().invalid() && emailForm.email().touched() ? 'email-error' : null"
              class="mt-2 min-h-12 w-full rounded-xl border border-line bg-surface px-4 text-ink shadow-sm outline-none transition focus:border-action"
            />
            @if (emailForm.email().invalid() && emailForm.email().touched()) {
              <p id="email-error" class="mt-2 text-sm font-medium text-danger">
                Ingresá un correo válido.
              </p>
            }

            @if (errorMessage()) {
              <p class="mt-4 rounded-xl bg-danger-subtle px-4 py-3 text-sm text-danger" role="alert">
                {{ errorMessage() }}
              </p>
            }

            <button
              type="submit"
              [disabled]="isSubmitting()"
              [attr.aria-busy]="isSubmitting()"
              class="mt-6 inline-flex min-h-12 w-full items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-brand-strong disabled:cursor-wait disabled:opacity-70"
            >
              @if (isSubmitting()) {
                <span class="nb-spinner mr-2" aria-hidden="true"></span>
                Enviando…
              } @else {
                Enviar enlace
              }
            </button>
          </form>
        </div>
      </div>
    </section>
  `
})
export class OnboardingStartPage {
  private readonly api = inject(OnboardingApiService);
  private readonly router = inject(Router);
  private readonly store = inject(OnboardingDraftStore);

  protected readonly emailModel = signal({ email: '' });
  protected readonly emailForm = form(this.emailModel, (path) => {
    required(path.email);
    email(path.email);
    maxLength(path.email, 255);
  });
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected submit(event: SubmitEvent): void {
    event.preventDefault();
    this.emailForm.email().markAsTouched();
    this.errorMessage.set(null);
    if (this.emailForm.email().invalid()) {
      this.emailForm.email().focusBoundControl();
      return;
    }

    const submittedEmail = this.emailModel().email.trim();
    this.isSubmitting.set(true);
    this.api
      .startApplication(submittedEmail)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.store.email.set(submittedEmail);
          this.emailModel.set({ email: '' });
          void this.router.navigate(['/onboarding/correo-enviado']);
        },
        error: () => {
          this.errorMessage.set('No pudimos enviar el enlace. Intentá nuevamente.');
        }
      });
  }
}
