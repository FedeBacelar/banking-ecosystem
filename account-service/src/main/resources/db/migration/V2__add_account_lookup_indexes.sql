CREATE INDEX idx_account_customer_id ON account (customer_id);
CREATE INDEX idx_account_status ON account (status);
CREATE INDEX idx_account_type ON account (type);
CREATE INDEX idx_account_currency ON account (currency);
CREATE INDEX idx_account_status_history_account_id ON account_status_history (account_id);
