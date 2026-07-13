import { NgTemplateOutlet } from '@angular/common';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { form, FormField, maxLength, required } from '@angular/forms/signals';
import {
  LucideCamera,
  LucideCheck,
  LucideChevronLeft,
  LucideDynamicIcon,
  LucideFileText,
  LucideUpload,
  LucideX
} from '@lucide/angular';
import { finalize } from 'rxjs';

import { OnboardingApiService } from '../../data-access/onboarding-api.service';
import { OnboardingDraftStore } from '../../data-access/onboarding-draft.store';
import { validateOnboardingFile } from '../../data-access/onboarding-file.validation';
import {
  isInvalidDocumentError,
  isOnboardingSessionError
} from '../../data-access/onboarding-error';
import { digitsOnly, toOnboardingSubmission } from '../../data-access/onboarding-submission.adapter';
import {
  ARGENTINA_PROVINCES,
  NATIONALITIES,
  OnboardingDraft
} from '../../models/onboarding.models';

type DocumentSide = 'front' | 'back';
type FileErrors = Record<DocumentSide, string | null>;

const STEP_LABELS = ['Sobre vos', 'Tu DNI', 'Contacto', 'Domicilio', 'Revisión'] as const;

@Component({
  selector: 'nb-onboarding-application-page',
  imports: [FormField, LucideDynamicIcon, NgTemplateOutlet, RouterLink],
  template: `
    <section class="py-8 sm:py-12">
      <div class="mx-auto grid w-full max-w-6xl gap-8 px-5 sm:px-6 lg:grid-cols-[15rem_minmax(0,1fr)]">
        <aside class="hidden lg:block" aria-label="Progreso de la solicitud">
          <p class="text-xs font-semibold uppercase tracking-[0.16em] text-ink-muted">Tu solicitud</p>
          <ol class="mt-5 space-y-2">
            @for (label of stepLabels; track label; let index = $index) {
              <li>
                <button
                  type="button"
                  [disabled]="index > furthestStep()"
                  (click)="openStep(index)"
                  class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-semibold disabled:cursor-default"
                  [class.bg-action-soft]="currentStep() === index"
                  [class.text-action]="currentStep() === index"
                  [class.text-ink-muted]="currentStep() !== index"
                >
                  <span
                    class="inline-flex size-7 shrink-0 items-center justify-center rounded-full border text-xs"
                    [class.border-action]="index <= furthestStep()"
                    [class.bg-action]="index < currentStep()"
                    [class.text-white]="index < currentStep()"
                  >
                    @if (index < currentStep()) {
                      <svg [lucideIcon]="check" class="size-4" aria-hidden="true"></svg>
                    } @else {
                      {{ index + 1 }}
                    }
                  </span>
                  {{ label }}
                </button>
              </li>
            }
          </ol>
        </aside>

        <div class="min-w-0">
          <div class="mb-6 lg:hidden" aria-label="Progreso de la solicitud">
            <div class="flex items-center justify-between text-xs font-semibold text-ink-muted">
              <span>Paso {{ currentStep() + 1 }} de 5</span>
              <span>{{ stepLabels[currentStep()] }}</span>
            </div>
            <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-line">
              <div class="h-full rounded-full bg-action transition-[width]" [style.width.%]="(currentStep() + 1) * 20"></div>
            </div>
          </div>

          <form class="rounded-2xl border border-line bg-surface p-5 shadow-card sm:p-8" novalidate (input)="store.markDirty()" (change)="store.markDirty()" (submit)="continue($event)">
            @if (reloadNotice()) {
              <div class="mb-6 rounded-xl border border-action/25 bg-action-soft px-4 py-3 text-sm leading-6 text-ink" role="status">
                La página se volvió a cargar. Por seguridad, completá los datos nuevamente.
              </div>
            }
            @if (currentStep() === 0) {
              <ng-container [ngTemplateOutlet]="personalStep"></ng-container>
            } @else if (currentStep() === 1) {
              <ng-container [ngTemplateOutlet]="documentStep"></ng-container>
            } @else if (currentStep() === 2) {
              <ng-container [ngTemplateOutlet]="contactStep"></ng-container>
            } @else if (currentStep() === 3) {
              <ng-container [ngTemplateOutlet]="addressStep"></ng-container>
            } @else {
              <ng-container [ngTemplateOutlet]="reviewStep"></ng-container>
            }

            @if (pageError()) {
              <div class="mt-6 rounded-xl bg-danger-subtle px-4 py-3 text-sm font-medium text-danger" role="alert" tabindex="-1" id="wizard-page-error">
                {{ pageError() }}
                @if (sessionExpired()) {
                  <a routerLink="/onboarding" class="mt-2 block font-semibold underline">Solicitar un nuevo enlace</a>
                }
              </div>
            }

            <div class="mt-8 flex flex-col-reverse gap-3 border-t border-line pt-6 sm:flex-row sm:items-center sm:justify-between">
              @if (currentStep() > 0) {
                <button type="button" (click)="back()" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold text-brand hover:bg-surface-subtle">
                  <svg [lucideIcon]="chevronLeft" class="size-4" aria-hidden="true"></svg>
                  Atrás
                </button>
              } @else {
                <button type="button" (click)="leave()" class="inline-flex min-h-11 items-center justify-center rounded-xl px-4 text-sm font-semibold text-ink-muted hover:bg-surface-subtle">
                  Salir
                </button>
              }

              <button
                type="submit"
                [disabled]="isSubmitting()"
                [attr.aria-busy]="isSubmitting()"
                class="inline-flex min-h-12 items-center justify-center rounded-xl bg-brand px-6 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong disabled:cursor-wait disabled:opacity-70"
              >
                @if (isSubmitting()) {
                  <span class="nb-spinner mr-2" aria-hidden="true"></span>
                  Enviando solicitud…
                } @else if (currentStep() === 4) {
                  Enviar solicitud
                } @else if (returnToReview()) {
                  Guardar y volver
                } @else {
                  Continuar
                }
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>

    <ng-template #personalStep>
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-action">Sobre vos</p>
      <h1 id="wizard-heading" class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink" tabindex="-1">Contanos sobre vos</h1>
      <p class="mt-3 text-sm leading-6 text-ink-muted">Para abrir una cuenta necesitás ser mayor de 18 años.</p>

      <div class="mt-7 grid gap-5 sm:grid-cols-2">
        <div>
          <label for="first-name" class="nb-label">Nombre</label>
          <input id="first-name" autocomplete="given-name" [formField]="applicationForm.firstName" [attr.aria-invalid]="showFieldError('firstName')" [attr.aria-describedby]="showFieldError('firstName') ? 'first-name-error' : null" class="nb-input" />
          @if (showFieldError('firstName')) { <p id="first-name-error" class="nb-field-error">Ingresá tu nombre.</p> }
        </div>
        <div>
          <label for="middle-name" class="nb-label">Segundo nombre <span class="font-normal text-ink-muted">(opcional)</span></label>
          <input id="middle-name" autocomplete="additional-name" [formField]="applicationForm.middleName" [attr.aria-invalid]="showFieldError('middleName')" [attr.aria-describedby]="showFieldError('middleName') ? 'middle-name-error' : null" class="nb-input" />
          @if (showFieldError('middleName')) { <p id="middle-name-error" class="nb-field-error">El segundo nombre es demasiado largo.</p> }
        </div>
        <div>
          <label for="last-name" class="nb-label">Apellido</label>
          <input id="last-name" autocomplete="family-name" [formField]="applicationForm.lastName" [attr.aria-invalid]="showFieldError('lastName')" [attr.aria-describedby]="showFieldError('lastName') ? 'last-name-error' : null" class="nb-input" />
          @if (showFieldError('lastName')) { <p id="last-name-error" class="nb-field-error">Ingresá tu apellido.</p> }
        </div>
        <div>
          <label for="birth-date" class="nb-label">Fecha de nacimiento</label>
          <input id="birth-date" type="date" autocomplete="bday" [max]="maxEligibleBirthDate" [formField]="applicationForm.birthDate" [attr.aria-invalid]="showFieldError('birthDate')" [attr.aria-describedby]="showFieldError('birthDate') ? 'birth-date-error' : null" class="nb-input" />
          @if (showFieldError('birthDate')) { <p id="birth-date-error" class="nb-field-error">{{ birthDateError() }}</p> }
        </div>
        <div class="sm:col-span-2">
          <label for="nationality" class="nb-label">Nacionalidad</label>
          <select id="nationality" autocomplete="country-name" [formField]="applicationForm.nationality" [attr.aria-invalid]="showFieldError('nationality')" [attr.aria-describedby]="showFieldError('nationality') ? 'nationality-error' : null" class="nb-input">
            @for (option of nationalities; track option.value) { <option [value]="option.value">{{ option.label }}</option> }
          </select>
          @if (showFieldError('nationality')) { <p id="nationality-error" class="nb-field-error">Elegí tu nacionalidad.</p> }
        </div>
      </div>
    </ng-template>

    <ng-template #documentStep>
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-action">Tu DNI</p>
      <h1 id="wizard-heading" class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink" tabindex="-1">Necesitamos tu DNI</h1>
      <p class="mt-3 text-sm leading-6 text-ink-muted">Usá un documento de prueba. Mostrá las cuatro esquinas, con buena luz y sin reflejos.</p>

      <div class="mt-7">
        <label for="document-number" class="nb-label">Número de DNI</label>
        <input id="document-number" inputmode="numeric" autocomplete="off" placeholder="12.345.678" [formField]="applicationForm.documentNumber" [attr.aria-invalid]="showFieldError('documentNumber')" [attr.aria-describedby]="showFieldError('documentNumber') ? 'document-number-error' : null" class="nb-input max-w-sm" />
        @if (showFieldError('documentNumber')) { <p id="document-number-error" class="nb-field-error">Revisá el número de DNI.</p> }
      </div>

      <fieldset id="document-validity" tabindex="-1" [attr.aria-invalid]="showFieldError('documentValidity')" [attr.aria-describedby]="showFieldError('documentValidity') ? 'document-validity-error' : null" class="mt-6">
        <legend class="nb-label">¿Tu DNI muestra una fecha de validez?</legend>
        <div class="mt-2 grid max-w-sm grid-cols-2 gap-3">
          <button type="button" (click)="setDocumentValidity('HAS_DATE')" [attr.aria-pressed]="draft().documentValidity === 'HAS_DATE'" class="nb-choice">Sí</button>
          <button type="button" (click)="setDocumentValidity('NO_DATE')" [attr.aria-pressed]="draft().documentValidity === 'NO_DATE'" class="nb-choice">No</button>
        </div>
        @if (showFieldError('documentValidity')) { <p id="document-validity-error" class="nb-field-error">Elegí una opción.</p> }
      </fieldset>

      @if (draft().documentValidity === 'HAS_DATE') {
        <div class="mt-5 max-w-sm">
          <label for="document-valid-until" class="nb-label">¿Hasta cuándo es válido?</label>
          <input id="document-valid-until" type="date" [min]="today" [formField]="applicationForm.documentValidUntil" [attr.aria-invalid]="showFieldError('documentValidUntil')" [attr.aria-describedby]="showFieldError('documentValidUntil') ? 'document-valid-until-error' : 'document-valid-until-help'" class="nb-input" />
          <p id="document-valid-until-help" class="mt-2 text-xs text-ink-muted">La fecha está en el frente del DNI.</p>
          @if (showFieldError('documentValidUntil')) { <p id="document-valid-until-error" class="nb-field-error">La fecha no puede ser anterior a hoy.</p> }
        </div>
      }

      <div class="mt-7 grid gap-5 sm:grid-cols-2">
        <ng-container [ngTemplateOutlet]="filePicker" [ngTemplateOutletContext]="{ side: 'front', label: 'Frente del DNI', file: store.dniFront(), error: fileErrors().front }"></ng-container>
        <ng-container [ngTemplateOutlet]="filePicker" [ngTemplateOutletContext]="{ side: 'back', label: 'Dorso del DNI', file: store.dniBack(), error: fileErrors().back }"></ng-container>
      </div>
    </ng-template>

    <ng-template #filePicker let-side="side" let-label="label" let-file="file" let-error="error">
      <div class="rounded-xl border border-line p-4" role="group" tabindex="-1" [attr.aria-labelledby]="side + '-document-label'" [attr.aria-describedby]="(error || (showErrors() && !file)) ? side + '-file-error' : null" [attr.aria-invalid]="!!(error || (showErrors() && !file))" [attr.id]="'document-' + side + '-zone'" (dragover)="allowDrop($event)" (drop)="dropFile($event, side)">
        <p class="text-sm font-semibold text-ink" [id]="side + '-document-label'">{{ label }}</p>
        @if (file) {
          <div class="mt-3 flex min-w-0 items-start gap-3 rounded-lg bg-positive-subtle p-3 text-positive">
            <svg [lucideIcon]="fileText" class="mt-0.5 size-5 shrink-0" aria-hidden="true"></svg>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-semibold text-ink">{{ file.name }}</p>
              <p class="mt-0.5 text-xs text-ink-muted">Listo para enviar · {{ formatFileSize(file.size) }}</p>
            </div>
            <button type="button" (click)="removeFile(side)" class="rounded-lg p-1 text-ink-muted hover:bg-white" [attr.aria-label]="'Quitar ' + label.toLowerCase()">
              <svg [lucideIcon]="x" class="size-4" aria-hidden="true"></svg>
            </button>
          </div>
        } @else {
          <div class="mt-3 hidden min-h-24 flex-col items-center justify-center rounded-lg border border-dashed border-line bg-surface-subtle px-3 text-center sm:flex">
            <svg [lucideIcon]="upload" class="size-5 text-action" aria-hidden="true"></svg>
            <p class="mt-2 text-xs text-ink-muted">Arrastrá el archivo acá</p>
          </div>
        }

        <div class="mt-3 grid gap-2 sm:grid-cols-2">
          <label [for]="side + '-camera'" class="inline-flex min-h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-line px-3 text-xs font-semibold text-brand hover:bg-surface-subtle">
            <svg [lucideIcon]="camera" class="size-4" aria-hidden="true"></svg>
            Sacá una foto
          </label>
          <input [id]="side + '-camera'" type="file" accept="image/jpeg,image/png" capture="environment" class="sr-only" [attr.aria-label]="'Sacar una foto para ' + label.toLowerCase()" (change)="selectFile($event, side)" />
          <label [for]="side + '-file'" class="inline-flex min-h-10 cursor-pointer items-center justify-center rounded-lg border border-line px-3 text-xs font-semibold text-brand hover:bg-surface-subtle">
            Elegí un archivo
          </label>
          <input [id]="side + '-file'" type="file" accept="image/jpeg,image/png,application/pdf" class="sr-only" [attr.aria-label]="'Elegir un archivo para ' + label.toLowerCase()" (change)="selectFile($event, side)" />
        </div>
        <p class="mt-2 text-xs text-ink-muted">JPG, PNG o PDF · hasta 10 MB</p>
        @if (error || (showErrors() && !file)) {
          <p [id]="side + '-file-error'" class="nb-field-error" role="alert">{{ error || 'Agregá este lado del DNI.' }}</p>
        }
      </div>
    </ng-template>

    <ng-template #contactStep>
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-action">Contacto</p>
      <h1 id="wizard-heading" class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink" tabindex="-1">¿Cómo podemos contactarte?</h1>
      <div class="mt-7 max-w-md">
        <label for="national-phone" class="nb-label">Número de teléfono</label>
        <div class="mt-2 flex rounded-xl border border-line bg-surface shadow-sm focus-within:border-action">
          <span class="flex min-h-12 items-center border-r border-line px-4 text-sm font-semibold text-ink" aria-hidden="true">+54</span>
          <input id="national-phone" type="tel" inputmode="tel" autocomplete="tel-national" placeholder="11 2345 6789" [formField]="applicationForm.nationalPhone" [attr.aria-invalid]="showFieldError('nationalPhone')" [attr.aria-describedby]="showFieldError('nationalPhone') ? 'national-phone-error' : 'national-phone-help'" class="min-h-12 min-w-0 flex-1 rounded-r-xl px-4 text-ink outline-none" />
        </div>
        <p id="national-phone-help" class="mt-2 text-xs text-ink-muted">Incluí el código de área, sin 0 ni 15.</p>
        @if (showFieldError('nationalPhone')) { <p id="national-phone-error" class="nb-field-error">{{ phoneError() }}</p> }
      </div>
    </ng-template>

    <ng-template #addressStep>
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-action">Domicilio</p>
      <h1 id="wizard-heading" class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink" tabindex="-1">¿Dónde vivís?</h1>
      <p class="mt-3 text-sm leading-6 text-ink-muted">La solicitud está disponible para personas que viven en Argentina.</p>
      <div class="mt-7 grid gap-5 sm:grid-cols-[minmax(0,1fr)_9rem]">
        <div>
          <label for="street" class="nb-label">Calle</label>
          <input id="street" autocomplete="address-line1" [formField]="applicationForm.street" [attr.aria-invalid]="showFieldError('street')" [attr.aria-describedby]="showFieldError('street') ? 'street-error' : null" class="nb-input" />
          @if (showFieldError('street')) { <p id="street-error" class="nb-field-error">Ingresá la calle.</p> }
        </div>
        <div>
          <label for="street-number" class="nb-label">Número</label>
          <input id="street-number" autocomplete="address-line2" placeholder="123 o S/N" [formField]="applicationForm.streetNumber" [attr.aria-invalid]="showFieldError('streetNumber')" [attr.aria-describedby]="showFieldError('streetNumber') ? 'street-number-error' : null" class="nb-input" />
          @if (showFieldError('streetNumber')) { <p id="street-number-error" class="nb-field-error">Ingresá la altura.</p> }
        </div>
        <div class="sm:col-span-2">
          <label for="province" class="nb-label">Provincia</label>
          <select id="province" autocomplete="address-level1" [formField]="applicationForm.province" [attr.aria-invalid]="showFieldError('province')" [attr.aria-describedby]="showFieldError('province') ? 'province-error' : null" class="nb-input">
            <option value="">Elegí una provincia</option>
            @for (option of provinces; track option.value) { <option [value]="option.value">{{ option.label }}</option> }
          </select>
          @if (showFieldError('province')) { <p id="province-error" class="nb-field-error">Elegí una provincia.</p> }
        </div>
        <div>
          <label for="locality" class="nb-label">Localidad</label>
          <input id="locality" autocomplete="address-level2" [formField]="applicationForm.locality" [attr.aria-invalid]="showFieldError('locality')" [attr.aria-describedby]="showFieldError('locality') ? 'locality-error' : null" class="nb-input" />
          @if (showFieldError('locality')) { <p id="locality-error" class="nb-field-error">Ingresá la localidad.</p> }
        </div>
        <div>
          <label for="postal-code" class="nb-label">Código postal</label>
          <input id="postal-code" autocomplete="postal-code" placeholder="1234 o C1234ABC" [formField]="applicationForm.postalCode" [attr.aria-invalid]="showFieldError('postalCode')" [attr.aria-describedby]="showFieldError('postalCode') ? 'postal-code-error' : null" class="nb-input" />
          @if (showFieldError('postalCode')) { <p id="postal-code-error" class="nb-field-error">Ingresá un código postal válido.</p> }
        </div>
      </div>
    </ng-template>

    <ng-template #reviewStep>
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-action">Último paso</p>
      <h1 id="wizard-heading" class="mt-3 text-3xl font-semibold tracking-[-0.03em] text-ink" tabindex="-1">Revisá tu información</h1>
      <div class="mt-7 grid gap-4 sm:grid-cols-2">
        <ng-container [ngTemplateOutlet]="reviewCard" [ngTemplateOutletContext]="{ title: 'Datos personales', step: 0, lines: personalReview() }"></ng-container>
        <ng-container [ngTemplateOutlet]="reviewCard" [ngTemplateOutletContext]="{ title: 'DNI', step: 1, lines: documentReview() }"></ng-container>
        <ng-container [ngTemplateOutlet]="reviewCard" [ngTemplateOutletContext]="{ title: 'Contacto', step: 2, lines: contactReview() }"></ng-container>
        <ng-container [ngTemplateOutlet]="reviewCard" [ngTemplateOutletContext]="{ title: 'Domicilio', step: 3, lines: addressReview() }"></ng-container>
      </div>

      <ng-template #reviewCard let-title="title" let-step="step" let-lines="lines">
        <article class="rounded-xl border border-line p-4">
          <div class="flex items-center justify-between gap-3">
            <h2 class="font-semibold text-ink">{{ title }}</h2>
            <button type="button" (click)="editStep(step)" class="text-sm font-semibold text-brand underline-offset-4 hover:underline">Editar</button>
          </div>
          <div class="mt-3 space-y-1 text-sm leading-6 text-ink-muted">
            @for (line of lines; track line) { <p>{{ line }}</p> }
          </div>
        </article>
      </ng-template>

      <div class="mt-6 rounded-xl border border-caution/30 bg-caution-subtle p-4 text-sm leading-6 text-ink">
        <strong>Documentos de desarrollo.</strong> No corresponden a una operación bancaria real.
      </div>
      <div class="mt-5 rounded-xl border border-line p-4">
        <label class="flex cursor-pointer items-start gap-3">
          <input id="terms-accepted" type="checkbox" [formField]="applicationForm.termsAccepted" [attr.aria-invalid]="showFieldError('termsAccepted')" [attr.aria-describedby]="showFieldError('termsAccepted') ? 'terms-accepted-error' : null" class="mt-1 size-4 rounded border-line text-action" />
          <span class="text-sm leading-6 text-ink">
            Acepto los
            <a routerLink="/legales/terminos" target="_blank" rel="noopener" class="font-semibold text-brand underline">Términos y condiciones</a>.
          </span>
        </label>
        <p class="mt-3 pl-7 text-sm leading-6 text-ink-muted">
          Antes de enviar, revisá cómo tratamos los datos en la
          <a routerLink="/legales/privacidad" target="_blank" rel="noopener" class="font-semibold text-brand underline">Política de privacidad</a>.
        </p>
        @if (showFieldError('termsAccepted')) {
          <p id="terms-accepted-error" class="nb-field-error pl-7">Para enviar la solicitud, aceptá los términos y condiciones.</p>
        }
      </div>
    </ng-template>
  `
})
export class OnboardingApplicationPage {
  private readonly api = inject(OnboardingApiService);
  private readonly router = inject(Router);
  protected readonly store = inject(OnboardingDraftStore);

  protected readonly applicationForm = form(this.store.draft, (path) => {
    required(path.firstName);
    maxLength(path.firstName, 120);
    maxLength(path.middleName, 120);
    required(path.lastName);
    maxLength(path.lastName, 120);
    required(path.birthDate);
    required(path.nationality);
    required(path.documentNumber);
    required(path.nationalPhone);
    required(path.street);
    maxLength(path.street, 160);
    required(path.streetNumber);
    maxLength(path.streetNumber, 40);
    required(path.province);
    maxLength(path.province, 120);
    required(path.locality);
    maxLength(path.locality, 120);
    required(path.postalCode);
    maxLength(path.postalCode, 30);
  });
  protected readonly draft = this.store.draft.asReadonly();
  protected readonly stepLabels = STEP_LABELS;
  protected readonly nationalities = NATIONALITIES;
  protected readonly provinces = ARGENTINA_PROVINCES;
  protected readonly today = localIsoDate(new Date());
  protected readonly maxEligibleBirthDate = yearsAgoIso(18);
  protected readonly currentStep = signal(0);
  protected readonly furthestStep = signal(0);
  protected readonly showErrors = signal(false);
  protected readonly reloadNotice = signal(!this.store.accessGranted());
  protected readonly returnToReview = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly pageError = signal<string | null>(null);
  protected readonly sessionExpired = signal(false);
  protected readonly fileErrors = signal<FileErrors>({ front: null, back: null });
  protected readonly personalReview = computed(() => [
    [this.draft().firstName, this.draft().middleName, this.draft().lastName].filter(Boolean).join(' '),
    formatDate(this.draft().birthDate),
    nationalityLabel(this.draft().nationality)
  ]);
  protected readonly documentReview = computed(() => [
    `DNI ${maskedDocument(this.draft().documentNumber)}`,
    this.draft().documentValidity === 'HAS_DATE'
      ? `Válido hasta ${formatDate(this.draft().documentValidUntil)}`
      : 'Sin fecha de validez informada',
    'Frente listo · Dorso listo'
  ]);
  protected readonly contactReview = computed(() => [`+54 ${this.draft().nationalPhone.trim()}`]);
  protected readonly addressReview = computed(() => [
    `${this.draft().street.trim()} ${this.draft().streetNumber.trim()}`,
    `${this.draft().locality.trim()}, ${this.draft().province}`,
    this.draft().postalCode.trim().toUpperCase()
  ]);

  protected readonly check = LucideCheck;
  protected readonly chevronLeft = LucideChevronLeft;
  protected readonly upload = LucideUpload;
  protected readonly fileText = LucideFileText;
  protected readonly camera = LucideCamera;
  protected readonly x = LucideX;

  protected continue(event: SubmitEvent): void {
    event.preventDefault();
    this.pageError.set(null);
    this.sessionExpired.set(false);
    this.showErrors.set(true);
    if (!this.validStep(this.currentStep())) {
      this.focusFirstInvalid();
      return;
    }

    this.store.markDirty();
    if (this.returnToReview() && this.currentStep() < 4) {
      this.currentStep.set(4);
      this.returnToReview.set(false);
      this.showErrors.set(false);
      this.focusHeading();
      return;
    }
    if (this.currentStep() < 4) {
      const next = this.currentStep() + 1;
      this.currentStep.set(next);
      this.furthestStep.update((value) => Math.max(value, next));
      this.showErrors.set(false);
      this.focusHeading();
      return;
    }
    this.submitApplication();
  }

  protected back(): void {
    if (this.currentStep() === 0) {
      return;
    }
    this.currentStep.update((step) => step - 1);
    this.returnToReview.set(false);
    this.showErrors.set(false);
    this.pageError.set(null);
    this.focusHeading();
  }

  protected openStep(step: number): void {
    if (step > this.furthestStep()) {
      return;
    }
    if (step > this.currentStep()) {
      this.showErrors.set(true);
      if (!this.validStep(this.currentStep())) {
        this.focusFirstInvalid();
        return;
      }
    }
    this.currentStep.set(step);
    this.returnToReview.set(false);
    this.showErrors.set(false);
    this.pageError.set(null);
    this.sessionExpired.set(false);
    this.focusHeading();
  }

  protected editStep(step: number): void {
    this.currentStep.set(step);
    this.returnToReview.set(true);
    this.showErrors.set(false);
    this.focusHeading();
  }

  protected leave(): void {
    if (this.store.dirty() && !window.confirm('¿Querés salir? Si salís ahora, vas a perder los datos que todavía no enviaste.')) {
      return;
    }
    this.store.clearAll();
    void this.router.navigate(['/']);
  }

  canLeave(): boolean {
    return !this.store.dirty()
      || window.confirm('¿Querés salir? Si salís ahora, vas a perder los datos que todavía no enviaste.');
  }

  protected setDocumentValidity(value: 'HAS_DATE' | 'NO_DATE'): void {
    this.store.draft.update((draft) => ({
      ...draft,
      documentValidity: value,
      documentValidUntil: value === 'NO_DATE' ? '' : draft.documentValidUntil
    }));
    this.store.markDirty();
  }

  protected selectFile(event: Event, side: DocumentSide): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    input.value = '';
    if (file) {
      this.acceptFile(file, side);
    }
  }

  protected allowDrop(event: DragEvent): void {
    event.preventDefault();
  }

  protected dropFile(event: DragEvent, side: DocumentSide): void {
    event.preventDefault();
    const file = event.dataTransfer?.files.item(0) ?? null;
    if (file) {
      this.acceptFile(file, side);
    }
  }

  protected removeFile(side: DocumentSide): void {
    this.fileSignal(side).set(null);
    this.fileErrors.update((errors) => ({ ...errors, [side]: null }));
    this.store.markDirty();
  }

  protected formatFileSize(bytes: number): string {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  protected showFieldError(field: keyof OnboardingDraft): boolean {
    if (!this.fieldHasError(field)) {
      return false;
    }
    return this.showErrors() || this.applicationForm[field]().touched();
  }

  protected birthDateError(): string {
    const value = this.draft().birthDate;
    if (!isIsoCalendarDate(value)) {
      return 'Ingresá una fecha válida.';
    }
    return 'Para continuar necesitás ser mayor de 18 años.';
  }

  protected phoneError(): string {
    return this.draft().nationalPhone.trim()
      ? 'Revisá el número. Incluí el código de área.'
      : 'Ingresá un número de teléfono.';
  }

  @HostListener('window:beforeunload', ['$event'])
  protected preventAccidentalExit(event: BeforeUnloadEvent): void {
    if (this.store.dirty()) {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  private submitApplication(): void {
    const front = this.store.dniFront();
    const back = this.store.dniBack();
    if (!front || !back) {
      return;
    }
    this.isSubmitting.set(true);
    this.api
      .submitApplication(toOnboardingSubmission(this.draft()), front, back)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.store.clearAll();
          void this.router.navigate(['/onboarding/solicitud-enviada']);
        },
        error: (error: unknown) => {
          if (isInvalidDocumentError(error)) {
            this.currentStep.set(1);
            this.returnToReview.set(false);
            this.pageError.set('No pudimos aceptar uno de los archivos del DNI. Probá con otro.');
          } else if (isOnboardingSessionError(error)) {
            this.sessionExpired.set(true);
            this.pageError.set('El enlace para completar la solicitud ya no está disponible.');
          } else {
            this.pageError.set('No pudimos enviar la solicitud. Tus datos siguen en esta pantalla; intentá nuevamente.');
          }
          queueMicrotask(() => document.getElementById('wizard-page-error')?.focus());
        }
      });
  }

  private validStep(step: number): boolean {
    if (step === 0) {
      return !(['firstName', 'middleName', 'lastName', 'birthDate', 'nationality'] as const).some((field) => this.fieldHasError(field));
    }
    if (step === 1) {
      return !(['documentNumber', 'documentValidity', 'documentValidUntil'] as const).some((field) => this.fieldHasError(field))
        && this.store.dniFront() !== null
        && this.store.dniBack() !== null;
    }
    if (step === 2) {
      return !this.fieldHasError('nationalPhone');
    }
    if (step === 3) {
      return !(['street', 'streetNumber', 'province', 'locality', 'postalCode'] as const).some((field) => this.fieldHasError(field));
    }
    return !this.fieldHasError('termsAccepted');
  }

  private fieldHasError(field: keyof OnboardingDraft): boolean {
    const value = this.draft()[field];
    if (field === 'firstName' || field === 'lastName') {
      return typeof value !== 'string' || value.trim().length === 0 || value.trim().length > 120;
    }
    if (field === 'middleName') {
      return typeof value !== 'string' || value.trim().length > 120;
    }
    if (field === 'birthDate') {
      return typeof value !== 'string' || !isAdult(value);
    }
    if (field === 'nationality') {
      return typeof value !== 'string'
        || !NATIONALITIES.some((nationality) => nationality.value === value);
    }
    if (field === 'documentNumber') {
      return typeof value !== 'string' || !/^\d{7,8}$/.test(digitsOnly(value));
    }
    if (field === 'documentValidity') {
      return value !== 'HAS_DATE' && value !== 'NO_DATE';
    }
    if (field === 'documentValidUntil') {
      return this.draft().documentValidity === 'HAS_DATE'
        && (typeof value !== 'string' || value < this.today);
    }
    if (field === 'nationalPhone') {
      return typeof value !== 'string' || digitsOnly(value).replace(/^54/, '').length !== 10;
    }
    if (field === 'street') {
      return typeof value !== 'string' || value.trim().length === 0 || value.trim().length > 160;
    }
    if (field === 'streetNumber') {
      return typeof value !== 'string' || value.trim().length === 0 || value.trim().length > 40;
    }
    if (field === 'province') {
      return typeof value !== 'string' || !ARGENTINA_PROVINCES.some((province) => province.value === value);
    }
    if (field === 'locality') {
      return typeof value !== 'string' || value.trim().length === 0 || value.trim().length > 120;
    }
    if (field === 'postalCode') {
      return typeof value !== 'string' || !/^(?:\d{4}|[A-Za-z]\d{4}[A-Za-z]{3})$/.test(value.trim());
    }
    if (field === 'termsAccepted') {
      return value !== true;
    }
    return false;
  }

  private focusFirstInvalid(): void {
    const stepFields: readonly string[] = this.invalidFieldIds();
    queueMicrotask(() => {
      const target = stepFields.map((id) => document.getElementById(id)).find(Boolean);
      (target ?? document.getElementById('wizard-heading'))?.focus();
    });
  }

  private invalidFieldIds(): readonly string[] {
    if (this.currentStep() === 0) {
      return [
        this.fieldHasError('firstName') ? 'first-name' : '',
        this.fieldHasError('middleName') ? 'middle-name' : '',
        this.fieldHasError('lastName') ? 'last-name' : '',
        this.fieldHasError('birthDate') ? 'birth-date' : '',
        this.fieldHasError('nationality') ? 'nationality' : ''
      ].filter(Boolean);
    }
    if (this.currentStep() === 1) {
      return [
        this.fieldHasError('documentNumber') ? 'document-number' : '',
        this.fieldHasError('documentValidity') ? 'document-validity' : '',
        this.fieldHasError('documentValidUntil') ? 'document-valid-until' : '',
        this.store.dniFront() ? '' : 'document-front-zone',
        this.store.dniBack() ? '' : 'document-back-zone'
      ].filter(Boolean);
    }
    if (this.currentStep() === 2) {
      return this.fieldHasError('nationalPhone') ? ['national-phone'] : [];
    }
    if (this.currentStep() === 3) {
      return [
        this.fieldHasError('street') ? 'street' : '',
        this.fieldHasError('streetNumber') ? 'street-number' : '',
        this.fieldHasError('province') ? 'province' : '',
        this.fieldHasError('locality') ? 'locality' : '',
        this.fieldHasError('postalCode') ? 'postal-code' : ''
      ].filter(Boolean);
    }
    return this.fieldHasError('termsAccepted') ? ['terms-accepted'] : [];
  }

  private focusHeading(): void {
    queueMicrotask(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      document.getElementById('wizard-heading')?.focus();
    });
  }

  private acceptFile(file: File, side: DocumentSide): void {
    const error = validateOnboardingFile(file);
    if (error) {
      this.fileErrors.update((errors) => ({ ...errors, [side]: error }));
      return;
    }
    this.fileSignal(side).set(file);
    this.fileErrors.update((errors) => ({ ...errors, [side]: null }));
    this.store.markDirty();
  }

  private fileSignal(side: DocumentSide) {
    return side === 'front' ? this.store.dniFront : this.store.dniBack;
  }
}

function isAdult(value: string): boolean {
  if (!isIsoCalendarDate(value)) {
    return false;
  }
  return value <= yearsAgoIso(18);
}

function isIsoCalendarDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }
  const date = new Date(`${value}T12:00:00`);
  return !Number.isNaN(date.getTime()) && localIsoDate(date) === value;
}

function yearsAgoIso(years: number): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  return localIsoDate(date);
}

function localIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDate(value: string): string {
  if (!value) {
    return '';
  }
  return new Intl.DateTimeFormat('es-AR').format(new Date(`${value}T12:00:00`));
}

function maskedDocument(value: string): string {
  const digits = digitsOnly(value);
  return `••.${digits.slice(-3)}`;
}

function nationalityLabel(value: string): string {
  return NATIONALITIES.find((option) => option.value === value)?.label ?? value;
}
