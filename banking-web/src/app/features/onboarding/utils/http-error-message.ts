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
        return 'El enlace no es valido. Solicita uno nuevo para continuar.';
      case 'ONBOARDING_MAGIC_LINK_EXPIRED':
      case 'ONBOARDING_CONTINUATION_EXPIRED':
        return 'El enlace vencio. Solicita uno nuevo para continuar.';
      case 'ONBOARDING_MAGIC_LINK_ALREADY_CONSUMED':
        return 'Este enlace ya fue utilizado.';
      case 'VALIDATION_ERROR':
        return 'Revisa los datos ingresados e intenta nuevamente.';
      case 'TERMS_ACCEPTANCE_REQUIRED':
        return 'Necesitamos tu aceptacion para continuar.';
      case 'INVALID_ONBOARDING_DOCUMENT':
        return 'No pudimos aceptar el archivo. Carga una imagen JPG, PNG o un PDF de hasta 10 MB.';
      case 'ONBOARDING_SESSION_REQUIRED':
        return 'Necesitas abrir el enlace recibido por correo para continuar.';
      default:
        return statusMessage(error.status);
    }
  }

  return statusMessage();
}

function statusMessage(status?: number): string {
  switch (status) {
    case 0:
      return 'No pudimos conectarnos. Intenta nuevamente en unos minutos.';
    case 400:
      return 'No pudimos validar la solicitud. Revisa los datos e intenta nuevamente.';
    case 409:
      return 'No pudimos continuar con esta solicitud. Revisa el estado del tramite o solicita un nuevo enlace.';
    case 410:
      return 'El enlace ya no esta disponible. Solicita uno nuevo para continuar.';
    case 429:
      return 'Recibimos demasiados intentos. Espera unos minutos y proba nuevamente.';
    case 500:
    case 502:
    case 503:
    case 504:
      return 'No pudimos procesar la solicitud en este momento. Intenta nuevamente mas tarde.';
    default:
      return 'No pudimos completar la operacion. Intenta nuevamente.';
  }
}
