import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from '../models/onboarding.models';

export function httpErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | undefined;

    switch (problem?.code) {
      case 'DUPLICATE_ACTIVE_ONBOARDING_APPLICATION':
        return 'Ya existe una solicitud activa para este email.';
      case 'INVALID_MAGIC_LINK_TOKEN':
      case 'INVALID_CONTINUATION_TOKEN':
        return 'El enlace no es válido. Solicitá uno nuevo para continuar.';
      case 'ONBOARDING_MAGIC_LINK_EXPIRED':
      case 'ONBOARDING_CONTINUATION_EXPIRED':
        return 'El enlace venció. Solicitá uno nuevo para continuar.';
      case 'ONBOARDING_MAGIC_LINK_ALREADY_CONSUMED':
        return 'Este enlace ya fue utilizado.';
      case 'VALIDATION_ERROR':
        return 'Revisá los datos ingresados e intentá nuevamente.';
      default:
        return statusMessage(error.status);
    }
  }

  return statusMessage();
}

function statusMessage(status?: number): string {
  switch (status) {
    case 0:
      return 'No pudimos conectarnos. Intentá nuevamente en unos minutos.';
    case 400:
      return 'No pudimos validar la solicitud. Revisá los datos e intentá nuevamente.';
    case 409:
      return 'No pudimos continuar con esta solicitud. Revisá el estado del trámite o solicitá un nuevo enlace.';
    case 410:
      return 'El enlace ya no está disponible. Solicitá uno nuevo para continuar.';
    case 429:
      return 'Recibimos demasiados intentos. Esperá unos minutos y probá nuevamente.';
    case 500:
    case 502:
    case 503:
    case 504:
      return 'No pudimos procesar la solicitud en este momento. Intentá nuevamente más tarde.';
    default:
      return 'No pudimos completar la operación. Intentá nuevamente.';
  }
}
