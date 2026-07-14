import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { OnboardingShellComponent } from './onboarding-shell.component';

describe('OnboardingShellComponent', () => {
  it('keeps the existing-customer login in the regular onboarding journey', async () => {
    await configure({});
    const fixture = TestBed.createComponent(OnboardingShellComponent);
    fixture.detectChanges();

    const login = (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLAnchorElement>('a[href="/web/auth/login/home"]');
    expect(login?.textContent).toContain('Ya tengo una cuenta');
  });

  it('hides the existing-customer login during onboarding completion', async () => {
    await configure({ hideExistingAccountLogin: true });
    const fixture = TestBed.createComponent(OnboardingShellComponent);
    fixture.detectChanges();

    expect(
      (fixture.nativeElement as HTMLElement)
        .querySelector('a[href="/web/auth/login/home"]')
    ).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'esta solicitud no abre una cuenta real'
    );
  });

  async function configure(data: Record<string, unknown>): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [OnboardingShellComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data } }
        }
      ]
    }).compileComponents();
  }
});
