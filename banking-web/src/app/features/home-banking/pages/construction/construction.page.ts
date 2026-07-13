import { Component } from '@angular/core';
import { LucideConstruction, LucideDynamicIcon } from '@lucide/angular';

@Component({
  selector: 'nb-construction-page',
  imports: [LucideDynamicIcon],
  template: `
    <section
      class="flex w-full flex-1 items-center justify-center px-5 py-12 sm:px-6"
      aria-labelledby="construction-title"
    >
      <div class="max-w-md text-center">
        <span
          class="mx-auto flex size-16 items-center justify-center rounded-2xl bg-surface-subtle text-brand"
        >
          <svg
            [lucideIcon]="construction"
            class="size-8"
            aria-hidden="true"
          ></svg>
        </span>
        <p
          class="mt-6 text-xs font-semibold uppercase tracking-[0.14em] text-action"
        >
          Home banking
        </p>
        <h1
          id="construction-title"
          class="mt-3 text-pretty text-3xl font-semibold tracking-tight text-ink sm:text-4xl"
        >
          El home banking está en construcción
        </h1>
        <p class="mt-4 text-sm leading-6 text-ink-muted sm:text-base">
          En esta versión todavía no podés consultar cuentas ni realizar
          operaciones.
        </p>
        <p class="mt-7 text-xs leading-5 text-ink-muted">
          Nerva Banking es un proyecto académico y no realiza operaciones
          reales.
        </p>
      </div>
    </section>
  `
})
export class ConstructionPage {
  protected readonly construction = LucideConstruction;
}
