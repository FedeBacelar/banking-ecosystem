CREATE TABLE account (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    account_number VARCHAR(24) NOT NULL,
    cbu VARCHAR(22) NOT NULL,
    alias VARCHAR(80) NULL,
    type VARCHAR(40) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    opened_at TIMESTAMP(6) NOT NULL,
    closed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_account_number UNIQUE (account_number),
    CONSTRAINT uk_account_cbu UNIQUE (cbu),
    CONSTRAINT uk_account_alias UNIQUE (alias)
);

CREATE TABLE account_balance (
    id CHAR(36) NOT NULL,
    account_id CHAR(36) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    current_balance DECIMAL(19, 2) NOT NULL,
    available_balance DECIMAL(19, 2) NOT NULL,
    hold_balance DECIMAL(19, 2) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_account_balance_account UNIQUE (account_id),
    CONSTRAINT fk_account_balance_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE account_status_history (
    id CHAR(36) NOT NULL,
    account_id CHAR(36) NOT NULL,
    previous_status VARCHAR(40) NULL,
    new_status VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    changed_by VARCHAR(120) NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_account_status_history_account FOREIGN KEY (account_id) REFERENCES account (id)
);
