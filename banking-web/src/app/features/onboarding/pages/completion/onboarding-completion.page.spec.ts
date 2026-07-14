import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { OnboardingCompletionStore } from '../../data-access/onboarding-completion.store';
import { OnboardingCompletionPage } from './onboarding-completion.page';

describe('OnboardingCompletionPage', () => {
  let fixture: ComponentFixture<OnboardingCompletionPage>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OnboardingCompletionPage],
      providers: [
        OnboardingCompletionStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    fixture?.destroy();
    http.verify();
  });

  it('shows a short customer-facing processing state and supports manual refresh', () => {
    createAndFlush('PROCESSING');

    const root = fixture.nativeElement as HTMLElement;
    const copy = root.textContent ?? '';
    expect(copy).toContain('Estamos terminando de preparar tu cuenta');
    expect(copy).toContain(
      'Ya creaste tu acceso. Ahora estamos terminando de preparar tu cuenta.'
    );
    expect(copy).not.toMatch(/Keycloak|reconciliaci|credential|subject|applicationId/i);

    const refresh = Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Actualizar'))!;
    refresh.click();
    http.expectOne('/web/onboarding/completion-status').flush(statusResponse('PROCESSING'));
  });

  it('focuses and announces the completed result with a direct app destination', async () => {
    createAndFlush('COMPLETED');
    await fixture.whenStable();
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const heading = root.querySelector<HTMLHeadingElement>('#completion-heading')!;
    const destination = root.querySelector<HTMLAnchorElement>('a[href="/app/inicio"]');
    const liveCopy = Array.from(root.querySelectorAll('[aria-live="polite"]'))
      .map((element) => element.textContent ?? '')
      .join(' ');

    expect(heading.textContent).toContain('Tu cuenta está lista');
    expect(document.activeElement).toBe(heading);
    expect(liveCopy).toContain('Tu cuenta está lista.');
    expect(destination?.textContent).toContain('Ir a Nerva Banking');
    expect(root.querySelector('a[href="/web/auth/login/home"]')).toBeNull();
  });

  it('shows a terminal failure without internal language or a fake support channel', async () => {
    createAndFlush('FAILED');
    await fixture.whenStable();
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const copy = root.textContent ?? '';
    expect(copy).toContain('No pudimos terminar de preparar tu cuenta');
    expect(copy).toContain(
      'Tu solicitud quedó guardada y no necesitás volver a cargar tus datos.'
    );
    expect(copy).not.toMatch(/Keycloak|reconciliaci|provision|soporte|subject/i);
    expect(root.querySelector('a[href="/"]')?.textContent).toContain('Volver al inicio');
  });

  it('uses the fixed top-level completion login when the authenticated session is missing', () => {
    fixture = TestBed.createComponent(OnboardingCompletionPage);
    fixture.detectChanges();
    http.expectOne('/web/onboarding/completion-status').flush(
      { code: 'AUTHENTICATION_REQUIRED' },
      { status: 401, statusText: 'Unauthorized' }
    );
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('Necesitás volver a ingresar');
    expect(root.textContent).toContain(
      'Ingresá nuevamente para confirmar si tu cuenta ya está lista.'
    );
    expect(
      root.querySelector<HTMLAnchorElement>(
        'a[href="/web/auth/login/onboarding-completion"]'
      )?.textContent
    ).toContain('Ingresar nuevamente');
  });

  function createAndFlush(status: 'PROCESSING' | 'COMPLETED' | 'FAILED'): void {
    fixture = TestBed.createComponent(OnboardingCompletionPage);
    fixture.detectChanges();
    http.expectOne('/web/onboarding/completion-status').flush(statusResponse(status));
    fixture.detectChanges();
  }

  function statusResponse(status: 'PROCESSING' | 'COMPLETED' | 'FAILED') {
    return {
      status,
      updatedAt: '2026-07-13T00:00:00Z'
    };
  }
});
