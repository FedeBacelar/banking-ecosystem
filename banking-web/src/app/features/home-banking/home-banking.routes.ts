import { Routes } from '@angular/router';

import { sessionGuard } from '../../core/auth/session.guard';
import { AuthenticatedShellComponent } from './layout/authenticated-shell.component';

export const homeBankingRoutes: Routes = [
  {
    path: '',
    component: AuthenticatedShellComponent,
    canActivateChild: [sessionGuard],
    children: [
      {
        path: 'inicio',
        title: 'Inicio | Nerva Banking',
        loadComponent: () =>
          import('./pages/construction/construction.page').then(
            (module) => module.ConstructionPage
          )
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'inicio'
      },
      {
        path: '**',
        redirectTo: 'inicio'
      }
    ]
  }
];
