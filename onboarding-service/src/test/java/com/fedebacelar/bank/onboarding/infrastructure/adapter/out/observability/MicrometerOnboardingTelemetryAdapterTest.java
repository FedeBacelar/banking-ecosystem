package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MicrometerOnboardingTelemetryAdapterTest {

    private SdkTracerProvider tracerProvider;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
        if (tracerProvider != null) {
            tracerProvider.close();
        }
    }

    @Test
    void recordsBoundedApplicationAndWorkMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var telemetry = new MicrometerOnboardingTelemetryAdapter(registry, OpenTelemetry.noop());

        telemetry.recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.CREATED);
        telemetry.recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.AUTO_REVIEW,
                OnboardingTelemetryPort.WorkOutcome.SUCCEEDED
        );

        assertThat(registry.get("nerva.onboarding.applications")
                .tag("event", "created").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("nerva.onboarding.work.executions")
                .tag("job_type", "auto_review")
                .tag("outcome", "succeeded")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void publishesTransactionalMetricsOnlyAfterCommit() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var telemetry = new MicrometerOnboardingTelemetryAdapter(registry, OpenTelemetry.noop());
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        telemetry.recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.SUBMITTED);

        assertThat(registry.get("nerva.onboarding.applications")
                .tag("event", "submitted").counter().count()).isZero();
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        assertThat(registry.get("nerva.onboarding.applications")
                .tag("event", "submitted").counter().count()).isEqualTo(1.0);
    }

    @Test
    void startsAnIndependentWorkerSpanAndMarksHandledRetriesAsErrors() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var telemetry = new MicrometerOnboardingTelemetryAdapter(registry, openTelemetry);
        Span ambient = openTelemetry.getTracer("test").spanBuilder("ambient").startSpan();

        try (Scope ignored = ambient.makeCurrent()) {
            telemetry.observeWorkerExecution(OnboardingTelemetryPort.WorkType.MAGIC_LINK_DELIVERY, () ->
                    telemetry.recordWorkOutcome(
                            OnboardingTelemetryPort.WorkType.MAGIC_LINK_DELIVERY,
                            OnboardingTelemetryPort.WorkOutcome.RETRY
                    ));
        } finally {
            ambient.end();
        }

        SpanData worker = exporter.getFinishedSpanItems().stream()
                .filter(span -> span.getName().equals("onboarding.worker.magic_link_delivery"))
                .findFirst()
                .orElseThrow();
        assertThat(worker.getParentSpanId()).isEqualTo("0000000000000000");
        assertThat(worker.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(worker.getAttributes().asMap()).containsEntry(
                io.opentelemetry.api.common.AttributeKey.stringKey("nerva.work.outcome"),
                "retry"
        );
        assertThat(worker.getEvents()).isEmpty();
    }

    @Test
    void createsAChildSpanForEachProvisioningBoundaryWithoutIdentifiers() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        var telemetry = new MicrometerOnboardingTelemetryAdapter(new SimpleMeterRegistry(), openTelemetry);

        telemetry.observeWorkerExecution(OnboardingTelemetryPort.WorkType.PROVISIONING, () -> {
            String result = telemetry.observeProvisioningStep(
                    ProvisioningStepType.OPEN_ACCOUNT,
                    () -> "internal-reference-must-not-be-recorded"
            );
            assertThat(result).isEqualTo("internal-reference-must-not-be-recorded");
        });

        SpanData step = exporter.getFinishedSpanItems().stream()
                .filter(span -> span.getName().equals("onboarding.provisioning.open_account"))
                .findFirst()
                .orElseThrow();
        SpanData worker = exporter.getFinishedSpanItems().stream()
                .filter(span -> span.getName().equals("onboarding.worker.provisioning"))
                .findFirst()
                .orElseThrow();

        assertThat(step.getTraceId()).isEqualTo(worker.getTraceId());
        assertThat(step.getParentSpanId()).isEqualTo(worker.getSpanId());
        assertThat(step.getAttributes().asMap()).containsOnly(
                java.util.Map.entry(
                        io.opentelemetry.api.common.AttributeKey.stringKey("nerva.provisioning.step"),
                        "open_account"
                )
        );
        assertThat(step.toString()).doesNotContain("internal-reference-must-not-be-recorded");
    }

    @Test
    void marksProvisioningFailuresWithoutRecordingTheirMessageAndRethrowsTheSameFailure() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        var telemetry = new MicrometerOnboardingTelemetryAdapter(new SimpleMeterRegistry(), openTelemetry);
        IllegalStateException failure = new IllegalStateException("sensitive persistence detail");

        assertThatThrownBy(() -> telemetry.observeProvisioningStep(
                ProvisioningStepType.CREATE_IDENTITY_LINK,
                () -> {
                    throw failure;
                }
        )).isSameAs(failure);

        SpanData step = exporter.getFinishedSpanItems().getFirst();
        assertThat(step.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(step.getAttributes().asMap()).containsEntry(
                io.opentelemetry.api.common.AttributeKey.stringKey("error.type"),
                "IllegalStateException"
        );
        assertThat(step.toString()).doesNotContain("sensitive persistence detail");
    }

    @Test
    void provisioningExecutionRemainsSingleAndSuccessfulWhenScopeCreationFails() {
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        Tracer tracer = mock(Tracer.class);
        SpanBuilder builder = mock(SpanBuilder.class);
        Span span = mock(Span.class);
        when(openTelemetry.getTracer(anyString())).thenReturn(tracer);
        when(tracer.spanBuilder(anyString())).thenReturn(builder);
        when(builder.setSpanKind(io.opentelemetry.api.trace.SpanKind.INTERNAL)).thenReturn(builder);
        when(builder.setAttribute(anyString(), anyString())).thenReturn(builder);
        when(builder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenThrow(new IllegalStateException("telemetry scope unavailable"));
        AtomicInteger executions = new AtomicInteger();
        var telemetry = new MicrometerOnboardingTelemetryAdapter(new SimpleMeterRegistry(), openTelemetry);

        String result = telemetry.observeProvisioningStep(
                ProvisioningStepType.OPEN_ACCOUNT,
                () -> {
                    executions.incrementAndGet();
                    return "reference";
                }
        );

        assertThat(result).isEqualTo("reference");
        assertThat(executions).hasValue(1);
        verify(span).end();
    }
}
