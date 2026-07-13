import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OnboardingApiService } from './onboarding-api.service';

describe('OnboardingApiService', () => {
  let service: OnboardingApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(OnboardingApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts an application without exposing data in the URL', () => {
    service.startApplication('person@example.com').subscribe();

    const request = http.expectOne('/web/onboarding/applications');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'person@example.com' });
    request.flush(null);
  });

  it('uses the exact multipart part names and lets the browser set its boundary', () => {
    const front = new File(['front'], 'front.png', { type: 'image/png' });
    const back = new File(['back'], 'back.png', { type: 'image/png' });
    const submission = {
      firstName: 'Ana', middleName: null, lastName: 'Pérez', birthDate: '1995-04-12',
      nationality: 'AR', documentType: 'DNI' as const, documentNumber: '12345678',
      documentIssuingCountry: 'AR' as const, documentExpirationDate: null,
      phoneNumber: '+541123456789', street: 'Corrientes', streetNumber: '123',
      city: 'Buenos Aires', province: 'Ciudad Autónoma de Buenos Aires', postalCode: '1043',
      country: 'AR' as const, termsAccepted: true
    };

    service.submitApplication(submission, front, back).subscribe();

    const request = http.expectOne('/web/onboarding/submissions');
    const body = request.request.body as FormData;
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.has('Content-Type')).toBe(false);
    expect(Array.from(body.keys())).toEqual(['submission', 'dniFront', 'dniBack']);
    expect((body.get('submission') as Blob).type).toBe('application/json');
    expect(body.get('dniFront')).toBe(front);
    expect(body.get('dniBack')).toBe(back);
    request.flush({ applicationId: 'id', status: 'SUBMITTED', submittedAt: '', updatedAt: '' });
  });
});
