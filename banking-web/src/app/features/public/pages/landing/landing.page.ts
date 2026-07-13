import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArrowRight, LucideDynamicIcon } from '@lucide/angular';

@Component({
  selector: 'nb-landing-page',
  imports: [LucideDynamicIcon, RouterLink],
  host: { class: 'flex flex-1' },
  template: `
    <section class="relative flex min-h-[32rem] w-full flex-1 items-center overflow-hidden bg-brand text-white">
      <div
        aria-hidden="true"
        class="absolute inset-0 opacity-[0.06]"
        style="background-image: linear-gradient(to right, white 1px, transparent 1px), linear-gradient(to bottom, white 1px, transparent 1px); background-size: 3.5rem 3.5rem"
      ></div>
      <div
        aria-hidden="true"
        class="absolute -right-24 top-1/2 size-80 -translate-y-1/2 rounded-full border border-white/10 bg-white/[0.03] sm:size-[30rem] lg:right-20"
      ></div>

      <div class="relative mx-auto w-full max-w-6xl px-5 py-20 sm:px-6 sm:py-24 lg:py-28">
        <div class="max-w-2xl">
          <p class="text-xs font-semibold uppercase tracking-[0.16em] text-accent-on-dark">
            Nerva Banking
          </p>
          <h1
            class="mt-4 text-pretty text-4xl font-semibold leading-[1.08] tracking-[-0.035em] sm:text-5xl lg:text-[3.5rem]"
          >
            Tu cuenta Nerva, en un solo lugar
          </h1>
          <p class="mt-5 max-w-xl text-base leading-7 text-white/75 sm:text-lg">
            Ingresá a tu cuenta o empezá una nueva solicitud.
          </p>

          <div class="mt-8 flex flex-col gap-3 sm:flex-row">
            <a
              routerLink="/onboarding"
              class="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-white px-5 text-sm font-semibold text-brand shadow-lg transition-colors hover:bg-slate-50 sm:w-auto"
            >
              Abrir una cuenta
              <svg [lucideIcon]="arrowRight" class="size-4" aria-hidden="true"></svg>
            </a>
            <a
              href="/web/auth/login/home"
              class="inline-flex min-h-12 w-full items-center justify-center rounded-xl border border-white/40 px-5 text-sm font-semibold text-white transition-colors hover:bg-white/10 sm:w-auto"
            >
              Ingresar
            </a>
          </div>
        </div>
      </div>
    </section>
  `
})
export class LandingPage {
  protected readonly arrowRight = LucideArrowRight;
}
