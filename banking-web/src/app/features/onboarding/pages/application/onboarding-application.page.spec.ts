import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { OnboardingDraftStore } from '../../data-access/onboarding-draft.store';
import { OnboardingApplicationPage } from './onboarding-application.page';

describe('OnboardingApplicationPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OnboardingApplicationPage],
      providers: [
        OnboardingDraftStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);
  });

  afterEach(() => vi.restoreAllMocks());

  it('starts with the first of five customer-facing steps', () => {
    const fixture = TestBed.createComponent(OnboardingApplicationPage);
    fixture.detectChanges();
    const copy = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(copy).toContain('Paso 1 de 5');
    expect(copy).toContain('Contanos sobre vos');
    expect(copy).not.toContain('ARG');
    expect(copy).not.toContain('documentType');

    const store = TestBed.inject(OnboardingDraftStore);
    const name = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#first-name')!;
    name.value = 'Ana';
    name.dispatchEvent(new Event('input', { bubbles: true }));
    expect(store.dirty()).toBe(true);
  });

  it('keeps the complete draft in memory and sends the reviewed multipart request', () => {
    const store = TestBed.inject(OnboardingDraftStore);
    store.accessGranted.set(true);
    store.draft.set({
      firstName: 'Ana',
      middleName: '',
      lastName: 'Prueba',
      birthDate: '1990-01-01',
      nationality: 'AR',
      documentNumber: '12.345.678',
      documentValidity: 'NO_DATE',
      documentValidUntil: '',
      nationalPhone: '11 2345 6789',
      street: 'Avenida Siempreviva',
      streetNumber: '742',
      province: 'Buenos Aires',
      locality: 'La Plata',
      postalCode: '1900',
      termsAccepted: false
    });
    store.dniFront.set(new File(['%PDF-test-front'], 'front.pdf', { type: 'application/pdf' }));
    store.dniBack.set(new File(['%PDF-test-back'], 'back.pdf', { type: 'application/pdf' }));
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(OnboardingApplicationPage);
    fixture.detectChanges();

    for (let step = 0; step < 4; step += 1) {
      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
      fixture.detectChanges();
    }

    const review = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(review).toContain('Revisá tu información');
    expect(review).toContain('DNI ••.678');
    expect(review).toContain('Frente listo · Dorso listo');
    expect(review).not.toContain('documentIssuingCountry');

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('article button')!.click();
    store.draft.update((draft) => ({ ...draft, firstName: '' }));
    fixture.detectChanges();
    const reviewStepButton = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('aside button')
    ).find((button) => button.textContent?.includes('Revisión'))!;
    reviewStepButton.click();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ingresá tu nombre.');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Contanos sobre vos');

    store.draft.update((draft) => ({ ...draft, firstName: 'Ana' }));
    fixture.detectChanges();
    reviewStepButton.click();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Revisá tu información');

    store.draft.update((draft) => ({ ...draft, termsAccepted: true }));
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[type="submit"]')!.click();

    const rejectedRequest = http.expectOne('/web/onboarding/submissions');
    expect(rejectedRequest.request.body).toBeInstanceOf(FormData);
    rejectedRequest.flush(
      { code: 'INVALID_ONBOARDING_DOCUMENT' },
      { status: 400, statusText: 'Bad Request' }
    );
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Necesitamos tu DNI');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'No pudimos aceptar uno de los archivos del DNI.'
    );

    const finalReviewButton = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('aside button')
    ).find((button) => button.textContent?.includes('Revisión'))!;
    finalReviewButton.click();
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    const acceptedRequest = http.expectOne('/web/onboarding/submissions');
    acceptedRequest.flush({ applicationId: 'id', status: 'SUBMITTED', submittedAt: '', updatedAt: '' });
    expect(navigate).toHaveBeenCalledWith(['/onboarding/solicitud-enviada']);
    expect(store.dniFront()).toBeNull();
    expect(store.dniBack()).toBeNull();
    http.verify();
  });
});
