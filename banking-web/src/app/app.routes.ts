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
