import { OnboardingState } from '../models/onboarding.models';
import { ONBOARDING_MILESTONES, presentOnboardingStatus } from './onboarding-status.presenter';

describe('onboarding status presenter', () => {
  const states: readonly OnboardingState[] = [
    'EMAIL_VERIFICATION_PENDING',
    'IN_PROGRESS',
    'SUBMITTED',
    'UNDER_AUTOMATED_REVIEW',
    'REVIEW_FAILED',
    'APPROVED',
    'REJECTED',
    'PROVISIONING',
    'PROVISIONING_FAILED',
    'CREDENTIAL_SETUP_PENDING',
    'CREDENTIAL_SETUP_EXPIRED',
    'CREDENTIAL_SETUP_FAILED',
    'COMPLETED',
    'EXPIRED',
    'CANCELLED'
  ];

  it('has an explicit customer-facing presentation for every public state', () => {
    for (const state of states) {
      const view = presentOnboardingStatus(state);
      expect(view.title).toBeTruthy();
      expect(view.description).toBeTruthy();
      expect(view.title).not.toMatch(/provision|keycloak|KYC|automated|failed/i);
      expect(view.description).not.toMatch(/provision|keycloak|KYC|automated|failed/i);
    }
    expect(ONBOARDING_MILESTONES).toHaveLength(4);
  });

  it('separates a commercial decision from recoverable processing problems', () => {
    expect(presentOnboardingStatus('REJECTED')).toMatchObject({
      title: 'No podemos avanzar con tu solicitud',
      autoPoll: false,
      canRefresh: false,
      completedMilestones: 2,
      failedMilestone: null
    });
    expect(presentOnboardingStatus('REVIEW_FAILED')).toMatchObject({
      title: 'No pudimos terminar la revisión',
      autoPoll: true,
      slowPoll: true,
      canRefresh: true
    });
    expect(presentOnboardingStatus('PROVISIONING_FAILED')).toMatchObject({
      title: 'No pudimos terminar de preparar tu acceso',
      autoPoll: true,
      slowPoll: true,
      canRefresh: true
    });
  });

  it('keeps checking while credential setup can finish in another tab or device', () => {
    expect(presentOnboardingStatus('CREDENTIAL_SETUP_PENDING')).toMatchObject({
      title: 'Creá tu usuario y contraseña',
      autoPoll: true,
      canResendCredentials: true
    });
    expect(presentOnboardingStatus('CREDENTIAL_SETUP_EXPIRED').canRefresh).toBe(false);
    expect(presentOnboardingStatus('CREDENTIAL_SETUP_FAILED').canRefresh).toBe(false);
  });
});
