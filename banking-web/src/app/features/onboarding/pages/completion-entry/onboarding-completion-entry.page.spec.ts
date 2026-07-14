import { TestBed } from '@angular/core/testing';

import {
  ONBOARDING_COMPLETION_LOGIN_URL,
  ONBOARDING_COMPLETION_NAVIGATOR,
  OnboardingCompletionEntryPage,
  OnboardingCompletionNavigator
} from './onboarding-completion-entry.page';

describe('OnboardingCompletionEntryPage', () => {
  afterEach(() => window.history.replaceState(null, '', '/'));

  it('replaces the old route with one fixed BFF login without forwarding URL data', () => {
    const navigator: OnboardingCompletionNavigator = {
      replace: vi.fn()
    };
    TestBed.configureTestingModule({
      imports: [OnboardingCompletionEntryPage],
      providers: [
        { provide: ONBOARDING_COMPLETION_NAVIGATOR, useValue: navigator }
      ]
    });

    window.history.replaceState(
      null,
      '',
      '/onboarding/credentials-complete?returnTo=https://attacker.example#secret'
    );
    const fixture = TestBed.createComponent(OnboardingCompletionEntryPage);
    fixture.detectChanges();

    expect(navigator.replace).toHaveBeenCalledOnce();
    expect(navigator.replace).toHaveBeenCalledWith(ONBOARDING_COMPLETION_LOGIN_URL);
    expect(ONBOARDING_COMPLETION_LOGIN_URL).toBe(
      '/web/auth/login/onboarding-completion'
    );
    expect(
      (fixture.nativeElement as HTMLElement)
        .querySelector<HTMLAnchorElement>('a')?.getAttribute('href')
    ).toBe(ONBOARDING_COMPLETION_LOGIN_URL);
  });
});
