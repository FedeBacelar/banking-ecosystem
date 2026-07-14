import { NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import {
  LucideCheck,
  LucideCircleAlert,
  LucideCircleCheck,
  LucideClock3,
  LucideDynamicIcon,
  LucideMail,
  LucideRefreshCw
} from '@lucide/angular';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../data-access/onboarding-api.service';
import {
  isCredentialInvitationCooldown,
  retryAfterSeconds
} from '../../data-access/onboarding-error';
import {
  ONBOARDING_MILESTONES,
  OnboardingStatusView
} from '../../data-access/onboarding-status.presenter';
import { OnboardingStatusStore } from '../../data-access/onboarding-status.store';

type MilestoneState = 'complete' | 'active' | 'failed' | 'pending';
type ResendState = 'idle' | 'loading' | 'success' | 'cooldown' | 'error';

@Component({
  selector: 'nb-onboarding-status-page',
  imports: [LucideDynamicIcon, NgTemplateOutlet, RouterLink],
  host: { class: 'flex flex-1' },
  template: `
    <section class="w-full py-8 sm:py-12 lg:py-16">
      <div class="mx-auto w-full max-w-5xl px-5 sm:px-6">
        <p class="sr-only" aria-live="polite">{{ store.announcement() }}</p>

        @if (store.loadState() === 'loading') {
          <div class="mx-auto max-w-lg rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8" aria-live="polite">
            <span class="nb-spinner mx-auto block text-action" aria-hidden="true"></span>
            <h1 class="mt-5 text-2xl font-semibold tracking-[-0.02em] text-ink">Consultando el estado de tu solicitud…</h1>
          </div>
        } @else if (store.loadState() === 'offline') {
          <ng-container [ngTemplateOutlet]="loadProblem" [ngTemplateOutletContext]="{
            title: 'Sin conexión',
            description: 'Cuando recuperes la conexión, vamos a actualizar el estado.',
            retry: false,
            actionLabel: null
          }" />
        } @else if (store.loadState() === 'session-required') {
          <ng-container [ngTemplateOutlet]="loadProblem" [ngTemplateOutletContext]="{
            title: 'No pudimos abrir tu solicitud',
            description: 'Pedí un nuevo enlace para volver a consultarla.',
            retry: false,
            actionLabel: 'Pedir un nuevo enlace'
          }" />
        } @else if (store.loadState() === 'error') {
          <ng-container [ngTemplateOutlet]="loadProblem" [ngTemplateOutletContext]="{
            title: 'No pudimos consultar el estado',
            description: 'Intentá nuevamente en unos segundos.',
            retry: true,
            actionLabel: null
          }" />
        } @else if (store.view(); as view) {
          <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_18rem] lg:items-start">
            <article class="rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-8" aria-labelledby="onboarding-status-heading">
              @switch (view.tone) {
                @case ('success') {
                  <span class="inline-flex size-12 items-center justify-center rounded-xl bg-positive-subtle text-positive">
                    <svg [lucideIcon]="circleCheck" class="size-6" aria-hidden="true"></svg>
                  </span>
                }
                @case ('action') {
                  <span class="inline-flex size-12 items-center justify-center rounded-xl bg-action-soft text-action">
                    <svg [lucideIcon]="mail" class="size-6" aria-hidden="true"></svg>
                  </span>
                }
                @case ('waiting') {
                  <span class="inline-flex size-12 items-center justify-center rounded-xl bg-action-soft text-action">
                    <svg [lucideIcon]="clock" class="size-6" aria-hidden="true"></svg>
                  </span>
                }
                @default {
                  <span class="inline-flex size-12 items-center justify-center rounded-xl bg-caution-subtle text-caution">
                    <svg [lucideIcon]="circleAlert" class="size-6" aria-hidden="true"></svg>
                  </span>
                }
              }

              <h1 id="onboarding-status-heading" class="mt-5 text-3xl font-semibold tracking-[-0.03em] text-ink">
                {{ view.title }}
              </h1>
              <p class="mt-3 max-w-2xl text-base leading-7 text-ink-muted">{{ view.description }}</p>
              @if (view.supportingText) {
                <p class="mt-3 max-w-2xl text-sm leading-6 text-ink-muted">{{ view.supportingText }}</p>
              }

              @if (!store.isOnline()) {
                <div class="mt-6 rounded-xl border border-caution/30 bg-caution-subtle px-4 py-3 text-sm leading-6 text-ink" role="status">
                  <strong>Sin conexión.</strong> Cuando la recuperes, vamos a actualizar el estado.
                </div>
              }

              @if (store.refreshError()) {
                <div class="mt-6 rounded-xl border border-danger/20 bg-danger-subtle px-4 py-3 text-sm leading-6 text-danger" role="alert">
                  {{ store.refreshError() }}
                </div>
              }

              @if (store.autoUpdateStopped()) {
                <p class="mt-6 rounded-xl border border-line bg-surface-subtle px-4 py-3 text-sm leading-6 text-ink-muted">
                  La actualización automática está pausada. Podés actualizar el estado cuando quieras.
                </p>
              }

              @if (store.status(); as status) {
                <p class="mt-6 text-xs leading-5 text-ink-muted">
                  Última actualización:
                  <time [attr.datetime]="status.updatedAt">{{ formatUpdatedAt(status.updatedAt) }}</time>
                </p>
              }

              @if (view.canResendCredentials) {
                <div class="mt-7 border-t border-line pt-6">
                  <p class="text-sm font-semibold text-ink">¿No recibiste el correo?</p>
                  <button
                    type="button"
                    (click)="resendCredentialInvitation()"
                    [disabled]="resendState() === 'loading' || resendState() === 'cooldown'"
                    class="mt-3 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-line bg-surface px-4 text-sm font-semibold text-brand hover:bg-surface-subtle disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
                  >
                    @if (resendState() === 'loading') {
                      <span class="nb-spinner size-4" aria-hidden="true"></span>
                      Enviando otro correo…
                    } @else if (resendState() === 'cooldown' && cooldownSeconds() > 0) {
                      Podés pedir otro correo en {{ formatCooldown(cooldownSeconds()) }}
                    } @else {
                      Reenviar correo
                    }
                  </button>

                  @if (resendMessage()) {
                    <p class="sr-only" aria-live="polite">{{ resendAnnouncement() }}</p>
                    <p
                      class="mt-3 text-sm leading-6"
                      [class.text-positive]="resendState() === 'success'"
                      [class.text-caution]="resendState() === 'cooldown'"
                      [class.text-danger]="resendState() === 'error'"
                      aria-live="off"
                    >
                      {{ resendMessage() }}
                    </p>
                  }
                </div>
              }

              @if (view.action || view.canRefresh) {
                <div class="mt-7 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
                  @if (view.action; as action) {
                    @if (action.kind === 'router') {
                      <a [routerLink]="action.href" class="inline-flex min-h-11 items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white hover:bg-brand-strong">
                        {{ action.label }}
                      </a>
                    } @else {
                      <a [href]="action.href" class="inline-flex min-h-11 items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white hover:bg-brand-strong">
                        {{ action.label }}
                      </a>
                    }
                  }
                  @if (view.canRefresh) {
                    <button
                      type="button"
                      (click)="store.refresh()"
                      [disabled]="store.isRefreshing() || !store.isOnline()"
                      class="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-line bg-surface px-5 text-sm font-semibold text-brand hover:bg-surface-subtle disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <svg [lucideIcon]="refresh" class="size-4" [class.animate-spin]="store.isRefreshing()" aria-hidden="true"></svg>
                      {{ store.isRefreshing() ? 'Actualizando…' : 'Actualizar estado' }}
                    </button>
                  }
                </div>
              }
            </article>

            <aside class="rounded-2xl border border-line bg-surface p-5 sm:p-6" aria-label="Avance de tu solicitud">
              <h2 class="text-sm font-semibold text-ink">Avance de tu solicitud</h2>
              <ol class="mt-5 space-y-0">
                @for (milestone of milestones; track milestone; let index = $index; let last = $last) {
                  <li class="relative flex gap-3 pb-5 last:pb-0">
                    @if (!last) {
                      <span class="absolute left-[0.6875rem] top-6 h-[calc(100%-1rem)] w-px bg-line" aria-hidden="true"></span>
                    }
                    <span
                      class="relative z-10 mt-0.5 inline-flex size-6 shrink-0 items-center justify-center rounded-full border-2 bg-surface"
                      [class.border-positive]="milestoneState(view, index) === 'complete'"
                      [class.bg-positive]="milestoneState(view, index) === 'complete'"
                      [class.border-action]="milestoneState(view, index) === 'active'"
                      [class.border-danger]="milestoneState(view, index) === 'failed'"
                      [class.border-line]="milestoneState(view, index) === 'pending'"
                      [attr.aria-current]="milestoneState(view, index) === 'active' ? 'step' : null"
                    >
                      @if (milestoneState(view, index) === 'complete') {
                        <svg [lucideIcon]="check" class="size-3.5 text-white" aria-hidden="true"></svg>
                      } @else if (milestoneState(view, index) === 'active') {
                        <span class="size-2 rounded-full bg-action" aria-hidden="true"></span>
                      } @else if (milestoneState(view, index) === 'failed') {
                        <span class="size-2 rounded-full bg-danger" aria-hidden="true"></span>
                      }
                    </span>
                    <span>
                      <span class="block text-sm font-semibold text-ink">{{ milestone }}</span>
                      <span class="mt-0.5 block text-xs text-ink-muted">{{ milestoneLabel(view, index) }}</span>
                    </span>
                  </li>
                }
              </ol>
            </aside>
          </div>
        }
      </div>
    </section>

    <ng-template #loadProblem let-title="title" let-description="description" let-retry="retry" let-actionLabel="actionLabel">
      <div class="mx-auto max-w-lg rounded-2xl border border-line bg-surface p-6 text-center shadow-card sm:p-8">
        <span class="mx-auto inline-flex size-12 items-center justify-center rounded-xl bg-caution-subtle text-caution">
          <svg [lucideIcon]="circleAlert" class="size-6" aria-hidden="true"></svg>
        </span>
        <h1 class="mt-5 text-2xl font-semibold tracking-[-0.02em] text-ink">{{ title }}</h1>
        <p class="mt-3 text-sm leading-6 text-ink-muted">{{ description }}</p>
        @if (retry) {
          <button type="button" (click)="store.refresh()" class="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white hover:bg-brand-strong">
            Intentar nuevamente
          </button>
        } @else if (actionLabel) {
          <a routerLink="/onboarding" class="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white hover:bg-brand-strong">
            {{ actionLabel }}
          </a>
        }
      </div>
    </ng-template>
  `
})
export class OnboardingStatusPage {
  private readonly api = inject(OnboardingApiService);
  private readonly destroyRef = inject(DestroyRef);
  private resendIdempotencyKey: string | null = null;
  private cooldownTimer: ReturnType<typeof setInterval> | null = null;

  protected readonly store = inject(OnboardingStatusStore);
  protected readonly milestones = ONBOARDING_MILESTONES;
  protected readonly resendState = signal<ResendState>('idle');
  protected readonly cooldownSeconds = signal(0);
  protected readonly resendAnnouncement = computed(() => {
    if (this.resendState() === 'success') {
      return 'Te enviamos un nuevo correo.';
    }
    if (this.resendState() === 'cooldown') {
      return 'Tenés que esperar antes de pedir otro correo.';
    }
    if (this.resendState() === 'error') {
      return 'No pudimos pedir otro envío. Intentá nuevamente.';
    }
    return '';
  });
  protected readonly resendMessage = computed(() => {
    if (this.resendState() === 'success') {
      return 'Te enviamos un nuevo correo.';
    }
    if (this.resendState() === 'cooldown') {
      return this.cooldownSeconds() > 0
        ? `Esperá ${this.formatCooldown(this.cooldownSeconds())} antes de pedir otro correo.`
        : 'Todavía no podés pedir otro correo. Intentá nuevamente en unos minutos.';
    }
    if (this.resendState() === 'error') {
      return 'No pudimos pedir otro envío. Intentá nuevamente.';
    }
    return '';
  });

  protected readonly check = LucideCheck;
  protected readonly circleAlert = LucideCircleAlert;
  protected readonly circleCheck = LucideCircleCheck;
  protected readonly clock = LucideClock3;
  protected readonly mail = LucideMail;
  protected readonly refresh = LucideRefreshCw;

  constructor() {
    this.destroyRef.onDestroy(() => this.clearCooldownTimer());
  }

  protected resendCredentialInvitation(): void {
    if (this.resendState() === 'loading' || this.resendState() === 'cooldown') {
      return;
    }
    this.resendIdempotencyKey ??= globalThis.crypto.randomUUID();
    this.resendState.set('loading');

    this.api.resendCredentialInvitation(this.resendIdempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (this.resendState() === 'loading') {
            this.resendState.set('idle');
          }
        })
      )
      .subscribe({
        next: () => {
          this.resendIdempotencyKey = null;
          this.resendState.set('success');
        },
        error: (error: unknown) => {
          if (isCredentialInvitationCooldown(error)) {
            const seconds = retryAfterSeconds(error);
            if (seconds !== null && seconds > 0) {
              this.resendState.set('cooldown');
              this.startCooldown(seconds);
            } else {
              this.resendState.set('error');
            }
            return;
          }
          this.resendState.set('error');
        }
      });
  }

  protected milestoneState(view: OnboardingStatusView, index: number): MilestoneState {
    if (index < view.completedMilestones) {
      return 'complete';
    }
    if (view.failedMilestone === index) {
      return 'failed';
    }
    if (view.activeMilestone === index) {
      return 'active';
    }
    return 'pending';
  }

  protected milestoneLabel(view: OnboardingStatusView, index: number): string {
    const state = this.milestoneState(view, index);
    if (state === 'complete') {
      return 'Listo';
    }
    if (state === 'active') {
      return 'En curso';
    }
    if (state === 'failed') {
      return 'No completado';
    }
    return 'Pendiente';
  }

  protected formatCooldown(seconds: number): string {
    return seconds >= 60 ? `${Math.ceil(seconds / 60)} min` : `${seconds} s`;
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

  private startCooldown(seconds: number): void {
    this.clearCooldownTimer();
    const availableAt = Date.now() + seconds * 1_000;
    this.cooldownSeconds.set(seconds);
    this.cooldownTimer = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((availableAt - Date.now()) / 1_000));
      this.cooldownSeconds.set(remaining);
      if (remaining === 0) {
        this.clearCooldownTimer();
        this.resendState.set('idle');
      }
    }, 1_000);
  }

  private clearCooldownTimer(): void {
    if (this.cooldownTimer !== null) {
      clearInterval(this.cooldownTimer);
      this.cooldownTimer = null;
    }
  }
}
