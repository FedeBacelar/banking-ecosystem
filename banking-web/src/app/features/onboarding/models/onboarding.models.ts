export type OnboardingState =
  | 'EMAIL_VERIFICATION_PENDING'
  | 'IN_PROGRESS'
  | 'SUBMITTED'
  | 'UNDER_AUTOMATED_REVIEW'
  | 'REVIEW_FAILED'
  | 'APPROVED'
  | 'REJECTED'
  | 'PROVISIONING'
  | 'PROVISIONING_FAILED'
  | 'CREDENTIAL_SETUP_PENDING'
  | 'CREDENTIAL_SETUP_EXPIRED'
  | 'CREDENTIAL_SETUP_FAILED'
  | 'COMPLETED'
  | 'EXPIRED'
  | 'CANCELLED';

export type OnboardingNextAction =
  | 'CONTINUE_APPLICATION'
  | 'WAIT'
  | 'CHECK_EMAIL'
  | 'LOGIN'
  | 'CONTACT_SUPPORT'
  | 'START_NEW_APPLICATION';

export interface OnboardingAccess {
  status: OnboardingState;
  nextAction: OnboardingNextAction;
}

export interface OnboardingSubmission {
  applicationId: string;
  status: OnboardingState;
  submittedAt: string;
  updatedAt: string;
}

export interface OnboardingStatus {
  applicationId: string;
  status: OnboardingState;
  nextAction: OnboardingNextAction;
  updatedAt: string;
}

export interface OnboardingApplicantDataRequest {
  firstName: string;
  middleName?: string | null;
  lastName: string;
  birthDate: string;
  nationality: string;
  documentType: 'DNI';
  documentNumber: string;
  documentIssuingCountry: string;
  documentExpirationDate?: string | null;
  phoneNumber: string;
  street: string;
  streetNumber: string;
  city: string;
  province: string;
  postalCode: string;
  country: string;
}

export interface OnboardingSubmissionRequest extends OnboardingApplicantDataRequest {
  termsAccepted: boolean;
}

export type OnboardingDocumentCategory = 'DNI_FRONT' | 'DNI_BACK';

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  missingSections?: string[];
}
