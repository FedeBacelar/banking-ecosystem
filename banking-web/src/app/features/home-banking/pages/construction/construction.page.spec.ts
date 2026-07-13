import { TestBed } from '@angular/core/testing';

import { ConstructionPage } from './construction.page';

describe('ConstructionPage', () => {
  it('shows only the construction state for home banking', async () => {
    await TestBed.configureTestingModule({ imports: [ConstructionPage] }).compileComponents();
    const fixture = TestBed.createComponent(ConstructionPage);
    fixture.detectChanges();

    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(content).toContain('Home banking');
    expect(content).toContain('está en construcción');
    expect(content).not.toContain('saldo');
    expect(content).not.toContain('movimientos');
  });
});
