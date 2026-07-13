import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { OnboardingDraftStore } from '../../data-access/onboarding-draft.store';
import { OnboardingContinuePage } from './onboarding-continue.page';

describe('OnboardingContinuePage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OnboardingContinuePage],
      providers: [
        OnboardingDraftStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
  });

  afterEach(() => {
    window.history.replaceState(null, '', '/');
  });

  it('removes the secret fragment before exchanging the token', () => {
    window.history.replaceState(null, '', '/onboarding/continue#token=secret-value');
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const http = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(OnboardingContinuePage);
    fixture.detectChanges();

    expect(window.location.hash).toBe('');
    expect(window.location.pathname).toBe('/onboarding/continue');
    const request = http.expectOne('/web/onboarding/magic-links/consume');
    expect(request.request.body).toEqual({ token: 'secret-value' });
    request.flush({ status: 'IN_PROGRESS', nextAction: 'CONTINUE_APPLICATION' });
    expect(navigate).toHaveBeenCalledWith(['/onboarding/solicitud']);
    http.verify();
  });

  it('does not call the API when the fragment is missing', () => {
    window.history.replaceState(null, '', '/onboarding/continue');
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(OnboardingContinuePage);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Este enlace no es válido.');
    http.expectNone('/web/onboarding/magic-links/consume');
    http.verify();
  });

  it('routes every post-submission state to the durable status page', () => {
    window.history.replaceState(null, '', '/onboarding/continue#token=secret-value');
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const http = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(OnboardingContinuePage);
    fixture.detectChanges();
    http.expectOne('/web/onboarding/magic-links/consume').flush({
      status: 'SUBMITTED',
      nextAction: 'WAIT'
    });

    expect(navigate).toHaveBeenCalledWith(['/onboarding/estado']);
    http.verify();
  });
});
