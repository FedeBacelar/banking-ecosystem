import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OnboardingCompletionState } from '../models/onboarding.models';
import { OnboardingCompletionStore } from './onboarding-completion.store';

describe('OnboardingCompletionStore', () => {
  let store: OnboardingCompletionStore;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        OnboardingCompletionStore,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    store = TestBed.inject(OnboardingCompletionStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('polls with backoff until completion and then stops', () => {
    flushStatus('PROCESSING');

    vi.advanceTimersByTime(1_999);
    http.expectNone('/web/onboarding/completion-status');
    vi.advanceTimersByTime(1);
    flushStatus('PROCESSING');

    vi.advanceTimersByTime(3_999);
    http.expectNone('/web/onboarding/completion-status');
    vi.advanceTimersByTime(1);
    flushStatus('COMPLETED');

    expect(store.announcement()).toBe('Tu cuenta está lista.');
    vi.advanceTimersByTime(60_000);
    http.expectNone('/web/onboarding/completion-status');
  });

  it('stops the automatic polling after ten visible minutes', () => {
    flushStatus('PROCESSING');
    const delays = [2_000, 4_000, 8_000, 15_000];
    let elapsed = 0;
    let iteration = 0;

    while (!store.autoUpdateStopped() && elapsed < 600_000 && iteration < 30) {
      const delay = delays[iteration] ?? 30_000;
      const step = Math.min(delay, 600_000 - elapsed);
      vi.advanceTimersByTime(step);
      elapsed += step;

      const requests = http.match('/web/onboarding/completion-status');
      if (requests.length === 1) {
        requests[0].flush(statusResponse('PROCESSING'));
      } else {
        expect(requests).toHaveLength(0);
      }
      iteration += 1;
    }

    expect(store.autoUpdateStopped()).toBe(true);
    expect(elapsed).toBe(600_000);
  });

  it('pauses while offline and resumes when the connection returns', () => {
    flushStatus('PROCESSING');

    window.dispatchEvent(new Event('offline'));
    vi.advanceTimersByTime(5_000);
    http.expectNone('/web/onboarding/completion-status');
    expect(store.isOnline()).toBe(false);

    window.dispatchEvent(new Event('online'));
    flushStatus('PROCESSING');
    expect(store.isOnline()).toBe(true);
  });

  it('pauses in the background without consuming the visible polling budget', () => {
    flushStatus('PROCESSING');
    const visibility = vi.spyOn(document, 'visibilityState', 'get');
    visibility.mockReturnValue('hidden');
    document.dispatchEvent(new Event('visibilitychange'));

    vi.advanceTimersByTime(15 * 60 * 1_000);
    http.expectNone('/web/onboarding/completion-status');

    visibility.mockReturnValue('visible');
    document.dispatchEvent(new Event('visibilitychange'));
    flushStatus('COMPLETED');
    expect(store.autoUpdateStopped()).toBe(false);
    visibility.mockRestore();
  });

  it('turns an unauthorized response into a completion-specific login state', () => {
    http.expectOne('/web/onboarding/completion-status').flush(
      { code: 'AUTHENTICATION_REQUIRED' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(store.status()).toBeNull();
    expect(store.loadState()).toBe('session-required');
    vi.advanceTimersByTime(60_000);
    http.expectNone('/web/onboarding/completion-status');
  });

  it('preserves the last status after a transient error and supports a manual retry', () => {
    flushStatus('PROCESSING');
    vi.advanceTimersByTime(2_000);
    http.expectOne('/web/onboarding/completion-status').flush(
      { code: 'ONBOARDING_SERVICE_UNAVAILABLE' },
      { status: 503, statusText: 'Service Unavailable' }
    );

    expect(store.status()?.status).toBe('PROCESSING');
    expect(store.refreshError()).toBe('No pudimos actualizar. Podés intentar nuevamente.');

    store.refresh();
    flushStatus('COMPLETED');
    expect(store.status()?.status).toBe('COMPLETED');
  });

  function flushStatus(status: OnboardingCompletionState): void {
    http.expectOne('/web/onboarding/completion-status').flush(statusResponse(status));
  }

  function statusResponse(status: OnboardingCompletionState) {
    return {
      status,
      updatedAt: '2026-07-13T00:00:00Z'
    };
  }
});
