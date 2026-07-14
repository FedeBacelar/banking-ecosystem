import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import {
  OnboardingCompletionState,
  OnboardingCompletionStatus
} from '../models/onboarding.models';
import { OnboardingApiService } from './onboarding-api.service';

export type OnboardingCompletionLoadState =
  | 'loading'
  | 'ready'
  | 'offline'
  | 'error'
  | 'session-required';

const POLL_DELAYS_MS = [2_000, 4_000, 8_000, 15_000, 30_000] as const;
const MAX_VISIBLE_POLLING_MS = 10 * 60 * 1_000;

const STATUS_ANNOUNCEMENTS: Record<OnboardingCompletionState, string> = {
  PROCESSING: 'Estamos terminando de preparar tu cuenta.',
  COMPLETED: 'Tu cuenta está lista.',
  FAILED: 'No pudimos terminar de preparar tu cuenta.'
};

@Injectable()
export class OnboardingCompletionStore {
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

  readonly status = signal<OnboardingCompletionStatus | null>(null);
  readonly loadState = signal<OnboardingCompletionLoadState>('loading');
  readonly isRefreshing = signal(false);
  readonly isOnline = signal(this.browser?.navigator.onLine ?? true);
  readonly autoUpdateStopped = signal(false);
  readonly refreshError = signal<string | null>(null);
  readonly announcement = signal('');

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
    if (this.shouldAutoPoll()) {
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
    if (this.isOnline() && this.shouldAutoPoll()) {
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
        this.refreshError.set('Sin conexión. Cuando la recuperes, vamos a continuar.');
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

    this.api.getCompletionStatus()
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

  private handleStatus(status: OnboardingCompletionStatus): void {
    const previousState = this.status()?.status ?? null;
    this.status.set(status);
    this.loadState.set('ready');
    this.refreshError.set(null);

    if (previousState !== status.status) {
      this.delayIndex = 0;
      this.announcement.set(STATUS_ANNOUNCEMENTS[status.status]);
    }

    if (this.shouldAutoPoll()) {
      this.scheduleNextPoll();
    } else {
      this.clearTimer();
    }
  }

  private handleError(error: unknown): void {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      this.clearTimer();
      this.status.set(null);
      this.refreshError.set(null);
      this.loadState.set('session-required');
      return;
    }
    if (this.status()) {
      this.refreshError.set('No pudimos actualizar. Podés intentar nuevamente.');
      if (this.shouldAutoPoll()) {
        this.scheduleNextPoll();
      }
      return;
    }
    this.loadState.set('error');
  }

  private shouldAutoPoll(): boolean {
    return this.loadState() === 'ready'
      && this.status()?.status === 'PROCESSING'
      && !this.autoUpdateStopped();
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
    const delay = POLL_DELAYS_MS[Math.min(this.delayIndex, POLL_DELAYS_MS.length - 1)];
    this.delayIndex += 1;
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
    const currentVisiblePeriod = this.visibleSince === null
      ? 0
      : Date.now() - this.visibleSince;
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
