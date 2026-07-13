import { Injectable, signal } from '@angular/core';

import {
  EMPTY_ONBOARDING_DRAFT,
  OnboardingDraft
} from '../models/onboarding.models';

@Injectable()
export class OnboardingDraftStore {
  readonly email = signal<string | null>(null);
  readonly accessGranted = signal(false);
  readonly draft = signal<OnboardingDraft>({ ...EMPTY_ONBOARDING_DRAFT });
  readonly dniFront = signal<File | null>(null);
  readonly dniBack = signal<File | null>(null);
  readonly dirty = signal(false);

  markDirty(): void {
    this.dirty.set(true);
  }

  resetApplication(): void {
    this.draft.set({ ...EMPTY_ONBOARDING_DRAFT });
    this.dniFront.set(null);
    this.dniBack.set(null);
    this.dirty.set(false);
  }

  clearAll(): void {
    this.email.set(null);
    this.accessGranted.set(false);
    this.resetApplication();
  }
}
