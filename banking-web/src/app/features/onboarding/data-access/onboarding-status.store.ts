import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { OnboardingStatus } from '../models/onboarding.models';
import { isOnboardingSessionError } from './onboarding-error';
import { OnboardingApiService } from './onboarding-api.service';
import { presentOnboardingStatus } from './onboarding-status.presenter';

export type OnboardingStatusLoadState = 'loading' | 'ready' | 'offline' | 'error' | 'session-required';

const POLL_DELAYS_MS = [2_000, 4_000, 8_000, 15_000, 30_000] as const;
const MAX_VISIBLE_POLLING_MS = 10 * 60 * 1_000;

@Injectable()
export class OnboardingStatusStore {
  private readonly api = inject(OnboardingApiService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly browser = this.document.defaultView;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private inFlight = false;
  private delayIndex = 0;
  private visibleElapsedMs = 0;
  private visibleSince: number | null = null;
  private lastRequestAt = 0;

  readonly status = signal<OnboardingStatus | null>(null);
  readonly loadState = signal<OnboardingStatusLoadState>('loading');
  readonly isRefreshing = signal(false);
  readonly isOnline = signal(this.browser?.navigator.onLine ?? true);
  readonly autoUpdateStopped = signal(false);
  readonly refreshError = signal<string | null>(null);
  readonly announcement = signal('');
  readonly view = computed(() => {
    const current = this.status();
    return current ? presentOnboardingStatus(current.status) : null;
  });

  constructor() {
    if (this.isVisible() && this.isOnline()) {
      this.visibleSince = Date.now();
    }
    this.browser?.addEventListener('online', this.onOnline);
    this.browser?.addEventListener('offline', this.onOffline);
    this.document.addEventListener('visibilitychange', this.onVisibilityChange);
    this.destroyRef.onDestroy(() => this.destroy());
    this.requestStatus('initial');
  }

  refresh(): void {
    this.requestStatus(this.status() ? 'manual' : 'initial');
  }

  private readonly onOnline = (): void => {
    this.isOnline.set(true);
    this.refreshError.set(null);
    if (this.isVisible() && this.visibleSince === null && !this.autoUpdateStopped()) {
      this.visibleSince = Date.now();
    }
    if (!this.status()) {
      this.requestStatus('initial');
      return;
    }
    if (this.loadState() === 'ready' && this.view()?.autoPoll && !this.autoUpdateStopped()) {
      this.resumeAutomaticUpdates();
    }
  };

  private readonly onOffline = (): void => {
    this.isOnline.set(false);
    this.clearTimer();
    this.freezeVisibleBudget();
    if (!this.status()) {
      this.loadState.set('offline');
    }
  };

  private readonly onVisibilityChange = (): void => {
    if (!this.isVisible()) {
      this.freezeVisibleBudget();
      this.clearTimer();
      return;
    }
    if (this.isOnline() && this.visibleSince === null && !this.autoUpdateStopped()) {
      this.visibleSince = Date.now();
    }
    if (this.isOnline() && this.loadState() === 'ready' && this.view()?.autoPoll && !this.autoUpdateStopped()) {
      this.resumeAutomaticUpdates();
    }
  };

  private requestStatus(trigger: 'initial' | 'manual' | 'automatic'): void {
    if (this.inFlight) {
      return;
    }
    if (!this.isOnline()) {
      if (trigger === 'initial') {
        this.loadState.set('offline');
      } else {
        this.refreshError.set('Sin conexión. Cuando la recuperes, vamos a actualizar el estado.');
      }
      return;
    }

    this.clearTimer();
    this.inFlight = true;
    this.lastRequestAt = Date.now();
    if (trigger === 'initial') {
      this.loadState.set('loading');
    } else {
      this.isRefreshing.set(true);
      this.refreshError.set(null);
    }

    this.api.getStatus()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.inFlight = false;
          this.isRefreshing.set(false);
        })
      )
      .subscribe({
        next: (status) => this.handleStatus(status),
        error: (error: unknown) => this.handleError(error)
      });
  }

  private handleStatus(status: OnboardingStatus): void {
    const previousState = this.status()?.status ?? null;
    const changed = previousState !== null && previousState !== status.status;
    this.status.set(status);
    this.loadState.set('ready');
    this.refreshError.set(null);

    if (changed) {
      this.delayIndex = 0;
      this.announcement.set(presentOnboardingStatus(status.status).title);
    }

    if (presentOnboardingStatus(status.status).autoPoll && !this.autoUpdateStopped()) {
      this.scheduleNextPoll();
    } else {
      this.clearTimer();
    }
  }

  private handleError(error: unknown): void {
    if (isOnboardingSessionError(error)) {
      this.clearTimer();
      this.status.set(null);
      this.refreshError.set(null);
      this.loadState.set('session-required');
      return;
    }
    if (this.status()) {
      this.refreshError.set('No pudimos actualizar el estado. Podés intentar nuevamente.');
      if (this.view()?.autoPoll && !this.autoUpdateStopped()) {
        this.scheduleNextPoll();
      }
      return;
    }
    this.loadState.set('error');
  }

  private scheduleNextPoll(): void {
    this.clearTimer();
    if (!this.isVisible() || !this.isOnline()) {
      return;
    }
    const remaining = this.remainingVisibleBudget();
    if (remaining <= 0) {
      this.stopAutomaticUpdates();
      return;
    }
    const delay = this.view()?.slowPoll
      ? POLL_DELAYS_MS[POLL_DELAYS_MS.length - 1]
      : POLL_DELAYS_MS[Math.min(this.delayIndex, POLL_DELAYS_MS.length - 1)];
    if (!this.view()?.slowPoll) {
      this.delayIndex += 1;
    }
    if (delay >= remaining) {
      this.timer = setTimeout(() => this.stopAutomaticUpdates(), remaining);
      return;
    }
    this.timer = setTimeout(() => {
      if (this.remainingVisibleBudget() <= 0) {
        this.stopAutomaticUpdates();
        return;
      }
      this.requestStatus('automatic');
    }, delay);
  }

  private resumeAutomaticUpdates(): void {
    if (Date.now() - this.lastRequestAt < POLL_DELAYS_MS[0]) {
      this.scheduleNextPoll();
      return;
    }
    this.requestStatus('automatic');
  }

  private remainingVisibleBudget(): number {
    const currentVisiblePeriod = this.visibleSince === null ? 0 : Date.now() - this.visibleSince;
    return MAX_VISIBLE_POLLING_MS - this.visibleElapsedMs - currentVisiblePeriod;
  }

  private freezeVisibleBudget(): void {
    if (this.visibleSince === null) {
      return;
    }
    this.visibleElapsedMs += Date.now() - this.visibleSince;
    this.visibleSince = null;
  }

  private stopAutomaticUpdates(): void {
    this.freezeVisibleBudget();
    this.autoUpdateStopped.set(true);
    this.clearTimer();
  }

  private isVisible(): boolean {
    return this.document.visibilityState !== 'hidden';
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  private destroy(): void {
    this.clearTimer();
    this.browser?.removeEventListener('online', this.onOnline);
    this.browser?.removeEventListener('offline', this.onOffline);
    this.document.removeEventListener('visibilitychange', this.onVisibilityChange);
  }
}
