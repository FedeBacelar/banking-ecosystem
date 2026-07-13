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

function problemCode(error: unknown): string | undefined {
  if (!(error instanceof HttpErrorResponse)) {
    return undefined;
  }
  return (error.error as ProblemDetail | null)?.code;
}
