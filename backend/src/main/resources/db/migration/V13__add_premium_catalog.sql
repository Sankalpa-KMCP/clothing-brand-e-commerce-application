INSERT INTO categories (id, name, description, image_url) VALUES 
(1001, 'Premium Denim', 'Handcrafted denim for everyday wear.', '/assets/category_denim.jpg'),
(1002, 'Essential Knits', 'Soft, breathable knitwear.', '/assets/category_knits.jpg'),
(1003, 'Outerwear', 'Tailored coats and jackets.', '/assets/category_outerwear.jpg'),
(1004, 'Accessories', 'Premium leather goods and scarves.', '/assets/category_accessories.jpg')
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (id, category_id, name, description, image_url, active) VALUES 
(1001, 1001, 'Selvedge Slim Jean', 'Classic 5-pocket slim fit selvedge denim.', '/assets/product_jean.jpg', true),
(1002, 1002, 'Cashmere Crew', '100% cashmere crewneck sweater.', '/assets/product_crew.jpg', true),
(1003, 1002, 'Merino Cardigan', 'Lightweight merino wool cardigan.', '/assets/product_cardigan.jpg', true),
(1004, 1003, 'Wool Trench Coat', 'Tailored camel wool trench coat.', '/assets/product_trench.jpg', true),
(1005, 1004, 'Leather Chelsea Boots', 'Italian leather slip-on Chelsea boots.', '/assets/product_boots.jpg', true),
(1006, 1004, 'Silk Neck Scarf', 'Patterned silk neck scarf.', '/assets/product_scarf.jpg', true),
(1007, 1001, 'Pleated Linen Trousers', 'Relaxed fit pleated linen tailored trousers.', '/assets/product_trousers.jpg', true),
(1008, 1002, 'Oversized Cotton Shirt', 'Crisp white oversized cotton button-down shirt.', '/assets/product_shirt.jpg', true),
(1009, 1003, 'Quilted Puffer Jacket', 'Water-resistant, lightweight down puffer jacket.', '/assets/product_puffer.jpg', true),
(1010, 1004, 'Signature Leather Belt', 'Full-grain Italian leather belt with a brass buckle.', '/assets/product_belt.jpg', true),
(1011, 1002, 'Heavyweight Hoodie', 'Premium 500gsm heavyweight cotton hoodie.', '/assets/product_hoodie.jpg', true),
(1012, 1001, 'Relaxed Fit Chinos', 'Garment-dyed cotton chinos in a relaxed fit.', '/assets/product_chinos.jpg', true),
(1013, 1002, 'Cashmere Turtleneck', 'Ultra-soft cashmere turtleneck sweater.', '/assets/product_turtleneck.jpg', true),
(1014, 1004, 'Wool Fedora Hat', 'Classic olive green wool fedora.', '/assets/product_fedora.jpg', true),
(1015, 1004, 'Leather Briefcase', 'Full-grain leather professional briefcase.', '/assets/product_briefcase.jpg', true),
(1016, 1004, 'Suede Loafers', 'Premium tan suede penny loafers.', '/assets/product_boots.jpg', true),
(1017, 1004, 'Aviator Sunglasses', 'Gold-framed aviator sunglasses.', '/assets/product_sunglasses.jpg', true),
(1018, 1004, 'Silk Pocket Square', 'Patterned silk pocket square.', '/assets/product_scarf.jpg', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, size, color, price, stock_quantity) VALUES 
(1001, 1001, 'DEN-SLIM-32', '32', 'Indigo', 4000.00, 50),
(1002, 1001, 'DEN-SLIM-34', '34', 'Indigo', 4000.00, 30),
(1003, 1002, 'CSH-CRW-M', 'M', 'Charcoal', 6000.00, 15),
(1004, 1003, 'MER-CRD-L', 'L', 'Navy', 5000.00, 20),
(1005, 1004, 'TRN-CML-M', 'M', 'Camel', 5999.00, 10),
(1006, 1005, 'BT-BLK-10', '10', 'Black', 4999.00, 12),
(1007, 1006, 'SCF-PRT-OS', 'OS', 'Multi', 2999.00, 40),
(1008, 1007, 'TRS-LIN-32', '32', 'Sand', 3999.00, 25),
(1009, 1008, 'SHT-WHT-L', 'L', 'White', 3000.00, 35),
(1010, 1009, 'PUF-BLK-L', 'L', 'Black', 6000.00, 10),
(1011, 1010, 'BLT-BRN-34', '34', 'Brown', 2999.00, 50),
(1012, 1011, 'HOD-GRY-XL', 'XL', 'Grey', 3999.00, 20),
(1013, 1012, 'CHN-NAV-32', '32', 'Navy', 4000.00, 40),
(1014, 1013, 'TRT-CRM-L', 'L', 'Cream', 5999.00, 25),
(1015, 1014, 'HAT-OLV-OS', 'OS', 'Olive', 3999.00, 30),
(1016, 1015, 'BRF-BRN-OS', 'OS', 'Brown', 6000.00, 15),
(1017, 1016, 'LOF-TAN-10', '10', 'Tan', 5000.00, 20),
(1018, 1017, 'SUN-GLD-OS', 'OS', 'Gold', 4000.00, 35),
(1019, 1018, 'PKT-MLT-OS', 'OS', 'Multi', 2999.00, 50)
ON CONFLICT (id) DO UPDATE SET price = EXCLUDED.price;
