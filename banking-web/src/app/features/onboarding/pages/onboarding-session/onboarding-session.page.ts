import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../../../core/api/onboarding-api.service';
import { OnboardingShellComponent } from '../../../../shared/ui/onboarding-shell/onboarding-shell.component';
import { OnboardingSession } from '../../models/onboarding.models';
import { httpErrorMessage } from '../../utils/http-error-message';

@Component({
  selector: 'app-onboarding-session-page',
  imports: [RouterLink, OnboardingShellComponent],
  templateUrl: './onboarding-session.page.html'
})
export class OnboardingSessionPage {
  private readonly onboardingApi = inject(OnboardingApiService);
  private readonly router = inject(Router);

  readonly isLoading = signal(true);
  readonly isClearing = signal(false);
  readonly session = signal<OnboardingSession | null>(null);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.loadSession();
  }

  loadSession(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.onboardingApi
      .getSession()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (session) => this.session.set(session),
        error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error))
      });
  }

  clearSession(): void {
    this.isClearing.set(true);

    this.onboardingApi
      .clearSession()
      .pipe(finalize(() => this.isClearing.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/onboarding/start']),
        error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error))
      });
  }
}
