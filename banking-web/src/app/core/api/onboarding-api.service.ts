import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, shareReplay, switchMap } from 'rxjs';

import {
  OnboardingApplicantData,
  OnboardingApplicantDataRequest,
  OnboardingApplication,
  OnboardingDocumentCategory,
  OnboardingDocumentReference,
  OnboardingSession,
  OnboardingSubmission,
  OnboardingStatus,
  OnboardingTermsAcceptance
} from '../../features/onboarding/models/onboarding.models';

@Injectable({
  providedIn: 'root'
})
export class OnboardingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/web/onboarding';
  private readonly csrfReady$ = this.http
    .get<{ headerName: string }>('/web/csrf', { withCredentials: true })
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));

  startApplication(email: string): Observable<void> {
    return this.withCsrf(() => this.http.post<void>(
      `${this.baseUrl}/applications`,
      { email },
      { withCredentials: true }
    ));
  }

  consumeMagicLink(token: string): Observable<OnboardingSession> {
    return this.withCsrf(() => this.http.post<OnboardingSession>(
      `${this.baseUrl}/magic-links/consume`,
      { token },
      { withCredentials: true }
    ));
  }

  getSession(): Observable<OnboardingSession> {
    return this.http.get<OnboardingSession>(`${this.baseUrl}/session`, { withCredentials: true });
  }

  clearSession(): Observable<void> {
    return this.withCsrf(() => this.http.delete<void>(`${this.baseUrl}/session`, { withCredentials: true }));
  }

  saveApplicantData(data: OnboardingApplicantDataRequest): Observable<OnboardingApplicantData> {
    return this.withCsrf(() => this.http.put<OnboardingApplicantData>(
      `${this.baseUrl}/applicant-data`,
      data,
      { withCredentials: true }
    ));
  }

  uploadDocument(category: OnboardingDocumentCategory, file: File): Observable<OnboardingDocumentReference> {
    const formData = new FormData();
    formData.append('file', file);

    return this.withCsrf(() => this.http.post<OnboardingDocumentReference>(
      `${this.baseUrl}/documents/${category}`,
      formData,
      { withCredentials: true }
    ));
  }

  acceptTerms(termsVersion: string): Observable<OnboardingTermsAcceptance> {
    return this.withCsrf(() => this.http.put<OnboardingTermsAcceptance>(
      `${this.baseUrl}/terms`,
      { accepted: true, termsVersion },
      { withCredentials: true }
    ));
  }

  submitApplication(): Observable<OnboardingSubmission> {
    return this.withCsrf(() => this.http.post<OnboardingSubmission>(
      `${this.baseUrl}/submissions`, {}, { withCredentials: true }
    ));
  }

  getStatus(): Observable<OnboardingStatus> {
    return this.http.get<OnboardingStatus>(`${this.baseUrl}/status`, { withCredentials: true });
  }

  resendCredentialInvitation(): Observable<OnboardingSubmission> {
    return this.withCsrf(() => this.http.post<OnboardingSubmission>(
      `${this.baseUrl}/credential-invitations/resend`, {}, { withCredentials: true }
    ));
  }

  private withCsrf<T>(request: () => Observable<T>): Observable<T> {
    return this.csrfReady$.pipe(switchMap(() => request()));
  }
}
