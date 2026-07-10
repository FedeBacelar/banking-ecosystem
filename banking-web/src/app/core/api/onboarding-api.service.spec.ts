import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OnboardingApiService } from './onboarding-api.service';

describe('OnboardingApiService', () => {
  let service: OnboardingApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    document.cookie = 'NB-XSRF-TOKEN=csrf-token; Path=/';

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withXsrfConfiguration({
            cookieName: 'NB-XSRF-TOKEN',
            headerName: 'X-XSRF-TOKEN'
          })
        ),
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

  it('sends the CSRF cookie value in the mutation header', () => {
    let completed = false;
    service.startApplication('applicant@example.com').subscribe({
      complete: () => completed = true
    });

    const csrfRequest = http.expectOne('/web/csrf');
    expect(csrfRequest.request.method).toBe('GET');
    csrfRequest.flush({ headerName: 'X-XSRF-TOKEN' });

    const startRequest = http.expectOne('/web/onboarding/applications');
    expect(startRequest.request.method).toBe('POST');
    expect(startRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    startRequest.flush(null, { status: 202, statusText: 'Accepted' });

    expect(completed).toBe(true);
  });
});
