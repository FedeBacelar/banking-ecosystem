import { Component, ElementRef, computed, effect, inject, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideCircleAlert,
  LucideCircleCheck,
  LucideClock3,
  LucideDynamicIcon,
  LucideRefreshCw
} from '@lucide/angular';

import { OnboardingCompletionStore } from '../../data-access/onboarding-completion.store';
import { OnboardingCompletionState } from '../../models/onboarding.models';

interface CompletionView {
  title: string;
  description: string;
  tone: 'waiting' | 'success' | 'failure';
}

interface CompletionProblem {
  title: string;
  description: string;
  action: 'none' | 'retry' | 'login';
}

const COMPLETION_VIEWS: Record<OnboardingCompletionState, CompletionView> = {
  PROCESSING: {
    title: 'Estamos terminando de preparar tu cuenta',
    description: 'Ya creaste tu acceso. Ahora estamos terminando de preparar tu cuenta.',
    tone: 'waiting'
  },
  COMPLETED: {
    title: 'Tu cuenta está lista',
    description: 'Ya podés entrar a Nerva Banking.',
    tone: 'success'
  },
  FAILED: {
    title: 'No pudimos terminar de preparar tu cuenta',
    description: 'Tu solicitud quedó guardada y no necesitás volver a cargar tus datos.',
    tone: 'failure'
  }
};

@Component({
  selector: 'nb-onboarding-completion-page',
  imports: [LucideDynamicIcon, RouterLink],
  host: { class: 'flex flex-1' },
  template: `
    <section class="flex w-full flex-1 items-center py-10 sm:py-16">
      <div class="mx-auto w-full max-w-lg px-5 sm:px-6">
        <p class="sr-only" aria-live="polite">{{ store.announcement() }}</p>

        @if (store.loadState() === 'loading') {
          <div
            class="rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8"
            aria-busy="true"
            aria-live="polite"
          >
            <span class="nb-spinner mx-auto block text-action" aria-hidden="true"></span>
            <h1 class="mt-5 text-2xl font-semibold tracking-[-0.02em] text-ink">
              Estamos terminando de preparar tu cuenta
            </h1>
            <p class="mt-3 text-sm leading-6 text-ink-muted">
              Esto puede demorar unos segundos.
            </p>
          </div>
        } @else if (problem(); as currentProblem) {
          <div class="rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8">
            <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-caution-subtle text-caution">
              <svg [lucideIcon]="circleAlert" class="size-6" aria-hidden="true"></svg>
            </span>
            <h1 class="mt-5 text-2xl font-semibold tracking-[-0.02em] text-ink">
              {{ currentProblem.title }}
            </h1>
            <p class="mt-3 text-sm leading-6 text-ink-muted">
              {{ currentProblem.description }}
            </p>

            @if (currentProblem.action === 'retry') {
              <button
                type="button"
                (click)="store.refresh()"
                class="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white hover:bg-brand-strong"
              >
                Intentar nuevamente
              </button>
            } @else if (currentProblem.action === 'login') {
              <a
                href="/web/auth/login/onboarding-completion"
                class="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white hover:bg-brand-strong"
              >
                Ingresar nuevamente
              </a>
            }
          </div>
        } @else if (view(); as currentView) {
          <article
            class="rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8"
            aria-labelledby="completion-heading"
          >
            @switch (currentView.tone) {
              @case ('success') {
                <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-positive-subtle text-positive">
                  <svg [lucideIcon]="circleCheck" class="size-6" aria-hidden="true"></svg>
                </span>
              }
              @case ('failure') {
                <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-caution-subtle text-caution">
                  <svg [lucideIcon]="circleAlert" class="size-6" aria-hidden="true"></svg>
                </span>
              }
              @default {
                <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-action-soft text-action">
                  <svg [lucideIcon]="clock" class="size-6" aria-hidden="true"></svg>
                </span>
              }
            }

            <h1
              #statusHeading
              id="completion-heading"
              tabindex="-1"
              style="outline: none"
              class="mt-5 text-pretty text-3xl font-semibold tracking-[-0.03em] text-ink"
            >
              {{ currentView.title }}
            </h1>
            <p class="mt-3 text-base leading-7 text-ink-muted">
              {{ currentView.description }}
            </p>

            @if (!store.isOnline()) {
              <div class="mt-6 rounded-xl border border-caution/30 bg-caution-subtle px-4 py-3 text-left text-sm leading-6 text-ink" role="status">
                <strong>Sin conexión.</strong> Cuando la recuperes, vamos a continuar.
              </div>
            }

            @if (store.refreshError()) {
              <div class="mt-6 rounded-xl border border-danger/20 bg-danger-subtle px-4 py-3 text-left text-sm leading-6 text-danger" role="alert">
                {{ store.refreshError() }}
              </div>
            }

            @if (store.autoUpdateStopped() && store.status()?.status === 'PROCESSING') {
              <p class="mt-6 rounded-xl border border-line bg-surface-subtle px-4 py-3 text-left text-sm leading-6 text-ink-muted">
                Todavía estamos preparando tu cuenta. Podés actualizar cuando quieras.
              </p>
            }

            @if (store.status(); as status) {
              <p class="mt-6 text-xs leading-5 text-ink-muted">
                Última actualización:
                <time [attr.datetime]="status.updatedAt">{{ formatUpdatedAt(status.updatedAt) }}</time>
              </p>
            }

            <div class="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
              @switch (store.status()?.status) {
                @case ('COMPLETED') {
                  <a
                    routerLink="/app/inicio"
                    class="inline-flex min-h-11 items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white hover:bg-brand-strong"
                  >
                    Ir a Nerva Banking
                  </a>
                }
                @case ('FAILED') {
                  <a
                    routerLink="/"
                    class="inline-flex min-h-11 items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white hover:bg-brand-strong"
                  >
                    Volver al inicio
                  </a>
                }
                @default {
                  <button
                    type="button"
                    (click)="store.refresh()"
                    [disabled]="store.isRefreshing() || !store.isOnline()"
                    class="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-line bg-surface px-5 text-sm font-semibold text-brand hover:bg-surface-subtle disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <svg [lucideIcon]="refresh" class="size-4" [class.animate-spin]="store.isRefreshing()" aria-hidden="true"></svg>
                    {{ store.isRefreshing() ? 'Actualizando…' : 'Actualizar' }}
                  </button>
                }
              }
            </div>
          </article>
        }
      </div>
    </section>
  `
})
export class OnboardingCompletionPage {
  protected readonly store = inject(OnboardingCompletionStore);
  protected readonly circleAlert = LucideCircleAlert;
  protected readonly circleCheck = LucideCircleCheck;
  protected readonly clock = LucideClock3;
  protected readonly refresh = LucideRefreshCw;
  protected readonly view = computed(() => {
    const state = this.store.status()?.status;
    return state ? COMPLETION_VIEWS[state] : null;
  });
  protected readonly problem = computed<CompletionProblem | null>(() => {
    switch (this.store.loadState()) {
      case 'offline':
        return {
          title: 'Sin conexión',
          description: 'Cuando recuperes la conexión, vamos a continuar.',
          action: 'none'
        };
      case 'session-required':
        return {
          title: 'Necesitás volver a ingresar',
          description: 'Ingresá nuevamente para confirmar si tu cuenta ya está lista.',
          action: 'login'
        };
      case 'error':
        return {
          title: 'No pudimos confirmar si tu cuenta está lista',
          description: 'Intentá nuevamente en unos segundos.',
          action: 'retry'
        };
      default:
        return null;
    }
  });

  private readonly statusHeading = viewChild<ElementRef<HTMLHeadingElement>>('statusHeading');
  private lastFocusedResult: OnboardingCompletionState | null = null;

  constructor() {
    effect(() => {
      const state = this.store.status()?.status ?? null;
      const heading = this.statusHeading();
      if (
        heading
        && (state === 'COMPLETED' || state === 'FAILED')
        && this.lastFocusedResult !== state
      ) {
        this.lastFocusedResult = state;
        queueMicrotask(() => {
          if (this.store.status()?.status === state) {
            heading.nativeElement.focus();
          }
        });
      }
    });
  }

  protected formatUpdatedAt(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'sin fecha disponible';
    }
    return new Intl.DateTimeFormat('es-AR', {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(date);
  }
}
