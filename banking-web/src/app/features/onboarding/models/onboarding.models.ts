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

export interface OnboardingStatus {
  applicationId: string;
  status: OnboardingState;
  nextAction: OnboardingNextAction;
  updatedAt: string;
}

export interface OnboardingSubmission {
  applicationId: string;
  status: OnboardingState;
  submittedAt: string;
  updatedAt: string;
}

export interface ProblemDetail {
  status?: number;
  code?: string;
}

export interface CatalogOption {
  value: string;
  label: string;
}

export type DocumentValidity = '' | 'HAS_DATE' | 'NO_DATE';

export interface OnboardingDraft {
  firstName: string;
  middleName: string;
  lastName: string;
  birthDate: string;
  nationality: string;
  documentNumber: string;
  documentValidity: DocumentValidity;
  documentValidUntil: string;
  nationalPhone: string;
  street: string;
  streetNumber: string;
  province: string;
  locality: string;
  postalCode: string;
  termsAccepted: boolean;
}

export interface OnboardingSubmissionRequest {
  firstName: string;
  middleName: string | null;
  lastName: string;
  birthDate: string;
  nationality: string;
  documentType: 'DNI';
  documentNumber: string;
  documentIssuingCountry: 'AR';
  documentExpirationDate: string | null;
  phoneNumber: string;
  street: string;
  streetNumber: string;
  city: string;
  province: string;
  postalCode: string;
  country: 'AR';
  termsAccepted: boolean;
}

export const EMPTY_ONBOARDING_DRAFT: OnboardingDraft = {
  firstName: '',
  middleName: '',
  lastName: '',
  birthDate: '',
  nationality: 'AR',
  documentNumber: '',
  documentValidity: '',
  documentValidUntil: '',
  nationalPhone: '',
  street: '',
  streetNumber: '',
  province: '',
  locality: '',
  postalCode: '',
  termsAccepted: false
};

const ISO_COUNTRY_CODES = `
AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR
BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC
EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK
HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB
LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ
NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU
RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN
TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW
`.trim().split(/\s+/);

const countryNames = new Intl.DisplayNames(['es-AR'], { type: 'region' });

export const NATIONALITIES: readonly CatalogOption[] = ISO_COUNTRY_CODES
  .map((value) => ({ value, label: countryNames.of(value) ?? value }))
  .sort((left, right) => left.label.localeCompare(right.label, 'es-AR'));

export const ARGENTINA_PROVINCES: readonly CatalogOption[] = [
  { value: 'Buenos Aires', label: 'Buenos Aires' },
  { value: 'Ciudad Autónoma de Buenos Aires', label: 'Ciudad Autónoma de Buenos Aires' },
  { value: 'Catamarca', label: 'Catamarca' },
  { value: 'Chaco', label: 'Chaco' },
  { value: 'Chubut', label: 'Chubut' },
  { value: 'Córdoba', label: 'Córdoba' },
  { value: 'Corrientes', label: 'Corrientes' },
  { value: 'Entre Ríos', label: 'Entre Ríos' },
  { value: 'Formosa', label: 'Formosa' },
  { value: 'Jujuy', label: 'Jujuy' },
  { value: 'La Pampa', label: 'La Pampa' },
  { value: 'La Rioja', label: 'La Rioja' },
  { value: 'Mendoza', label: 'Mendoza' },
  { value: 'Misiones', label: 'Misiones' },
  { value: 'Neuquén', label: 'Neuquén' },
  { value: 'Río Negro', label: 'Río Negro' },
  { value: 'Salta', label: 'Salta' },
  { value: 'San Juan', label: 'San Juan' },
  { value: 'San Luis', label: 'San Luis' },
  { value: 'Santa Cruz', label: 'Santa Cruz' },
  { value: 'Santa Fe', label: 'Santa Fe' },
  { value: 'Santiago del Estero', label: 'Santiago del Estero' },
  { value: 'Tierra del Fuego, Antártida e Islas del Atlántico Sur', label: 'Tierra del Fuego' },
  { value: 'Tucumán', label: 'Tucumán' }
];
