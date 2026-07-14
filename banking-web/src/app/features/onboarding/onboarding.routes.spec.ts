import { OnboardingCompletionStore } from './data-access/onboarding-completion.store';
import { OnboardingShellComponent } from './layout/onboarding-shell.component';
import { onboardingRoutes } from './onboarding.routes';
import { OnboardingCompletionEntryPage } from './pages/completion-entry/onboarding-completion-entry.page';
import { OnboardingCompletionPage } from './pages/completion/onboarding-completion.page';

describe('onboarding routes', () => {
  it('keeps authenticated completion separate from continuation-cookie status', async () => {
    const route = onboardingRoutes.find((candidate) => candidate.path === 'finalizando')!;

    expect(route.component).toBe(OnboardingShellComponent);
    expect(route.data?.['hideExistingAccountLogin']).toBe(true);
    expect(route.providers).toContain(OnboardingCompletionStore);
    expect(route.children).toHaveLength(1);
    expect(route.children?.[0].path).toBe('');
    expect(await route.children?.[0].loadComponent?.()).toBe(OnboardingCompletionPage);
  });

  it('keeps the old email destination as an exact fixed-navigation alias', async () => {
    const route = onboardingRoutes.find(
      (candidate) => candidate.path === 'credentials-complete'
    )!;

    expect(route.component).toBe(OnboardingShellComponent);
    expect(route.data?.['hideExistingAccountLogin']).toBe(true);
    expect(route.children).toHaveLength(1);
    expect(await route.children?.[0].loadComponent?.()).toBe(
      OnboardingCompletionEntryPage
    );
  });

  it('keeps the regular onboarding wildcard after the two completion routes', () => {
    const regularJourneyIndex = onboardingRoutes.findIndex(
      (candidate) => candidate.path === ''
    );
    const completionIndex = onboardingRoutes.findIndex(
      (candidate) => candidate.path === 'finalizando'
    );
    const aliasIndex = onboardingRoutes.findIndex(
      (candidate) => candidate.path === 'credentials-complete'
    );

    expect(completionIndex).toBeLessThan(regularJourneyIndex);
    expect(aliasIndex).toBeLessThan(regularJourneyIndex);
    expect(
      onboardingRoutes[regularJourneyIndex].children?.at(-1)?.path
    ).toBe('**');
  });
});
