import { Routes } from '@angular/router';

import { PublicShellComponent } from '../../shared/layout/public-shell.component';

const loadLegalPage = () =>
  import('./pages/legal-document.page').then((module) => module.LegalDocumentPage);

export const legalRoutes: Routes = [
  {
    path: '',
    component: PublicShellComponent,
    children: [
      {
        path: 'terminos',
        title: 'Términos y condiciones | Nerva Banking',
        data: { document: 'terms' },
        loadComponent: loadLegalPage
      },
      {
        path: 'privacidad',
        title: 'Política de privacidad | Nerva Banking',
        data: { document: 'privacy' },
        loadComponent: loadLegalPage
      },
      { path: '**', redirectTo: 'terminos' }
    ]
  }
];
