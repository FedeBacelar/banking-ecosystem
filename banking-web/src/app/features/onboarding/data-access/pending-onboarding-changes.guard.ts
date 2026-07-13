import { CanDeactivateFn } from '@angular/router';

export interface PendingOnboardingChanges {
  canLeave(): boolean;
}

export const pendingOnboardingChangesGuard: CanDeactivateFn<PendingOnboardingChanges> = (
  component
) => component.canLeave();
