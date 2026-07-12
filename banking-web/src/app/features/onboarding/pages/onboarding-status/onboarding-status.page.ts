import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../../../core/api/onboarding-api.service';
import { OnboardingShellComponent } from '../../../../shared/ui/onboarding-shell/onboarding-shell.component';
import { OnboardingStatus } from '../../models/onboarding.models';
import { httpErrorMessage } from '../../utils/http-error-message';

@Component({
  selector: 'app-onboarding-status-page',
  imports: [RouterLink, OnboardingShellComponent],
  templateUrl: './onboarding-status.page.html'
})
export class OnboardingStatusPage {
  private readonly api = inject(OnboardingApiService);
  readonly status = signal<OnboardingStatus | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isResending = signal(false);

  constructor() {
    this.loadStatus();
  }

  loadStatus(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.api.getStatus().pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (status) => this.status.set(status),
      error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error))
    });
  }

  resendInvitation(): void {
    this.isResending.set(true);
    this.errorMessage.set(null);
    this.api.resendCredentialInvitation()
      .pipe(finalize(() => this.isResending.set(false)))
      .subscribe({ error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error)) });
  }
}
