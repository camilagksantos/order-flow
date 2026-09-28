ALTER TABLE shop_order
    ADD COLUMN delivery_street VARCHAR(200),
    ADD COLUMN delivery_number VARCHAR(20),
    ADD COLUMN delivery_complement VARCHAR(100),
    ADD COLUMN delivery_neighborhood VARCHAR(100),
    ADD COLUMN delivery_city VARCHAR(100),
    ADD COLUMN delivery_district VARCHAR(50),
    ADD COLUMN delivery_postal_code VARCHAR(8),
    ADD COLUMN delivery_country VARCHAR(2);