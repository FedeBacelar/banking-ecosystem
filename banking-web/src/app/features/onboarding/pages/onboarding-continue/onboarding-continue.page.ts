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
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.isLoading.set(false);
      this.errorMessage.set('El enlace no contiene un token válido.');
      return;
    }

    this.onboardingApi
      .consumeMagicLink(token)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/onboarding/session']),
        error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error))
      });
  }
}
