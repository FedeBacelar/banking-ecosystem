import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';

import { isOnboardingStartRateLimited, retryAfterSeconds } from './onboarding-error';

describe('onboarding error presentation', () => {
  it('reads delta-seconds from Retry-After', () => {
    const error = new HttpErrorResponse({
      status: 429,
      headers: new HttpHeaders({ 'Retry-After': '42' })
    });

    expect(retryAfterSeconds(error)).toBe(42);
  });

  it('reads an HTTP date from Retry-After without producing a negative wait', () => {
    const now = Date.parse('2026-07-13T00:00:00Z');
    const error = new HttpErrorResponse({
      status: 429,
      headers: new HttpHeaders({ 'Retry-After': 'Mon, 13 Jul 2026 00:01:00 GMT' })
    });

    expect(retryAfterSeconds(error, now)).toBe(60);
  });

  it('recognizes the stable onboarding start limit contract', () => {
    const error = new HttpErrorResponse({
      status: 429,
      error: { code: 'ONBOARDING_START_RATE_LIMIT' }
    });

    expect(isOnboardingStartRateLimited(error)).toBe(true);
  });
});
