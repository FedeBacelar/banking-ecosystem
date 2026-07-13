import { TestBed } from '@angular/core/testing';

import { LandingPage } from './landing.page';

describe('LandingPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [LandingPage] }).compileComponents();
  });

  it('offers a top-level navigation to the BFF login endpoint', () => {
    const fixture = TestBed.createComponent(LandingPage);
    fixture.detectChanges();

    const loginLinks = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>(
        'a[href="/web/auth/login/home"]'
      )
    );

    expect(loginLinks.length).toBeGreaterThan(0);
    expect(loginLinks[0]?.textContent).toContain('Ingresar');
  });

  it('uses direct customer-facing copy without implementation language', () => {
    const fixture = TestBed.createComponent(LandingPage);
    fixture.detectChanges();

    const copy = (fixture.nativeElement as HTMLElement).textContent?.toLowerCase() ?? '';
    expect(copy).toContain('tu cuenta nerva, en un solo lugar');
    expect(copy).not.toContain('experiencia');
    expect(copy).not.toContain('entorno protegido');
    expect(copy).not.toContain('keycloak');
    expect(copy).not.toContain('microservicio');
  });
});
