import { Routes } from '@angular/router';

import { OnboardingDraftStore } from './data-access/onboarding-draft.store';
import { pendingOnboardingChangesGuard } from './data-access/pending-onboarding-changes.guard';
import { OnboardingShellComponent } from './layout/onboarding-shell.component';

const loadContinuePage = () =>
  import('./pages/continue/onboarding-continue.page').then(
    (module) => module.OnboardingContinuePage
  );

export const onboardingRoutes: Routes = [
  {
    path: '',
    component: OnboardingShellComponent,
    providers: [OnboardingDraftStore],
    children: [
      {
        path: '',
        title: 'Empezá tu solicitud | Nerva Banking',
        loadComponent: () =>
          import('./pages/start/onboarding-start.page').then(
            (module) => module.OnboardingStartPage
          )
      },
      { path: 'inicio', pathMatch: 'full', redirectTo: '' },
      { path: 'start', pathMatch: 'full', redirectTo: '' },
      {
        path: 'correo-enviado',
        title: 'Revisá tu correo | Nerva Banking',
        loadComponent: () =>
          import('./pages/check-email/onboarding-check-email.page').then(
            (module) => module.OnboardingCheckEmailPage
          )
      },
      {
        path: 'continuar',
        title: 'Continuar solicitud | Nerva Banking',
        loadComponent: loadContinuePage
      },
      {
        path: 'continue',
        title: 'Continuar solicitud | Nerva Banking',
        loadComponent: loadContinuePage
      },
      {
        path: 'solicitud',
        title: 'Tu solicitud | Nerva Banking',
        canDeactivate: [pendingOnboardingChangesGuard],
        loadComponent: () =>
          import('./pages/application/onboarding-application.page').then(
            (module) => module.OnboardingApplicationPage
          )
      },
      {
        path: 'solicitud-enviada',
        title: 'Solicitud enviada | Nerva Banking',
        loadComponent: () =>
          import('./pages/confirmation/onboarding-confirmation.page').then(
            (module) => module.OnboardingConfirmationPage
          )
      },
      { path: '**', redirectTo: '' }
    ]
  }
];
