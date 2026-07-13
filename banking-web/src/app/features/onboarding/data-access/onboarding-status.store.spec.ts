import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OnboardingState } from '../models/onboarding.models';
import { OnboardingStatusStore } from './onboarding-status.store';

describe('OnboardingStatusStore', () => {
  let store: OnboardingStatusStore;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [OnboardingStatusStore, provideHttpClient(), provideHttpClientTesting()]
    });
    store = TestBed.inject(OnboardingStatusStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('polls with progressive delays and resets the delay when the state advances', () => {
    flushStatus('SUBMITTED');

    vi.advanceTimersByTime(1_999);
    http.expectNone('/web/onboarding/status');
    vi.advanceTimersByTime(1);
    flushStatus('SUBMITTED');

    vi.advanceTimersByTime(3_999);
    http.expectNone('/web/onboarding/status');
    vi.advanceTimersByTime(1);
    flushStatus('PROVISIONING');

    vi.advanceTimersByTime(1_999);
    http.expectNone('/web/onboarding/status');
    vi.advanceTimersByTime(1);
    flushStatus('PROVISIONING');
  });

  it('stops polling after reaching a terminal state', () => {
    flushStatus('SUBMITTED');
    vi.advanceTimersByTime(2_000);
    flushStatus('COMPLETED');

    vi.advanceTimersByTime(60_000);

    http.expectNone('/web/onboarding/status');
    expect(store.view()?.title).toBe('Tu cuenta está lista');
  });

  it('pauses while offline and refreshes when the connection returns', () => {
    flushStatus('SUBMITTED');

    window.dispatchEvent(new Event('offline'));
    vi.advanceTimersByTime(2_000);
    http.expectNone('/web/onboarding/status');
    expect(store.isOnline()).toBe(false);

    window.dispatchEvent(new Event('online'));
    flushStatus('SUBMITTED');
    expect(store.isOnline()).toBe(true);
  });

  it('does not consume the automatic update budget during a long disconnection', () => {
    flushStatus('SUBMITTED');

    window.dispatchEvent(new Event('offline'));
    vi.advanceTimersByTime(15 * 60 * 1_000);
    window.dispatchEvent(new Event('online'));
    flushStatus('SUBMITTED');

    expect(store.autoUpdateStopped()).toBe(false);
    vi.advanceTimersByTime(4_000);
    flushStatus('SUBMITTED');
  });

  it('does not overlap requests and preserves the last known state after a transient error', () => {
    const initial = http.expectOne('/web/onboarding/status');
    store.refresh();
    http.expectNone('/web/onboarding/status');
    initial.flush(statusResponse('SUBMITTED'));

    vi.advanceTimersByTime(2_000);
    http.expectOne('/web/onboarding/status').flush(
      { code: 'ONBOARDING_SERVICE_UNAVAILABLE' },
      { status: 503, statusText: 'Service Unavailable' }
    );

    expect(store.status()?.status).toBe('SUBMITTED');
    expect(store.refreshError()).toBe(
      'No pudimos actualizar el estado. Podés intentar nuevamente.'
    );
  });

  it('stops polling and asks for a new link when the continuation expires', () => {
    flushStatus('SUBMITTED');
    vi.advanceTimersByTime(2_000);
    http.expectOne('/web/onboarding/status').flush(
      { code: 'ONBOARDING_CONTINUATION_EXPIRED' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(store.status()).toBeNull();
    expect(store.loadState()).toBe('session-required');
    vi.advanceTimersByTime(30_000);
    http.expectNone('/web/onboarding/status');
  });

  it('shows the initial loading state again while retrying a failed first request', () => {
    http.expectOne('/web/onboarding/status').flush(
      { code: 'ONBOARDING_SERVICE_UNAVAILABLE' },
      { status: 503, statusText: 'Service Unavailable' }
    );
    expect(store.loadState()).toBe('error');

    store.refresh();

    expect(store.loadState()).toBe('loading');
    flushStatus('SUBMITTED');
    expect(store.loadState()).toBe('ready');
  });

  function flushStatus(status: OnboardingState): void {
    http.expectOne('/web/onboarding/status').flush(statusResponse(status));
  }

  function statusResponse(status: OnboardingState) {
    return {
      applicationId: 'not-rendered',
      status,
      nextAction: 'WAIT',
      updatedAt: '2026-07-13T00:00:00Z'
    };
  }
});
