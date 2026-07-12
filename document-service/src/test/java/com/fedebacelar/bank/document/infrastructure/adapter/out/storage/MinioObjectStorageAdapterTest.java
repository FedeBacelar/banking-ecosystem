package com.fedebacelar.bank.document.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.document.domain.exception.DocumentStorageException;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class MinioObjectStorageAdapterTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final MinioObjectStorageAdapter adapter = new MinioObjectStorageAdapter(s3Client, "banking-documents");

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

    private DocumentFile file() {
        return new DocumentFile(
                () -> new ByteArrayInputStream(new byte[]{1, 2, 3, 4}),
                4,
                "image/jpeg",
                "dni-front.jpg"
        );
    }
}
