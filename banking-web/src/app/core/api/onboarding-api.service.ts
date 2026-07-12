import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  OnboardingAccess,
  OnboardingSubmission,
  OnboardingSubmissionRequest,
  OnboardingStatus
} from '../../features/onboarding/models/onboarding.models';

@Injectable({
  providedIn: 'root'
})
export class OnboardingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/web/onboarding';

  startApplication(email: string): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/applications`,
      { email },
      { withCredentials: true }
    );
  }

  consumeMagicLink(token: string): Observable<OnboardingAccess> {
    return this.http.post<OnboardingAccess>(
      `${this.baseUrl}/magic-links/consume`,
      { token },
      { withCredentials: true }
    );
  }

  submitApplication(
    submission: OnboardingSubmissionRequest,
    dniFront: File,
    dniBack: File
  ): Observable<OnboardingSubmission> {
    const body = new FormData();
    body.append('submission', new Blob([JSON.stringify(submission)], { type: 'application/json' }));
    body.append('dniFront', dniFront);
    body.append('dniBack', dniBack);

    return this.http.post<OnboardingSubmission>(`${this.baseUrl}/submissions`, body, {
      withCredentials: true
    });
  }

  getStatus(): Observable<OnboardingStatus> {
    return this.http.get<OnboardingStatus>(`${this.baseUrl}/status`, { withCredentials: true });
  }

  resendCredentialInvitation(): Observable<OnboardingSubmission> {
    return this.http.post<OnboardingSubmission>(
      `${this.baseUrl}/credential-invitations/resend`,
      {},
      { withCredentials: true }
    );
  }
}
