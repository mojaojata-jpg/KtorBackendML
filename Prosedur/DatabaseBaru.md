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
-- 6) PREDICTION RESULTS
-- =========================================================
CREATE TABLE prediction_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    current_stock INT NOT NULL,
    predicted_days_remaining INT NOT NULL,
    predicted_stock_out_date DATE NOT NULL,
    confidence_score NUMERIC(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_prediction_results_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE
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

CREATE INDEX idx_prediction_results_product_id
ON prediction_results(product_id);

CREATE INDEX idx_prediction_results_created_at
ON prediction_results(created_at DESC);