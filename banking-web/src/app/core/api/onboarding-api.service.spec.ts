import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OnboardingSubmissionRequest } from '../../features/onboarding/models/onboarding.models';
import { OnboardingApiService } from './onboarding-api.service';

describe('OnboardingApiService', () => {
  let service: OnboardingApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    document.cookie = 'NB-XSRF-TOKEN=csrf-token; Path=/';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withXsrfConfiguration({
          cookieName: 'NB-XSRF-TOKEN',
          headerName: 'X-XSRF-TOKEN'
        })),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(OnboardingApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    document.cookie = 'NB-XSRF-TOKEN=; Max-Age=0; Path=/';
  });

  it('starts onboarding with exactly one API request', () => {
    service.startApplication('applicant@example.com').subscribe();

    const request = http.expectOne('/web/onboarding/applications');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'applicant@example.com' });
    request.flush(null, { status: 202, statusText: 'Accepted' });
  });

  it('exchanges the magic link without requesting session state separately', () => {
    service.consumeMagicLink('magic-token').subscribe();

    const request = http.expectOne('/web/onboarding/magic-links/consume');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ token: 'magic-token' });
    request.flush({ status: 'IN_PROGRESS', nextAction: 'CONTINUE_APPLICATION' });
  });

  it('submits applicant data, terms and both documents in one multipart request', () => {
    const submission: OnboardingSubmissionRequest = {
      firstName: 'Federico',
      lastName: 'Bacelar',
      birthDate: '1990-05-10',
      nationality: 'AR',
      documentType: 'DNI',
      documentNumber: '30111222',
      documentIssuingCountry: 'AR',
      phoneNumber: '+5491122223333',
      street: 'Av Siempre Viva',
      streetNumber: '742',
      city: 'Buenos Aires',
      province: 'Buenos Aires',
      postalCode: '1000',
      country: 'AR',
      termsAccepted: true
    };
    const front = new File(['front'], 'front.png', { type: 'image/png' });
    const back = new File(['back'], 'back.png', { type: 'image/png' });

    service.submitApplication(submission, front, back).subscribe();

    const request = http.expectOne('/web/onboarding/submissions');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    expect(request.request.body).toBeInstanceOf(FormData);
    expect((request.request.body as FormData).get('dniFront')).toBe(front);
    expect((request.request.body as FormData).get('dniBack')).toBe(back);
    request.flush({
      applicationId: 'application-1',
      status: 'SUBMITTED',
      submittedAt: '2026-07-10T12:00:00Z',
      updatedAt: '2026-07-10T12:00:00Z'
    });
  });
});
