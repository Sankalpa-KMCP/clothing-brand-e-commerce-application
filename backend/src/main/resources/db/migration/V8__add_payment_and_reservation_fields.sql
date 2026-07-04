ALTER TABLE orders ADD COLUMN payment_status VARCHAR(50);
UPDATE orders SET payment_status = 'NOT_APPLICABLE';
ALTER TABLE orders ALTER COLUMN payment_status SET NOT NULL;

ALTER TABLE orders ADD COLUMN stripe_session_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN stripe_payment_intent_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN reservation_expires_at TIMESTAMP WITH TIME ZONE;

-- Unique constraints
ALTER TABLE orders ADD CONSTRAINT uk_orders_stripe_session_id UNIQUE (stripe_session_id);
ALTER TABLE orders ADD CONSTRAINT uk_orders_stripe_payment_intent_id UNIQUE (stripe_payment_intent_id);

-- Index for reservation expiry
CREATE INDEX idx_orders_reservation_expires_at ON orders(reservation_expires_at);

-- Webhook event idempotency table
CREATE TABLE webhook_events (
    event_id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
