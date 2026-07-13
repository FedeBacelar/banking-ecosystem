import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'nb-legal-document-page',
  template: `
    <article class="mx-auto w-full max-w-3xl px-5 py-10 sm:px-6 sm:py-16">
      <div class="rounded-xl border border-caution/30 bg-caution-subtle px-4 py-3 text-sm text-ink">
        <strong>Borrador de desarrollo.</strong> Este documento forma parte de un proyecto
        académico. No abre una cuenta real ni constituye un servicio financiero.
      </div>

      @if (isPrivacy) {
        <h1 class="mt-8 text-3xl font-semibold tracking-[-0.03em] text-ink">Política de privacidad</h1>
        <p class="mt-2 text-sm text-ink-muted">Actualizada el 12 de julio de 2026</p>
        <div class="mt-8 space-y-8 text-base leading-7 text-ink-muted">
          <section>
            <h2 class="text-xl font-semibold text-ink">Datos para esta demostración</h2>
            <p class="mt-2">La aplicación solicita información personal y archivos para evaluar el funcionamiento técnico y la experiencia del proyecto.</p>
          </section>
          <section>
            <h2 class="text-xl font-semibold text-ink">Usá solamente información ficticia</h2>
            <p class="mt-2">No ingreses datos personales reales, imágenes de documentos reales, contraseñas bancarias ni información financiera.</p>
          </section>
          <section>
            <h2 class="text-xl font-semibold text-ink">Uso de la información</h2>
            <p class="mt-2">Los datos de prueba se utilizan únicamente para recorrer y evaluar esta implementación académica. No se abre una cuenta ni se realizan operaciones con dinero.</p>
          </section>
        </div>
      } @else {
        <h1 class="mt-8 text-3xl font-semibold tracking-[-0.03em] text-ink">Términos y condiciones</h1>
        <p class="mt-2 text-sm text-ink-muted">Actualizados el 12 de julio de 2026</p>
        <div class="mt-8 space-y-8 text-base leading-7 text-ink-muted">
          <section>
            <h2 class="text-xl font-semibold text-ink">Alcance</h2>
            <p class="mt-2">Nerva Banking es una aplicación desarrollada con fines académicos. El recorrido representa una solicitud, pero no ofrece productos ni servicios financieros reales.</p>
          </section>
          <section>
            <h2 class="text-xl font-semibold text-ink">Información de prueba</h2>
            <p class="mt-2">Al continuar, aceptás usar datos ficticios y documentos creados para pruebas. No debés utilizar información de una persona real.</p>
          </section>
          <section>
            <h2 class="text-xl font-semibold text-ink">Sin relación bancaria</h2>
            <p class="mt-2">Completar este recorrido no genera una cuenta, un contrato bancario, un saldo ni derechos sobre un producto financiero.</p>
          </section>
        </div>
      }
    </article>
  `
})
export class LegalDocumentPage {
  private readonly route = inject(ActivatedRoute);
  protected readonly isPrivacy = this.route.snapshot.data['document'] === 'privacy';
}
