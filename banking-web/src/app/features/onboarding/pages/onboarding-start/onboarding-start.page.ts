import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../../../core/api/onboarding-api.service';
import { OnboardingShellComponent } from '../../../../shared/ui/onboarding-shell/onboarding-shell.component';
import { httpErrorMessage } from '../../utils/http-error-message';

@Component({
  selector: 'app-onboarding-start-page',
  imports: [ReactiveFormsModule, OnboardingShellComponent],
  templateUrl: './onboarding-start.page.html'
})
export class OnboardingStartPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly onboardingApi = inject(OnboardingApiService);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const email = this.form.controls.email.value.trim().toLowerCase();
    this.isSubmitting.set(true);

    this.onboardingApi
      .startApplication(email)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (application) => {
          void this.router.navigate(['/onboarding/check-email'], {
            state: {
              email: application.email,
              magicLinkExpiresAt: application.magicLinkExpiresAt
            }
          });
        },
        error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error))
      });
  }

  get showEmailError(): boolean {
    const email = this.form.controls.email;
    return email.invalid && (email.touched || email.dirty);
  }
}
