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

  it('captures birth day, month and year without a long date-picker journey', () => {
    const store = TestBed.inject(OnboardingDraftStore);
    store.draft.set({ ...validDraft(), birthDate: '' });
    const fixture = TestBed.createComponent(OnboardingApplicationPage);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const day = element.querySelector<HTMLInputElement>('#birth-date-day')!;
    const month = element.querySelector<HTMLInputElement>('#birth-date-month')!;
    const year = element.querySelector<HTMLInputElement>('#birth-date-year')!;

    expect(day.autocomplete).toBe('bday-day');
    expect(month.autocomplete).toBe('bday-month');
    expect(year.autocomplete).toBe('bday-year');
    expect(element.querySelector('input[type="date"]#birth-date-day')).toBeNull();

    enterValue(day, '31');
    enterValue(month, '02');
    enterValue(year, '2000');
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Ingresá una fecha válida.');

    enterValue(day, '01');
    enterValue(month, '01');
    enterValue(year, String(new Date().getFullYear() - 10));
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('tenés que tener 18 años o más');

    enterValue(year, '1990');
    fixture.detectChanges();
    expect(store.draft().birthDate).toBe('1990-01-01');
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Necesitamos tu DNI');
  });

  it('uses one file action per DNI side and previews, replaces and removes local files', () => {
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL')
      .mockReturnValueOnce('blob:front-image')
      .mockReturnValueOnce('blob:front-pdf')
      .mockReturnValueOnce('blob:back-image');
    const revokeObjectUrl = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const store = TestBed.inject(OnboardingDraftStore);
    store.draft.set(validDraft());
    const fixture = TestBed.createComponent(OnboardingApplicationPage);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(element.querySelector('input[capture]')).toBeNull();
    expect(element.querySelectorAll('input[type="file"]')).toHaveLength(2);
    const frontInput = element.querySelector<HTMLInputElement>('#front-file')!;
    expect(frontInput.accept).toBe('image/jpeg,image/png,application/pdf');
    expect(element.querySelector<HTMLLabelElement>('label[for="front-file"]')!.textContent).toContain('Agregar archivo');

    chooseFile(frontInput, new File(['image'], 'frente.png', { type: 'image/png' }));
    fixture.detectChanges();
    expect(store.dniFront()?.name).toBe('frente.png');
    expect(element.querySelector<HTMLImageElement>('img[alt="Vista previa de frente del dni"]')?.getAttribute('src')).toBe('blob:front-image');
    expect(element.querySelector<HTMLLabelElement>('label[for="front-file"]')!.textContent).toContain('Reemplazar archivo');

    chooseFile(frontInput, new File(['%PDF'], 'frente.pdf', { type: 'application/pdf' }));
    fixture.detectChanges();
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:front-image');
    expect(element.querySelector<HTMLIFrameElement>('iframe[title="Vista previa de frente del dni"]')?.getAttribute('src')).toBe('blob:front-pdf');

    element.querySelector<HTMLButtonElement>('button[aria-label="Quitar frente del dni"]')!.click();
    fixture.detectChanges();
    expect(store.dniFront()).toBeNull();
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:front-pdf');
    expect(element.querySelector<HTMLLabelElement>('label[for="front-file"]')!.textContent).toContain('Agregar archivo');

    const backInput = element.querySelector<HTMLInputElement>('#back-file')!;
    chooseFile(backInput, new File(['image'], 'dorso.jpg', { type: 'image/jpeg' }));
    fixture.detectChanges();
    fixture.destroy();
    expect(createObjectUrl).toHaveBeenCalledTimes(3);
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:back-image');
  });

  it('treats the country code and phone input as one focused control', () => {
    const store = TestBed.inject(OnboardingDraftStore);
    store.draft.set(validDraft());
    store.dniFront.set(new File(['front'], 'front.png', { type: 'image/png' }));
    store.dniBack.set(new File(['back'], 'back.png', { type: 'image/png' }));
    const fixture = TestBed.createComponent(OnboardingApplicationPage);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    const input = element.querySelector<HTMLInputElement>('#national-phone')!;
    expect(input.classList).toContain('nb-phone-input');
    expect(input.parentElement?.classList).toContain('nb-phone-control');
    expect(input.getAttribute('aria-describedby')).toBe('national-phone-help');
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
    expect(review).not.toContain('Documentos de desarrollo');

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
    expect(navigate).toHaveBeenCalledWith(['/onboarding/estado']);
    expect(store.dniFront()).toBeNull();
    expect(store.dniBack()).toBeNull();
    http.verify();
  });
});

function validDraft() {
  return {
    firstName: 'Ana',
    middleName: '',
    lastName: 'Prueba',
    birthDate: '1990-01-01',
    nationality: 'AR',
    documentNumber: '12.345.678',
    documentValidity: 'NO_DATE' as const,
    documentValidUntil: '',
    nationalPhone: '11 2345 6789',
    street: 'Avenida Siempreviva',
    streetNumber: '742',
    province: 'Buenos Aires',
    locality: 'La Plata',
    postalCode: '1900',
    termsAccepted: false
  };
}

function enterValue(input: HTMLInputElement, value: string): void {
  input.value = value;
  input.dispatchEvent(new Event('input', { bubbles: true }));
}

function chooseFile(input: HTMLInputElement, file: File): void {
  Object.defineProperty(input, 'files', {
    configurable: true,
    value: { item: (index: number) => index === 0 ? file : null, length: 1, 0: file }
  });
  input.dispatchEvent(new Event('change', { bubbles: true }));
}
