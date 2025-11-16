ALTER TABLE payments
    ADD COLUMN wallet_account_index INTEGER;

ALTER TABLE payments
    ADD COLUMN wallet_subaddress_index INTEGER;

CREATE INDEX idx_payments_wallet_subaddress
    ON payments(wallet_account_index, wallet_subaddress_index);
