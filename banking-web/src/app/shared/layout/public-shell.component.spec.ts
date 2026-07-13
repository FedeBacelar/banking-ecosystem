import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PublicShellComponent } from './public-shell.component';

describe('PublicShellComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicShellComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('shows the academic disclaimer without marketing filler', () => {
    const fixture = TestBed.createComponent(PublicShellComponent);
    fixture.detectChanges();

    const copy = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(copy).toContain('Nerva Banking no es una entidad financiera');
    expect(copy).toContain('No ingreses datos personales');
    expect(copy).not.toContain('experiencia bancaria clara');
  });
});
