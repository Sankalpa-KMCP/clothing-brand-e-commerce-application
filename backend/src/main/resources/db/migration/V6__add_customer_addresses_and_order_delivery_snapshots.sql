CREATE TABLE customer_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(100),
    recipient_name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    city VARCHAR(120) NOT NULL,
    region VARCHAR(120),
    postal_code VARCHAR(32),
    country VARCHAR(120) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_addresses_user_id ON customer_addresses(user_id);
CREATE UNIQUE INDEX idx_customer_addresses_user_id_default ON customer_addresses(user_id) WHERE is_default = TRUE;

CREATE TABLE order_delivery_addresses (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    recipient_name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    city VARCHAR(120) NOT NULL,
    region VARCHAR(120),
    postal_code VARCHAR(32),
    country VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_delivery_addresses_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
);
