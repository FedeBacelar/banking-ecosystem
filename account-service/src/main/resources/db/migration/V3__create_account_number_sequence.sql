CREATE TABLE account_number_sequence (
    year INT NOT NULL,
    next_value BIGINT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (year)
);
