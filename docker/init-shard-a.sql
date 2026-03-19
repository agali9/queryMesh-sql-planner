-- Shard A: users, regions, reviews
CREATE TABLE regions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50) NOT NULL
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL,
    region_id INT REFERENCES regions(id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reviews (
    id SERIAL PRIMARY KEY,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO regions (name, country) VALUES
    ('West', 'US'), ('East', 'US'), ('Central', 'US');

INSERT INTO users (name, email, region_id) VALUES
    ('Alice Johnson', 'alice@example.com', 1),
    ('Bob Smith', 'bob@example.com', 2),
    ('Carol Williams', 'carol@example.com', 1),
    ('David Brown', 'david@example.com', 3),
    ('Eve Davis', 'eve@example.com', 2);

INSERT INTO reviews (product_id, user_id, rating, comment) VALUES
    (1, 1, 5, 'Excellent product'),
    (2, 2, 4, 'Good value'),
    (3, 1, 3, 'Average quality'),
    (1, 3, 5, 'Would buy again');

CREATE INDEX idx_users_region ON users(region_id);
CREATE INDEX idx_reviews_product ON reviews(product_id);
CREATE INDEX idx_reviews_user ON reviews(user_id);
