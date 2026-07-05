package com.fedebacelar.bank.document.application.port.out;

import com.fedebacelar.bank.document.domain.model.DocumentFile;

public interface ObjectStoragePort {

    StoredObject store(String objectKey, DocumentFile file);

    record StoredObject(
            String bucketName,
            String objectKey
    ) {
    }
}

