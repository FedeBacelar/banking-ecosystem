import { Component, input } from '@angular/core';

@Component({
  selector: 'app-onboarding-shell',
  imports: [],
  templateUrl: './onboarding-shell.component.html',
  styleUrl: './onboarding-shell.component.scss'
})
export class OnboardingShellComponent {
  readonly eyebrow = input('Nerva Banking');
  readonly title = input.required<string>();
  readonly subtitle = input.required<string>();
}
