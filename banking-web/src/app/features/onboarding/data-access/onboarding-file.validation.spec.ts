import { validateOnboardingFile } from './onboarding-file.validation';

describe('validateOnboardingFile', () => {
  it('accepts the three supported formats up to 10 MB', () => {
    expect(validateOnboardingFile({ size: 10 * 1024 * 1024, type: 'image/jpeg' })).toBeNull();
    expect(validateOnboardingFile({ size: 20, type: 'image/png' })).toBeNull();
    expect(validateOnboardingFile({ size: 20, type: 'application/pdf' })).toBeNull();
  });

  it('uses a precise error for empty, oversized, and unsupported files', () => {
    expect(validateOnboardingFile({ size: 0, type: 'image/png' })).toBe('El archivo está vacío.');
    expect(validateOnboardingFile({ size: 10 * 1024 * 1024 + 1, type: 'image/png' })).toBe(
      'El archivo supera los 10 MB.'
    );
    expect(validateOnboardingFile({ size: 20, type: 'image/webp' })).toBe(
      'Usá una imagen JPG o PNG, o un PDF.'
    );
  });
});
