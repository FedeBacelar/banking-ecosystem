import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  OnboardingApplication,
  OnboardingSession
} from '../../features/onboarding/models/onboarding.models';

@Injectable({
  providedIn: 'root'
})
export class OnboardingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/web/onboarding';

  startApplication(email: string): Observable<OnboardingApplication> {
    return this.http.post<OnboardingApplication>(
      `${this.baseUrl}/applications`,
      { email },
      { withCredentials: true }
    );
  }

  consumeMagicLink(token: string): Observable<OnboardingSession> {
    return this.http.post<OnboardingSession>(
      `${this.baseUrl}/magic-links/consume`,
      { token },
      { withCredentials: true }
    );
  }

  getSession(): Observable<OnboardingSession> {
    return this.http.get<OnboardingSession>(`${this.baseUrl}/session`, { withCredentials: true });
  }

  clearSession(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/session`, { withCredentials: true });
  }
}
