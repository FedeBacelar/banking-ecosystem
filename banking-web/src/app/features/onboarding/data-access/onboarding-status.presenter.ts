import { OnboardingState } from '../models/onboarding.models';

export const ONBOARDING_MILESTONES = [
  'Solicitud enviada',
  'Revisión de la solicitud',
  'Preparación de la cuenta',
  'Creación del acceso'
] as const;

export type OnboardingStatusTone =
  | 'waiting'
  | 'action'
  | 'success'
  | 'commercial'
  | 'technical'
  | 'unavailable';

export interface OnboardingStatusAction {
  kind: 'router' | 'full-page';
  label: string;
  href: string;
}

export interface OnboardingStatusView {
  title: string;
  description: string;
  supportingText?: string;
  tone: OnboardingStatusTone;
  completedMilestones: number;
  activeMilestone: number | null;
  failedMilestone: number | null;
  autoPoll: boolean;
  slowPoll?: boolean;
  canRefresh: boolean;
  canResendCredentials: boolean;
  action?: OnboardingStatusAction;
}

const STATUS_VIEWS = {
  EMAIL_VERIFICATION_PENDING: {
    title: 'Revisá tu correo',
    description: 'Usá el enlace que te enviamos para continuar con la solicitud.',
    tone: 'action',
    completedMilestones: 0,
    activeMilestone: null,
    failedMilestone: null,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'router',
      label: 'Pedir otro enlace',
      href: '/onboarding'
    }
  },
  IN_PROGRESS: {
    title: 'Falta completar tu solicitud',
    description: 'Todavía no recibimos todos los datos.',
    tone: 'action',
    completedMilestones: 0,
    activeMilestone: null,
    failedMilestone: null,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'router',
      label: 'Continuar solicitud',
      href: '/onboarding/solicitud'
    }
  },
  SUBMITTED: {
    title: 'Estamos revisando tu solicitud',
    description: 'Recibimos tus datos. Por ahora no necesitás hacer nada.',
    tone: 'waiting',
    completedMilestones: 1,
    activeMilestone: 1,
    failedMilestone: null,
    autoPoll: true,
    canRefresh: true,
    canResendCredentials: false
  },
  UNDER_AUTOMATED_REVIEW: {
    title: 'Estamos revisando tu solicitud',
    description: 'Recibimos tus datos. Por ahora no necesitás hacer nada.',
    tone: 'waiting',
    completedMilestones: 1,
    activeMilestone: 1,
    failedMilestone: null,
    autoPoll: true,
    canRefresh: true,
    canResendCredentials: false
  },
  REVIEW_FAILED: {
    title: 'No pudimos terminar la revisión',
    description: 'Tu solicitud quedó guardada. No hace falta que vuelvas a enviarla. Volvé a consultar más tarde.',
    tone: 'technical',
    completedMilestones: 1,
    activeMilestone: null,
    failedMilestone: 1,
    autoPoll: true,
    slowPoll: true,
    canRefresh: true,
    canResendCredentials: false
  },
  APPROVED: {
    title: 'Estamos preparando tu acceso',
    description: 'Aprobamos tu solicitud. Cuando el acceso esté listo, te vamos a enviar un correo para que elijas tu usuario y contraseña.',
    tone: 'waiting',
    completedMilestones: 2,
    activeMilestone: 2,
    failedMilestone: null,
    autoPoll: true,
    canRefresh: true,
    canResendCredentials: false
  },
  REJECTED: {
    title: 'No podemos avanzar con tu solicitud',
    description: 'En esta oportunidad no podemos abrir la cuenta que solicitaste.',
    tone: 'commercial',
    completedMilestones: 2,
    activeMilestone: null,
    failedMilestone: null,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'router',
      label: 'Volver al inicio',
      href: '/'
    }
  },
  PROVISIONING: {
    title: 'Estamos preparando tu acceso',
    description: 'Aprobamos tu solicitud. Cuando el acceso esté listo, te vamos a enviar un correo para que elijas tu usuario y contraseña.',
    tone: 'waiting',
    completedMilestones: 2,
    activeMilestone: 2,
    failedMilestone: null,
    autoPoll: true,
    canRefresh: true,
    canResendCredentials: false
  },
  PROVISIONING_FAILED: {
    title: 'No pudimos terminar de preparar tu acceso',
    description: 'Tu solicitud está aprobada y sigue guardada. Volvé a consultar más tarde.',
    tone: 'technical',
    completedMilestones: 2,
    activeMilestone: null,
    failedMilestone: 2,
    autoPoll: true,
    slowPoll: true,
    canRefresh: true,
    canResendCredentials: false
  },
  CREDENTIAL_SETUP_PENDING: {
    title: 'Creá tu acceso a Nerva Banking',
    description: 'Te enviamos un correo para que elijas tu usuario y contraseña.',
    supportingText: 'Usá el enlace del último correo que recibiste. Si no lo encontrás, revisá Spam.',
    tone: 'action',
    completedMilestones: 3,
    activeMilestone: 3,
    failedMilestone: null,
    autoPoll: true,
    canRefresh: true,
    canResendCredentials: true
  },
  CREDENTIAL_SETUP_EXPIRED: {
    title: 'El enlace para crear tu acceso venció',
    description: 'No pudimos completar el alta porque terminó el plazo para elegir tu usuario y contraseña.',
    tone: 'unavailable',
    completedMilestones: 3,
    activeMilestone: null,
    failedMilestone: 3,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'router',
      label: 'Volver al inicio',
      href: '/'
    }
  },
  CREDENTIAL_SETUP_FAILED: {
    title: 'No pudimos completar la creación de tu acceso',
    description: 'Tu solicitud quedó guardada, pero por ahora no podemos completar el alta.',
    tone: 'technical',
    completedMilestones: 3,
    activeMilestone: null,
    failedMilestone: 3,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'router',
      label: 'Volver al inicio',
      href: '/'
    }
  },
  COMPLETED: {
    title: 'Tu cuenta está lista',
    description: 'Ya podés ingresar a Nerva Banking.',
    tone: 'success',
    completedMilestones: 4,
    activeMilestone: null,
    failedMilestone: null,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'full-page',
      label: 'Ingresar a Nerva Banking',
      href: '/web/auth/login/home'
    }
  },
  EXPIRED: {
    title: 'Tu solicitud venció',
    description: 'El plazo para completarla terminó. Si querés, podés empezar una nueva.',
    tone: 'unavailable',
    completedMilestones: 0,
    activeMilestone: null,
    failedMilestone: 0,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'router',
      label: 'Empezar otra solicitud',
      href: '/onboarding'
    }
  },
  CANCELLED: {
    title: 'Solicitud cancelada',
    description: 'Esta solicitud ya no está activa. Si querés, podés empezar una nueva.',
    tone: 'unavailable',
    completedMilestones: 0,
    activeMilestone: null,
    failedMilestone: 0,
    autoPoll: false,
    canRefresh: false,
    canResendCredentials: false,
    action: {
      kind: 'router',
      label: 'Empezar otra solicitud',
      href: '/onboarding'
    }
  }
} as const satisfies Record<OnboardingState, OnboardingStatusView>;

export function presentOnboardingStatus(status: OnboardingState): OnboardingStatusView {
  return STATUS_VIEWS[status];
}
