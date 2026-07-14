import { Routes } from '@angular/router';

import { OnboardingCompletionStore } from './data-access/onboarding-completion.store';
import { OnboardingDraftStore } from './data-access/onboarding-draft.store';
import { OnboardingStatusStore } from './data-access/onboarding-status.store';
import { pendingOnboardingChangesGuard } from './data-access/pending-onboarding-changes.guard';
import { OnboardingShellComponent } from './layout/onboarding-shell.component';

const loadContinuePage = () =>
  import('./pages/continue/onboarding-continue.page').then(
    (module) => module.OnboardingContinuePage
  );

export const onboardingRoutes: Routes = [
  {
    path: 'finalizando',
    component: OnboardingShellComponent,
    data: { hideExistingAccountLogin: true },
    providers: [OnboardingCompletionStore],
    children: [
      {
        path: '',
        title: 'Terminando de preparar tu cuenta | Nerva Banking',
        loadComponent: () =>
          import('./pages/completion/onboarding-completion.page').then(
            (module) => module.OnboardingCompletionPage
          )
      }
    ]
  },
  {
    path: 'credentials-complete',
    component: OnboardingShellComponent,
    data: { hideExistingAccountLogin: true },
    children: [
      {
        path: '',
        title: 'Continuar | Nerva Banking',
        loadComponent: () =>
          import('./pages/completion-entry/onboarding-completion-entry.page').then(
            (module) => module.OnboardingCompletionEntryPage
          )
      }
    ]
  },
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
        path: 'estado',
        title: 'Estado de tu solicitud | Nerva Banking',
        providers: [OnboardingStatusStore],
        loadComponent: () =>
          import('./pages/status/onboarding-status.page').then(
            (module) => module.OnboardingStatusPage
          )
      },
      { path: 'solicitud-enviada', pathMatch: 'full', redirectTo: 'estado' },
      { path: '**', redirectTo: '' }
    ]
  }
];
