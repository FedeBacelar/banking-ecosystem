import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from '../models/onboarding.models';

export type MagicLinkError = 'invalid' | 'expired' | 'used' | 'unavailable';

export function magicLinkError(error: unknown): MagicLinkError {
  const code = problemCode(error);
  if (code === 'ONBOARDING_MAGIC_LINK_EXPIRED') {
    return 'expired';
  }
  if (code === 'ONBOARDING_MAGIC_LINK_ALREADY_CONSUMED') {
    return 'used';
  }
  if (code === 'INVALID_MAGIC_LINK_TOKEN') {
    return 'invalid';
  }
  return 'unavailable';
}

export function isInvalidDocumentError(error: unknown): boolean {
  return [
    'INVALID_ONBOARDING_DOCUMENT',
    'INVALID_ONBOARDING_MULTIPART',
    'ONBOARDING_DOCUMENT_TOO_LARGE'
  ].includes(problemCode(error) ?? '');
}

export function isOnboardingSessionError(error: unknown): boolean {
  return ['ONBOARDING_SESSION_REQUIRED', 'ONBOARDING_CONTINUATION_EXPIRED'].includes(
    problemCode(error) ?? ''
  );
}

export function isCredentialInvitationCooldown(error: unknown): boolean {
  return error instanceof HttpErrorResponse
    && (error.status === 429 || problemCode(error) === 'CREDENTIAL_INVITATION_COOLDOWN');
}

export function isOnboardingStartRateLimited(error: unknown): boolean {
  return error instanceof HttpErrorResponse
    && (error.status === 429 || problemCode(error) === 'ONBOARDING_START_RATE_LIMIT');
}

export function retryAfterSeconds(error: unknown, now = Date.now()): number | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }
  const value = error.headers.get('Retry-After')?.trim();
  if (!value) {
    return null;
  }
  if (/^\d+$/.test(value)) {
    return Number(value);
  }
  const retryAt = Date.parse(value);
  if (Number.isNaN(retryAt)) {
    return null;
  }
  return Math.max(0, Math.ceil((retryAt - now) / 1000));
}

function problemCode(error: unknown): string | undefined {
  if (!(error instanceof HttpErrorResponse)) {
    return undefined;
  }
  return (error.error as ProblemDetail | null)?.code;
}
