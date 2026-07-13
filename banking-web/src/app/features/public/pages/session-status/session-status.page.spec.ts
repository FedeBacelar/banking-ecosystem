import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { SessionStatusPage } from './session-status.page';

describe('SessionStatusPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionStatusPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { statusKind: 'closed' } } }
        }
      ]
    }).compileComponents();
  });

  it('presents a dedicated logout confirmation without duplicate actions', async () => {
    const fixture = TestBed.createComponent(SessionStatusPage);
    fixture.detectChanges();
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    const copy = element.textContent ?? '';
    const loginLinks = element.querySelectorAll(
      'a[href="/web/auth/login/home"]'
    );

    expect(element.querySelector('h1')?.textContent).toContain('Sesión cerrada');
    expect(copy).toContain('Cerraste tu sesión.');
    expect(copy).toContain('Nerva Banking no es una entidad financiera');
    expect(copy).not.toContain('finalizó correctamente');
    expect(loginLinks).toHaveLength(1);
  });
});
