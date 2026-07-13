import { Component, input } from '@angular/core';

@Component({
  selector: 'nb-nerva-logo',
  template: `
    <img
      class="h-9 w-auto sm:h-10"
      [src]="inverse() ? '/assets/brand/nerva-logo-light.svg' : '/assets/brand/nerva-logo.svg'"
      width="190"
      height="42"
      alt="Nerva Banking"
    />
  `
})
export class NervaLogoComponent {
  readonly inverse = input(false);
}
