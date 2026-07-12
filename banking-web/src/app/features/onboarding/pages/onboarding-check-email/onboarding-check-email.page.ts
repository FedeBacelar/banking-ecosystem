import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { OnboardingShellComponent } from '../../../../shared/ui/onboarding-shell/onboarding-shell.component';

interface CheckEmailState {
  email?: string;
  magicLinkExpiresAt?: string;
}

@Component({
  selector: 'app-onboarding-check-email-page',
  imports: [RouterLink, OnboardingShellComponent],
  templateUrl: './onboarding-check-email.page.html'
})
export class OnboardingCheckEmailPage {
  private readonly state = history.state as CheckEmailState;

  readonly email = this.state.email ?? null;
  readonly magicLinkExpiresAt = this.state.magicLinkExpiresAt ?? null;
}
