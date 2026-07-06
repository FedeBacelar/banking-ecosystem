export interface OnboardingApplication {
  id: string;
  email: string;
  status: string;
  magicLinkExpiresAt: string;
  emailVerifiedAt: string | null;
  continuationExpiresAt: string | null;
  expiresAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface OnboardingSession {
  active: boolean;
  applicationId: string | null;
  status: string | null;
  continuationExpiresAt: string | null;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
}
