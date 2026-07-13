import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { OnboardingDraftStore } from '../../data-access/onboarding-draft.store';
import { OnboardingStartPage } from './onboarding-start.page';

describe('OnboardingStartPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OnboardingStartPage],
      providers: [
        OnboardingDraftStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
  });

  it('validates the email locally and uses the enumeration-safe endpoint', () => {
    const fixture = TestBed.createComponent(OnboardingStartPage);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('input')!;
    input.value = 'person@example.com';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLFormElement>('form')!
      .dispatchEvent(new SubmitEvent('submit'));

    const request = http.expectOne('/web/onboarding/applications');
    expect(request.request.body).toEqual({ email: 'person@example.com' });
    request.flush(null);
    expect(navigate).toHaveBeenCalledWith(['/onboarding/correo-enviado']);
    http.verify();
  });
});
