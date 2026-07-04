CREATE UNIQUE INDEX uk_orders_active_pending_payment 
ON orders (user_id) 
WHERE status = 'PENDING_PAYMENT';
