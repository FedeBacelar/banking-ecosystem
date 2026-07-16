package com.fedebacelar.bank.document.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.document.domain.exception.DocumentStorageException;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class MinioObjectStorageAdapterTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final MinioObjectStorageAdapter adapter =
            new MinioObjectStorageAdapter(s3Client, "banking-documents", OpenTelemetry.noop());
    private SdkTracerProvider tracerProvider;

    @AfterEach
    void tearDown() {
        if (tracerProvider != null) {
            tracerProvider.close();
        }
    }

    @Test
    void storesFileInConfiguredBucket() throws Exception {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            RequestBody body = invocation.getArgument(1);
            try (var content = body.contentStreamProvider().newStream()) {
                content.readAllBytes();
            }
            return PutObjectResponse.builder().build();
        });

        var storedObject = adapter.store("onboarding/application-1/DNI_FRONT/document-id", file());

        assertThat(storedObject.bucketName()).isEqualTo("banking-documents");
        assertThat(storedObject.objectKey()).isEqualTo("onboarding/application-1/DNI_FRONT/document-id");
        assertThat(storedObject.contentSha256())
                .isEqualTo("9f64a747e1b97f131fabb6b447296c9b6f0201e79fb3c5356e6c77e89b6a806a");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void wrapsStorageFailures() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("storage unavailable"));

        assertThatThrownBy(() -> adapter.store("onboarding/application-1/DNI_FRONT/document-id", file()))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessageContaining("storage unavailable");
    }

    @Test
    void tracesMinioWithoutRecordingDocumentIdentifiers() throws Exception {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            RequestBody body = invocation.getArgument(1);
            try (var content = body.contentStreamProvider().newStream()) {
                content.readAllBytes();
            }
            return PutObjectResponse.builder().build();
        });
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        var tracedAdapter = new MinioObjectStorageAdapter(
                s3Client,
                "banking-documents",
                openTelemetry(exporter)
        );

        tracedAdapter.store("onboarding/sensitive-reference/DNI_FRONT/document-id", file());

        var span = exporter.getFinishedSpanItems().getFirst();
        assertThat(span.getName()).isEqualTo("document.storage.put");
        assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);
        assertThat(span.getAttributes().asMap()).containsOnly(
                java.util.Map.entry(
                        io.opentelemetry.api.common.AttributeKey.stringKey("peer.service"),
                        "minio"
                ),
                java.util.Map.entry(
                        io.opentelemetry.api.common.AttributeKey.stringKey("nerva.storage.system"),
                        "minio"
                ),
                java.util.Map.entry(
                        io.opentelemetry.api.common.AttributeKey.stringKey("nerva.storage.operation"),
                        "put"
                )
        );
        assertThat(span.toString())
                .doesNotContain("sensitive-reference")
                .doesNotContain("document-id")
                .doesNotContain("banking-documents")
                .doesNotContain("dni-front.jpg");
    }

    @Test
    void marksStorageFailureWithoutRecordingItsMessage() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("sensitive-storage-detail"));
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        var tracedAdapter = new MinioObjectStorageAdapter(
                s3Client,
                "banking-documents",
                openTelemetry(exporter)
        );

        assertThatThrownBy(() -> tracedAdapter.store("sensitive-object-key", file()))
                .isInstanceOf(DocumentStorageException.class);

        var span = exporter.getFinishedSpanItems().getFirst();
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getAttributes().asMap()).containsEntry(
                io.opentelemetry.api.common.AttributeKey.stringKey("error.type"),
                "DocumentStorageException"
        );
        assertThat(span.toString())
                .doesNotContain("sensitive-storage-detail")
                .doesNotContain("sensitive-object-key");
    }

    @Test
    void telemetryFailureDoesNotRepeatOrPreventStorage() throws Exception {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            RequestBody body = invocation.getArgument(1);
            try (var content = body.contentStreamProvider().newStream()) {
                content.readAllBytes();
            }
            return PutObjectResponse.builder().build();
        });
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        Tracer tracer = mock(Tracer.class);
        when(openTelemetry.getTracer(any())).thenReturn(tracer);
        when(tracer.spanBuilder(any())).thenThrow(new IllegalStateException("collector unavailable"));
        var failOpenAdapter = new MinioObjectStorageAdapter(s3Client, "banking-documents", openTelemetry);

        assertThat(failOpenAdapter.store("object-key", file())).isNotNull();

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    private OpenTelemetry openTelemetry(InMemorySpanExporter exporter) {
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
    }

    private DocumentFile file() {
        return new DocumentFile(
                () -> new ByteArrayInputStream(new byte[]{1, 2, 3, 4}),
                4,
                "image/jpeg",
                "dni-front.jpg"
        );
    }
}
