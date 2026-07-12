ALTER TABLE identification_document
    ADD COLUMN document_number_canonical VARCHAR(80) NULL AFTER document_number;

UPDATE identification_document
SET document_number_canonical = UPPER(REGEXP_REPLACE(document_number, '[^A-Za-z0-9]', ''));

ALTER TABLE identification_document
    MODIFY document_number_canonical VARCHAR(80) NOT NULL,
    DROP INDEX uk_identification_document,
    ADD CONSTRAINT uk_identification_document_canonical
        UNIQUE (document_type, document_number_canonical, issuing_country);
