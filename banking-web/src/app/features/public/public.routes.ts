import { Routes } from '@angular/router';

import { PublicShellComponent } from '../../shared/layout/public-shell.component';

const loadSessionStatusPage = () =>
  import('./pages/session-status/session-status.page').then(
    (module) => module.SessionStatusPage
  );

export const publicRoutes: Routes = [
  {
    path: 'error',
    title: 'No pudimos iniciar sesión | Nerva Banking',
    data: { statusKind: 'error' },
    loadComponent: loadSessionStatusPage
  },
  {
    path: 'sesion-expirada',
    title: 'Necesitás volver a ingresar | Nerva Banking',
    data: { statusKind: 'expired' },
    loadComponent: loadSessionStatusPage
  },
  {
    path: 'sesion-cerrada',
    title: 'Sesión cerrada | Nerva Banking',
    data: { statusKind: 'closed' },
    loadComponent: loadSessionStatusPage
  },
  {
    path: '',
    component: PublicShellComponent,
    children: [
      {
        path: '',
        title: 'Nerva Banking',
        loadComponent: () =>
          import('./pages/landing/landing.page').then(
            (module) => module.LandingPage
          )
      }
    ]
  }
];
