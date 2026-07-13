import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { OnboardingStatusStore } from '../../data-access/onboarding-status.store';
import { OnboardingStatusPage } from './onboarding-status.page';

describe('OnboardingStatusPage', () => {
  let fixture: ComponentFixture<OnboardingStatusPage>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OnboardingStatusPage],
      providers: [
        OnboardingStatusStore,
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

  it('shows the real review state and four customer-facing milestones without identifiers', () => {
    fixture = TestBed.createComponent(OnboardingStatusPage);
    fixture.detectChanges();
    http.expectOne('/web/onboarding/status').flush({
      applicationId: 'internal-application-id',
      status: 'UNDER_AUTOMATED_REVIEW',
      nextAction: 'WAIT',
      updatedAt: '2026-07-13T00:00:00Z'
    });
    fixture.detectChanges();

    const copy = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(copy).toContain('Estamos revisando tu solicitud');
    expect(copy).toContain('Recibimos tus datos. Por ahora no necesitás hacer nada.');
    expect(copy).toContain('Solicitud enviada');
    expect(copy).toContain('Revisión de la solicitud');
    expect(copy).toContain('Preparación de la cuenta');
    expect(copy).toContain('Creación del acceso');
    expect(copy).not.toContain('internal-application-id');
    expect(copy).not.toContain('UNDER_AUTOMATED_REVIEW');
  });

  it('requests a durable resend with an idempotency key and honors Retry-After', () => {
    fixture = TestBed.createComponent(OnboardingStatusPage);
    fixture.detectChanges();
    http.expectOne('/web/onboarding/status').flush({
      applicationId: 'id',
      status: 'CREDENTIAL_SETUP_PENDING',
      nextAction: 'CHECK_EMAIL',
      updatedAt: '2026-07-13T00:00:00Z'
    });
    fixture.detectChanges();

    const button = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')
    ).find((candidate) => candidate.textContent?.includes('Reenviar correo'))!;
    button.click();

    const resend = http.expectOne('/web/onboarding/credential-invitations/resend');
    expect(resend.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/i);
    resend.flush(
      { code: 'CREDENTIAL_INVITATION_COOLDOWN' },
      { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '42' } }
    );
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Esperá 42 s antes de pedir otro correo.'
    );
    const liveCopy = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('[aria-live="polite"]')
    ).map((element) => element.textContent ?? '').join(' ');
    expect(liveCopy).toContain('Tenés que esperar antes de pedir otro correo.');
  });

  it('offers a new link when the continuation is no longer available', () => {
    fixture = TestBed.createComponent(OnboardingStatusPage);
    fixture.detectChanges();
    http.expectOne('/web/onboarding/status').flush(
      { code: 'ONBOARDING_SESSION_REQUIRED' },
      { status: 401, statusText: 'Unauthorized' }
    );
    fixture.detectChanges();

    const copy = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(copy).toContain('No pudimos abrir tu solicitud');
    expect(copy).toContain('Pedí un nuevo enlace para volver a consultarla.');
    expect(copy).toContain('Pedir un nuevo enlace');
  });
});
