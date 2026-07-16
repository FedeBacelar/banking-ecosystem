package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("observability")
public class MicrometerOnboardingTelemetryAdapter implements OnboardingTelemetryPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerOnboardingTelemetryAdapter.class);
    private static final String INSTRUMENTATION_SCOPE = "com.fedebacelar.bank.onboarding.workers";

    private final Map<ApplicationEvent, Counter> applicationCounters;
    private final Map<WorkType, Map<WorkOutcome, Counter>> workCounters;
    private final Tracer tracer;

    @Autowired
    public MicrometerOnboardingTelemetryAdapter(
            MeterRegistry meterRegistry,
            ObjectProvider<OpenTelemetry> openTelemetryProvider
    ) {
        this(meterRegistry, openTelemetryProvider.getIfAvailable(OpenTelemetry::noop));
    }

    MicrometerOnboardingTelemetryAdapter(MeterRegistry meterRegistry, OpenTelemetry openTelemetry) {
        this.applicationCounters = registerApplicationCounters(meterRegistry);
        this.workCounters = registerWorkCounters(meterRegistry);
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE);
    }

    @Override
    public void recordApplicationEvent(ApplicationEvent event) {
        recordAfterCommit(() -> {
            applicationCounters.get(event).increment();
            LOGGER.atInfo()
                    .addKeyValue("event.name", "onboarding.application." + event.metricValue())
                    .addKeyValue("application.event", event.metricValue())
                    .log("Onboarding application lifecycle event");
        });
    }

    @Override
    public void recordWorkClaimed(WorkType workType) {
        recordSafely(() -> LOGGER.atInfo()
                .addKeyValue("event.name", "onboarding.work.claimed")
                .addKeyValue("job.type", workType.metricValue())
                .log("Onboarding work claimed"));
    }

    @Override
    public void recordWorkOutcome(WorkType workType, WorkOutcome outcome) {
        annotateCurrentSpan(outcome);
        recordAfterCommit(() -> {
            workCounters.get(workType).get(outcome).increment();
            LOGGER.atInfo()
                    .addKeyValue("event.name", "onboarding.work." + outcome.metricValue())
                    .addKeyValue("job.type", workType.metricValue())
                    .addKeyValue("work.outcome", outcome.metricValue())
                    .log("Onboarding work execution completed");
        });
    }

    @Override
    public void observeWorkerExecution(WorkType workType, Runnable execution) {
        observeIndependentWorker(workType.metricValue(), execution);
    }

    @Override
    public void observeExpirationExecution(Runnable execution) {
        observeIndependentWorker("expiration", execution);
    }

    private void observeIndependentWorker(String jobType, Runnable execution) {
        Span span;
        try {
            span = tracer.spanBuilder("onboarding.worker." + jobType)
                    .setSpanKind(SpanKind.INTERNAL)
                    .setNoParent()
                    .setAttribute("nerva.job.type", jobType)
                    .startSpan();
        } catch (RuntimeException telemetryFailure) {
            execution.run();
            return;
        }

        Scope scope;
        try {
            scope = span.makeCurrent();
        } catch (RuntimeException telemetryFailure) {
            safeEnd(span);
            execution.run();
            return;
        }

        try {
            execution.run();
        } catch (RuntimeException | Error executionFailure) {
            safeSpanFailure(span, executionFailure);
            throw executionFailure;
        } finally {
            safeClose(scope);
            safeEnd(span);
        }
    }

    @Override
    public <T> T observeProvisioningStep(ProvisioningStepType stepType, Supplier<T> execution) {
        String step = stepType.name().toLowerCase(Locale.ROOT);
        Span span;
        try {
            span = tracer.spanBuilder("onboarding.provisioning." + step)
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("nerva.provisioning.step", step)
                    .startSpan();
        } catch (RuntimeException telemetryFailure) {
            return execution.get();
        }

        Scope scope;
        try {
            scope = span.makeCurrent();
        } catch (RuntimeException telemetryFailure) {
            safeEnd(span);
            return execution.get();
        }

        try {
            T result = execution.get();
            recordSafely(() -> LOGGER.atInfo()
                    .addKeyValue("event.name", "onboarding.provisioning.step.succeeded")
                    .addKeyValue("provisioning.step", step)
                    .log("Onboarding provisioning step completed"));
            return result;
        } catch (RuntimeException | Error executionFailure) {
            safeSpanFailure(span, executionFailure);
            recordSafely(() -> LOGGER.atWarn()
                    .addKeyValue("event.name", "onboarding.provisioning.step.failed")
                    .addKeyValue("provisioning.step", step)
                    .addKeyValue("error.type", executionFailure.getClass().getSimpleName())
                    .log("Onboarding provisioning step failed"));
            throw executionFailure;
        } finally {
            safeClose(scope);
            safeEnd(span);
        }
    }

    private Map<ApplicationEvent, Counter> registerApplicationCounters(MeterRegistry meterRegistry) {
        Map<ApplicationEvent, Counter> counters = new EnumMap<>(ApplicationEvent.class);
        for (ApplicationEvent event : ApplicationEvent.values()) {
            counters.put(event, Counter.builder("nerva.onboarding.applications")
                    .description("Onboarding application lifecycle transitions")
                    .tag("event", event.metricValue())
                    .register(meterRegistry));
        }
        return Map.copyOf(counters);
    }

    private Map<WorkType, Map<WorkOutcome, Counter>> registerWorkCounters(MeterRegistry meterRegistry) {
        Map<WorkType, Map<WorkOutcome, Counter>> counters = new EnumMap<>(WorkType.class);
        for (WorkType workType : WorkType.values()) {
            Map<WorkOutcome, Counter> outcomeCounters = new EnumMap<>(WorkOutcome.class);
            for (WorkOutcome outcome : WorkOutcome.values()) {
                outcomeCounters.put(outcome, Counter.builder("nerva.onboarding.work.executions")
                        .description("Durable onboarding work execution outcomes")
                        .tag("job_type", workType.metricValue())
                        .tag("outcome", outcome.metricValue())
                        .register(meterRegistry));
            }
            counters.put(workType, Map.copyOf(outcomeCounters));
        }
        return Map.copyOf(counters);
    }

    private void recordAfterCommit(Runnable telemetry) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        recordSafely(telemetry);
                    }
                });
                return;
            } catch (RuntimeException ignored) {
                // Telemetry must never affect the business transaction.
            }
        }
        recordSafely(telemetry);
    }

    private void recordSafely(Runnable telemetry) {
        try {
            telemetry.run();
        } catch (RuntimeException ignored) {
            // Export and registry failures are deliberately fail-open.
        }
    }

    private void annotateCurrentSpan(WorkOutcome outcome) {
        try {
            Span current = Span.current();
            current.setAttribute("nerva.work.outcome", outcome.metricValue());
            if (outcome != WorkOutcome.SUCCEEDED) {
                current.setStatus(StatusCode.ERROR);
            }
        } catch (RuntimeException ignored) {
            // Outcome metrics and persistence must not depend on trace state.
        }
    }

    private void safeSpanFailure(Span span, Throwable failure) {
        try {
            span.setStatus(StatusCode.ERROR);
            span.setAttribute("error.type", failure.getClass().getSimpleName());
        } catch (RuntimeException ignored) {
            // Never record exception messages because they may contain sensitive data.
        }
    }

    private void safeClose(Scope scope) {
        try {
            scope.close();
        } catch (RuntimeException ignored) {
            // Telemetry cleanup is fail-open.
        }
    }

    private void safeEnd(Span span) {
        try {
            span.end();
        } catch (RuntimeException ignored) {
            // Telemetry cleanup is fail-open.
        }
    }
}
