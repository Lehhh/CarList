-- =========================
-- CarStoreView - V1 Init
-- =========================

CREATE TABLE IF NOT EXISTS car_view (
                                        id BIGINT PRIMARY KEY,
                                        brand VARCHAR(60) NOT NULL,
    model VARCHAR(80) NOT NULL,
    car_year INT NOT NULL,
    color VARCHAR(30) NOT NULL,
    price NUMERIC(15,2) NOT NULL,
    sold BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL
    );

CREATE TABLE IF NOT EXISTS sales (
                                     id BIGSERIAL PRIMARY KEY,
                                     car_id BIGINT NOT NULL UNIQUE,
                                     status VARCHAR(20) NOT NULL,
    locked_price NUMERIC(12,2) NOT NULL,
    reserved_until TIMESTAMPTZ NULL,
    payment_code VARCHAR(255) UNIQUE NULL,
    buyer_cpf VARCHAR(11) NULL,
    sold_at TIMESTAMPTZ NULL,
    version BIGINT NULL
    );

CREATE INDEX IF NOT EXISTS idx_sales_status_price
    ON sales (status, locked_price);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sale_car
    ON sales (car_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sale_payment
    ON sales (payment_code);
