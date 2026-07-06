import { Routes } from '@angular/router';

export const onboardingRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'start'
  },
  {
    path: 'start',
    loadComponent: () =>
      import('./pages/onboarding-start/onboarding-start.page').then((m) => m.OnboardingStartPage)
  },
  {
    path: 'check-email',
    loadComponent: () =>
      import('./pages/onboarding-check-email/onboarding-check-email.page').then(
        (m) => m.OnboardingCheckEmailPage
      )
  },
  {
    path: 'continue',
    loadComponent: () =>
      import('./pages/onboarding-continue/onboarding-continue.page').then(
        (m) => m.OnboardingContinuePage
      )
  },
  {
    path: 'session',
    loadComponent: () =>
      import('./pages/onboarding-session/onboarding-session.page').then((m) => m.OnboardingSessionPage)
  }
];
