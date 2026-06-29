CREATE TABLE customer_number_sequence (
    sequence_year INT NOT NULL,
    next_value BIGINT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sequence_year)
);
