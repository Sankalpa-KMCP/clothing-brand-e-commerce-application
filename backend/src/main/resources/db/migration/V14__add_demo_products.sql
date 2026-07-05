INSERT INTO products (id, category_id, name, description, image_url, base_price, active, created_at, updated_at) VALUES 
(1019, 1004, 'Signature Gold Minimalist Watch', 'A masterfully crafted minimalist watch with a pure silver face, precision quartz movement, and genuine black leather strap.', '/assets/product_watch.jpg', 15000.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1020, 1004, 'VÉLURE Classic Handbag', 'An elegant designer handbag meticulously handcrafted from premium black full-grain leather, finished with exquisite gold hardware.', '/assets/product_handbag.jpg', 45000.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, base_price = EXCLUDED.base_price;

INSERT INTO product_variants (id, product_id, sku, size, color, price, stock_quantity, created_at, updated_at) VALUES 
(1020, 1019, 'WTCH-MIN-SLV', 'OS', 'Black/Silver', 15000.00, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1021, 1020, 'BAG-CLSC-BLK', 'OS', 'Black/Gold', 45000.00, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET stock_quantity = EXCLUDED.stock_quantity;
