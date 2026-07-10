import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OnboardingShellComponent } from '../../../../shared/ui/onboarding-shell/onboarding-shell.component';

@Component({
  selector: 'app-onboarding-credentials-complete-page',
  imports: [RouterLink, OnboardingShellComponent],
  templateUrl: './onboarding-credentials-complete.page.html'
})
export class OnboardingCredentialsCompletePage {}
