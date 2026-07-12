import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../../../core/api/onboarding-api.service';
import { OnboardingShellComponent } from '../../../../shared/ui/onboarding-shell/onboarding-shell.component';
import {
  OnboardingDocumentCategory,
  OnboardingSubmissionRequest
} from '../../models/onboarding.models';
import { httpErrorMessage } from '../../utils/http-error-message';

@Component({
  selector: 'app-onboarding-applicant-data-page',
  imports: [ReactiveFormsModule, OnboardingShellComponent],
  templateUrl: './onboarding-applicant-data.page.html'
})
export class OnboardingApplicantDataPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly onboardingApi = inject(OnboardingApiService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly dniFrontFile = signal<File | null>(null);
  readonly dniBackFile = signal<File | null>(null);

  readonly form = this.formBuilder.group({
    firstName: ['', [Validators.required, Validators.maxLength(120)]],
    middleName: ['', [Validators.maxLength(120)]],
    lastName: ['', [Validators.required, Validators.maxLength(120)]],
    birthDate: ['', [Validators.required]],
    nationality: ['AR', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    documentType: ['DNI', [Validators.required, Validators.pattern(/^DNI$/)]],
    documentNumber: ['', [Validators.required, Validators.maxLength(80)]],
    documentIssuingCountry: ['AR', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    documentExpirationDate: [''],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9][0-9\s-]{6,24}$/)]],
    street: ['', [Validators.required, Validators.maxLength(160)]],
    streetNumber: ['', [Validators.required, Validators.maxLength(40)]],
    city: ['', [Validators.required, Validators.maxLength(120)]],
    province: ['', [Validators.required, Validators.maxLength(120)]],
    postalCode: ['', [Validators.required, Validators.maxLength(30)]],
    country: ['AR', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    termsAccepted: [false, [Validators.requiredTrue]]
  });

  submit(): void {
    this.form.markAllAsTouched();
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (this.form.invalid || !this.dniFrontFile() || !this.dniBackFile()) {
      this.errorMessage.set('Revisa los datos ingresados para continuar.');
      return;
    }

    this.isSubmitting.set(true);

    this.onboardingApi
      .submitApplication(
        this.toRequest(),
        this.dniFrontFile() as File,
        this.dniBackFile() as File
      )
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/onboarding/status']),
        error: (error: unknown) => this.errorMessage.set(httpErrorMessage(error))
      });
  }

  selectDocument(event: Event, category: OnboardingDocumentCategory): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    if (category === 'DNI_FRONT') {
      this.dniFrontFile.set(file);
      return;
    }
    this.dniBackFile.set(file);
  }

  missingDocument(category: OnboardingDocumentCategory): boolean {
    if (category === 'DNI_FRONT') {
      return !this.dniFrontFile() && this.form.touched;
    }
    return !this.dniBackFile() && this.form.touched;
  }

  invalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private toRequest(): OnboardingSubmissionRequest {
    const value = this.form.getRawValue();
    return {
      firstName: this.requiredText(value.firstName),
      middleName: this.optionalText(value.middleName),
      lastName: this.requiredText(value.lastName),
      birthDate: this.requiredText(value.birthDate),
      nationality: this.countryCode(value.nationality),
      documentType: 'DNI',
      documentNumber: this.requiredText(value.documentNumber),
      documentIssuingCountry: this.countryCode(value.documentIssuingCountry),
      documentExpirationDate: value.documentExpirationDate || null,
      phoneNumber: this.requiredText(value.phoneNumber),
      street: this.requiredText(value.street),
      streetNumber: this.requiredText(value.streetNumber),
      city: this.requiredText(value.city),
      province: this.requiredText(value.province),
      postalCode: this.requiredText(value.postalCode),
      country: this.countryCode(value.country),
      termsAccepted: value.termsAccepted === true
    };
  }

  private requiredText(value: string | null | undefined): string {
    return (value ?? '').trim();
  }

  private optionalText(value: string | null | undefined): string | null {
    const normalizedValue = this.requiredText(value);
    return normalizedValue.length > 0 ? normalizedValue : null;
  }

  private countryCode(value: string | null | undefined): string {
    return this.requiredText(value).toUpperCase();
  }
}
