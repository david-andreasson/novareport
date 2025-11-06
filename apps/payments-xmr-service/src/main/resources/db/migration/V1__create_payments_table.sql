CREATE TABLE payments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    payment_address VARCHAR(255) NOT NULL,
    amount_xmr DECIMAL(19, 8) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    duration_days INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);
