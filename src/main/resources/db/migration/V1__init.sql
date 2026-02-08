CREATE TABLE IF NOT EXISTS car_view (
                                        id BIGINT PRIMARY KEY,
                                        brand VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    year INT NOT NULL,
    color VARCHAR(40) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS sales (
                                     id BIGSERIAL PRIMARY KEY,
                                     car_id BIGINT NOT NULL UNIQUE,
                                     status VARCHAR(20) NOT NULL,
    locked_price NUMERIC(12,2) NOT NULL,
    reserved_until TIMESTAMP NULL,
    payment_code VARCHAR(255) UNIQUE NULL,
    buyer_cpf VARCHAR(11) NULL,
    sold_at TIMESTAMP NULL,
    version BIGINT NULL
    );

CREATE INDEX IF NOT EXISTS idx_sales_status_price ON sales(status, locked_price);
