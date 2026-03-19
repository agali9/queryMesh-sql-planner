-- Shard C: inventory, warehouses, shipments
CREATE TABLE warehouses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(200) NOT NULL,
    capacity INT NOT NULL
);

CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    product_id INT NOT NULL,
    warehouse_id INT REFERENCES warehouses(id),
    quantity INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT NOW()
);

CREATE TABLE shipments (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    warehouse_id INT REFERENCES warehouses(id),
    status VARCHAR(30) DEFAULT 'pending',
    shipped_at TIMESTAMP
);

INSERT INTO warehouses (name, location, capacity) VALUES
    ('West Coast DC', 'Los Angeles, CA', 50000),
    ('East Coast DC', 'Newark, NJ', 45000),
    ('Central Hub', 'Dallas, TX', 60000);

INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES
    (1, 1, 500),
    (1, 2, 300),
    (2, 1, 200),
    (2, 3, 150),
    (3, 2, 400),
    (3, 3, 250),
    (4, 1, 1000),
    (5, 2, 180);

INSERT INTO shipments (order_id, warehouse_id, status, shipped_at) VALUES
    (1, 1, 'delivered', NOW() - INTERVAL '3 days'),
    (2, 1, 'delivered', NOW() - INTERVAL '2 days'),
    (3, 2, 'in_transit', NOW() - INTERVAL '1 day'),
    (4, 1, 'delivered', NOW() - INTERVAL '5 days'),
    (5, 1, 'pending', NULL),
    (6, 2, 'in_transit', NOW() - INTERVAL '12 hours');

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);
CREATE INDEX idx_shipments_order ON shipments(order_id);
