package com.fedebacelar.bank.document.infrastructure.adapter.out.storage;

import com.fedebacelar.bank.document.application.port.out.ObjectStoragePort;
import com.fedebacelar.bank.document.domain.exception.DocumentStorageException;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class MinioObjectStorageAdapter implements ObjectStoragePort {

    private static final String INSTRUMENTATION_SCOPE = "com.fedebacelar.bank.document.storage";

    private final S3Client s3Client;
    private final String bucketName;
    private final Tracer tracer;

    public MinioObjectStorageAdapter(
            S3Client s3Client,
            @Value("${document.storage.bucket}") String bucketName,
            OpenTelemetry openTelemetry
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE);
    }

    @Override
    public StoredObject store(String objectKey, DocumentFile file) {
        return traceStorageOperation("put", () -> storeObject(objectKey, file));
    }

    private StoredObject storeObject(String objectKey, DocumentFile file) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.contentType())
                .contentLength(file.size())
                .build();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream content = new DigestInputStream(file.openStream(), digest)) {
                s3Client.putObject(request, RequestBody.fromInputStream(content, file.size()));
            }
            return new StoredObject(bucketName, objectKey, HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException exception) {
            throw new DocumentStorageException("SHA-256 digest is unavailable", exception);
        } catch (java.io.IOException exception) {
            throw new DocumentStorageException("Could not read document content", exception);
        } catch (S3Exception exception) {
            throw new DocumentStorageException("Document storage failed: " + exception.awsErrorDetails().errorMessage(), exception);
        } catch (RuntimeException exception) {
            throw new DocumentStorageException("Document storage failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        traceStorageOperation("delete", () -> {
            deleteObject(objectKey);
            return null;
        });
    }

    private void deleteObject(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (RuntimeException exception) {
            throw new DocumentStorageException("Document cleanup failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public String bucketName() {
        return bucketName;
    }

    private <T> T traceStorageOperation(String operation, Supplier<T> execution) {
        Span span;
        try {
            span = tracer.spanBuilder("document.storage." + operation)
                    .setSpanKind(SpanKind.CLIENT)
                    .setAttribute("peer.service", "minio")
                    .setAttribute("nerva.storage.system", "minio")
                    .setAttribute("nerva.storage.operation", operation)
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
            return execution.get();
        } catch (RuntimeException | Error executionFailure) {
            safeFailure(span, executionFailure);
            throw executionFailure;
        } finally {
            safeClose(scope);
            safeEnd(span);
        }
    }

    private void safeFailure(Span span, Throwable failure) {
        try {
            span.setStatus(StatusCode.ERROR);
            span.setAttribute("error.type", failure.getClass().getSimpleName());
        } catch (RuntimeException ignored) {
            // Storage behavior must never depend on telemetry state.
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

