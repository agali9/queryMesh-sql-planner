-- Shard B: orders, products, categories, payments
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category_id INT REFERENCES categories(id),
    price DECIMAL(10,2) NOT NULL,
    sku VARCHAR(50) UNIQUE
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT REFERENCES products(id),
    quantity INT NOT NULL DEFAULT 1,
    total DECIMAL(10,2) NOT NULL,
    order_date TIMESTAMP DEFAULT NOW()
);

CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id),
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'completed',
    paid_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices and accessories'),
    ('Books', 'Physical and digital books'),
    ('Clothing', 'Apparel and accessories');

INSERT INTO products (name, category_id, price, sku) VALUES
    ('Wireless Headphones', 1, 79.99, 'ELEC-001'),
    ('Java Programming Guide', 2, 49.99, 'BOOK-001'),
    ('Running Shoes', 3, 129.99, 'CLTH-001'),
    ('USB-C Hub', 1, 34.99, 'ELEC-002'),
    ('Design Patterns Book', 2, 59.99, 'BOOK-002');

INSERT INTO orders (user_id, product_id, quantity, total) VALUES
    (1, 1, 1, 79.99),
    (1, 2, 2, 99.98),
    (2, 3, 1, 129.99),
    (3, 1, 1, 79.99),
    (4, 4, 3, 104.97),
    (5, 5, 1, 59.99);

INSERT INTO payments (order_id, amount, method) VALUES
    (1, 79.99, 'credit_card'),
    (2, 99.98, 'paypal'),
    (3, 129.99, 'credit_card'),
    (4, 79.99, 'debit_card'),
    (5, 104.97, 'credit_card'),
    (6, 59.99, 'paypal');

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_product ON orders(product_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_payments_order ON payments(order_id);
