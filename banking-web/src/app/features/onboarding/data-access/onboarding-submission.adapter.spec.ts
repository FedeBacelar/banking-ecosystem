import {
  argentinaPhone,
  digitsOnly,
  toOnboardingSubmission
} from './onboarding-submission.adapter';
import { OnboardingDraft } from '../models/onboarding.models';

describe('onboarding submission adapter', () => {
  it('translates human-facing values to the current API contract', () => {
    const draft: OnboardingDraft = {
      firstName: '  Ana ',
      middleName: ' ',
      lastName: ' Pérez ',
      birthDate: '1995-04-12',
      nationality: 'AR',
      documentNumber: '12.345.678',
      documentValidity: 'NO_DATE',
      documentValidUntil: '',
      nationalPhone: '11 2345 6789',
      street: ' Corrientes ',
      streetNumber: '123',
      province: 'Ciudad Autónoma de Buenos Aires',
      locality: ' Buenos Aires ',
      postalCode: ' c1043aaz ',
      termsAccepted: true
    };

    expect(toOnboardingSubmission(draft)).toEqual({
      firstName: 'Ana',
      middleName: null,
      lastName: 'Pérez',
      birthDate: '1995-04-12',
      nationality: 'AR',
      documentType: 'DNI',
      documentNumber: '12345678',
      documentIssuingCountry: 'AR',
      documentExpirationDate: null,
      phoneNumber: '+541123456789',
      street: 'Corrientes',
      streetNumber: '123',
      city: 'Buenos Aires',
      province: 'Ciudad Autónoma de Buenos Aires',
      postalCode: 'C1043AAZ',
      country: 'AR',
      termsAccepted: true
    });
  });

  it('normalizes pasted DNI and phone values without duplicating the country code', () => {
    expect(digitsOnly('12.345 678')).toBe('12345678');
    expect(argentinaPhone('+54 11 2345-6789')).toBe('+541123456789');
  });

  it('keeps the selected nationality independent from Argentine residence', () => {
    const draft: OnboardingDraft = {
      firstName: 'Ana', middleName: '', lastName: 'Prueba', birthDate: '1990-01-01',
      nationality: 'UY', documentNumber: '12345678', documentValidity: 'NO_DATE',
      documentValidUntil: '', nationalPhone: '11 2345 6789', street: 'Calle',
      streetNumber: '123', province: 'Buenos Aires', locality: 'La Plata',
      postalCode: '1900', termsAccepted: true
    };

    const submission = toOnboardingSubmission(draft);
    expect(submission.nationality).toBe('UY');
    expect(submission.country).toBe('AR');
    expect(submission.documentIssuingCountry).toBe('AR');
  });
});
