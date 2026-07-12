import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../../../core/api/onboarding-api.service';
import { OnboardingShellComponent } from '../../../../shared/ui/onboarding-shell/onboarding-shell.component';
import { httpErrorMessage } from '../../utils/http-error-message';

@Component({
  selector: 'app-onboarding-continue-page',
  imports: [RouterLink, OnboardingShellComponent],
  templateUrl: './onboarding-continue.page.html'
})
export class OnboardingContinuePage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly onboardingApi = inject(OnboardingApiService);

  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    const token = new URLSearchParams(this.route.snapshot.fragment ?? '').get('token');
    window.history.replaceState(null, '', window.location.pathname + window.location.search);

    if (!token) {
      this.isLoading.set(false);
      this.errorMessage.set('El enlace no es válido o está incompleto.');
      return;
    }

    this.onboardingApi
      .consumeMagicLink(token)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (access) => {
          const destination = access.nextAction === 'CONTINUE_APPLICATION'
            ? '/onboarding/applicant-data'
            : '/onboarding/status';
          void this.router.navigate([destination]);
        },
        error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error))
      });
  }
}
