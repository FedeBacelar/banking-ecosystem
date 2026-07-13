import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'app',
    loadChildren: () =>
      import('./features/home-banking/home-banking.routes').then(
        (module) => module.homeBankingRoutes
      )
  },
  {
    path: 'onboarding',
    loadChildren: () =>
      import('./features/onboarding/onboarding.routes').then(
        (module) => module.onboardingRoutes
      )
  },
  {
    path: 'legales',
    loadChildren: () =>
      import('./features/legal/legal.routes').then(
        (module) => module.legalRoutes
      )
  },
  {
    path: '',
    loadChildren: () =>
      import('./features/public/public.routes').then(
        (module) => module.publicRoutes
      )
  },
  {
    path: '**',
    redirectTo: ''
  }
];
