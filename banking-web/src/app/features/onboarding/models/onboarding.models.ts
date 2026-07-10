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

export interface OnboardingApplicantDataRequest {
  firstName: string;
  middleName?: string | null;
  lastName: string;
  birthDate: string;
  nationality: string;
  documentType: string;
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

export interface OnboardingApplicantData extends OnboardingApplicantDataRequest {
  applicationId: string;
  createdAt: string;
  updatedAt: string;
}

export type OnboardingDocumentCategory = 'DNI_FRONT' | 'DNI_BACK';

export interface OnboardingDocumentReference {
  id: string;
  applicationId: string;
  category: OnboardingDocumentCategory;
  documentId: string;
  createdAt: string;
  updatedAt: string;
}

export interface OnboardingTermsAcceptance {
  applicationId: string;
  termsVersion: string;
  acceptedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
}
