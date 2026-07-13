import {
  OnboardingDraft,
  OnboardingSubmissionRequest
} from '../models/onboarding.models';

export function toOnboardingSubmission(draft: OnboardingDraft): OnboardingSubmissionRequest {
  return {
    firstName: draft.firstName.trim(),
    middleName: optionalText(draft.middleName),
    lastName: draft.lastName.trim(),
    birthDate: draft.birthDate,
    nationality: draft.nationality,
    documentType: 'DNI',
    documentNumber: digitsOnly(draft.documentNumber),
    documentIssuingCountry: 'AR',
    documentExpirationDate:
      draft.documentValidity === 'HAS_DATE' ? draft.documentValidUntil : null,
    phoneNumber: argentinaPhone(draft.nationalPhone),
    street: draft.street.trim(),
    streetNumber: draft.streetNumber.trim(),
    city: draft.locality.trim(),
    province: draft.province,
    postalCode: draft.postalCode.trim().toUpperCase(),
    country: 'AR',
    termsAccepted: draft.termsAccepted
  };
}

export function digitsOnly(value: string): string {
  return value.replace(/\D/g, '');
}

export function argentinaPhone(value: string): string {
  const digits = digitsOnly(value);
  const withoutCountryCode = digits.startsWith('54') ? digits.slice(2) : digits;
  return `+54${withoutCountryCode}`;
}

function optionalText(value: string): string | null {
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : null;
}
