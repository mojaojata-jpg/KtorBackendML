CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- 1) ADMINS
-- =========================================================
CREATE TABLE admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 2) PRODUCTS
-- =========================================================
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    unit_label VARCHAR(30) NOT NULL DEFAULT 'pcs',
    min_stock_threshold INT NOT NULL DEFAULT 0,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 3) PRODUCT RFID TAGS
--    1 tag fisik = 1 item/label
-- =========================================================
CREATE TABLE product_rfid_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    tag_uid VARCHAR(100) UNIQUE NOT NULL,
    tag_label VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, LOST, DAMAGED
    registered_by_admin_id UUID,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_rfid_tags_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_product_rfid_tags_admin
        FOREIGN KEY (registered_by_admin_id) REFERENCES admins(id)
        ON DELETE SET NULL
);

-- =========================================================
-- 4) INVENTORY EVENTS
--    Semua aktivitas scan masuk ke sini
-- =========================================================
CREATE TABLE inventory_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    tag_id UUID,
    admin_id UUID,
    event_type VARCHAR(30) NOT NULL, -- REGISTER, IN, OUT, ADJUSTMENT
    quantity INT NOT NULL DEFAULT 1,
    note TEXT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_events_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_inventory_events_tag
        FOREIGN KEY (tag_id) REFERENCES product_rfid_tags(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_inventory_events_admin
        FOREIGN KEY (admin_id) REFERENCES admins(id)
        ON DELETE SET NULL
);

-- =========================================================
-- 5) INVENTORY SNAPSHOTS
--    Stok terbaru per produk
-- =========================================================
CREATE TABLE inventory_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    current_stock INT NOT NULL,
    status VARCHAR(50) NOT NULL, -- OUT_OF_STOCK, LOW_STOCK, SUFFICIENT
    snapshot_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_event_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_snapshots_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_inventory_snapshots_event
        FOREIGN KEY (source_event_id) REFERENCES inventory_events(id)
        ON DELETE SET NULL
);

-- =========================================================
-- INDEXES
-- =========================================================
CREATE INDEX idx_product_rfid_tags_product_id
ON product_rfid_tags(product_id);

CREATE INDEX idx_product_rfid_tags_tag_uid
ON product_rfid_tags(tag_uid);

CREATE INDEX idx_inventory_events_product_id
ON inventory_events(product_id);

CREATE INDEX idx_inventory_events_tag_id
ON inventory_events(tag_id);

CREATE INDEX idx_inventory_events_recorded_at
ON inventory_events(recorded_at DESC);

CREATE INDEX idx_inventory_snapshots_product_id
ON inventory_snapshots(product_id);

CREATE INDEX idx_inventory_snapshots_snapshot_time
ON inventory_snapshots(snapshot_time DESC);

-- =========================================================
-- 7) DAILY INVENTORY AGGREGATES
--    Menyimpan rekap total IN dan OUT harian per produk
-- =========================================================
CREATE TABLE daily_aggregates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    date DATE NOT NULL,
    total_in INT NOT NULL DEFAULT 0,
    total_out INT NOT NULL DEFAULT 0,
    net_flow INT NOT NULL DEFAULT 0, -- (total_in - total_out)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_daily_aggregates_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE,

    -- Memastikan tidak ada duplikat data aggregate untuk tanggal dan produk yang sama
    CONSTRAINT uq_product_date UNIQUE(product_id, date)
);

-- =========================================================
-- 8) FORECASTING RESULTS (PROPHET)
--    Menyimpan hasil prediksi time series dari model Prophet
-- =========================================================
CREATE TABLE forecasting_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    target_date DATE NOT NULL,
    predicted_value NUMERIC(10,2) NOT NULL, -- Total OUT yang diprediksi
    lower_bound NUMERIC(10,2) NOT NULL,
    upper_bound NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_forecasting_results_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_forecast_product_date UNIQUE(product_id, target_date)
);

-- Indexes untuk mempercepat query saat render chart
CREATE INDEX idx_daily_aggregates_product_date
ON daily_aggregates(product_id, date DESC);

CREATE INDEX idx_forecasting_results_product_date
ON forecasting_results(product_id, target_date ASC);