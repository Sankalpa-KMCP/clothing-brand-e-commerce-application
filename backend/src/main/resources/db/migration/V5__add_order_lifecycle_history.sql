CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    actor_type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_status_history_order_id_created_at ON order_status_history(order_id, created_at, id);

INSERT INTO order_status_history (order_id, previous_status, new_status, actor_type, actor_user_id, created_at)
SELECT id, NULL, status, 'SYSTEM', NULL, created_at
FROM orders;
