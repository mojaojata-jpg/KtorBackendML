CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE admins (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
name VARCHAR(100) NOT NULL,
email VARCHAR(150) UNIQUE NOT NULL,
password_hash TEXT NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
name VARCHAR(100) NOT NULL,
code VARCHAR(50) UNIQUE NOT NULL,
unit_weight NUMERIC(10,2) NOT NULL,
min_stock_threshold INT NOT NULL DEFAULT 0,
description TEXT,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE iot_devices (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
device_code VARCHAR(100) UNIQUE NOT NULL,
device_name VARCHAR(100) NOT NULL,
product_id UUID NOT NULL,
status VARCHAR(50) NOT NULL,
last_seen_at TIMESTAMP,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_iot_devices_product
FOREIGN KEY (product_id) REFERENCES products(id)
ON DELETE CASCADE
);

CREATE TABLE sensor_readings (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
device_id UUID NOT NULL,
product_id UUID NOT NULL,
raw_weight NUMERIC(10,3) NOT NULL,
filtered_weight NUMERIC(10,3) NOT NULL,
estimated_stock INT NOT NULL,
validation_status VARCHAR(50) NOT NULL,
recorded_at TIMESTAMP NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_sensor_readings_device
FOREIGN KEY (device_id) REFERENCES iot_devices(id)
ON DELETE CASCADE,
CONSTRAINT fk_sensor_readings_product
FOREIGN KEY (product_id) REFERENCES products(id)
ON DELETE CASCADE
);

CREATE TABLE stock_snapshots (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
product_id UUID NOT NULL,
device_id UUID NOT NULL,
current_weight NUMERIC(10,3) NOT NULL,
current_stock INT NOT NULL,
status VARCHAR(50) NOT NULL,
snapshot_time TIMESTAMP NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_stock_snapshots_product
FOREIGN KEY (product_id) REFERENCES products(id)
ON DELETE CASCADE,
CONSTRAINT fk_stock_snapshots_device
FOREIGN KEY (device_id) REFERENCES iot_devices(id)
ON DELETE CASCADE
);

CREATE TABLE prediction_results (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
product_id UUID NOT NULL,
model_name VARCHAR(100) NOT NULL,
model_version VARCHAR(50) NOT NULL,
predicted_days_remaining INT NOT NULL,
predicted_stock_out_date DATE NOT NULL,
confidence_score NUMERIC(5,2),
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_prediction_results_product
FOREIGN KEY (product_id) REFERENCES products(id)
ON DELETE CASCADE
);

CREATE TABLE stock_transactions (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
product_id UUID NOT NULL,
admin_id UUID NOT NULL,
transaction_type VARCHAR(50) NOT NULL,
quantity INT NOT NULL,
notes TEXT,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT fk_stock_transactions_product
FOREIGN KEY (product_id) REFERENCES products(id)
ON DELETE CASCADE,
CONSTRAINT fk_stock_transactions_admin
FOREIGN KEY (admin_id) REFERENCES admins(id)
ON DELETE CASCADE
);

CREATE INDEX idx_iot_devices_product_id
ON iot_devices(product_id);

CREATE INDEX idx_sensor_readings_device_id
ON sensor_readings(device_id);

CREATE INDEX idx_sensor_readings_product_id
ON sensor_readings(product_id);

CREATE INDEX idx_sensor_readings_recorded_at
ON sensor_readings(recorded_at DESC);

CREATE INDEX idx_sensor_readings_product_recorded
ON sensor_readings(product_id, recorded_at DESC);

CREATE INDEX idx_stock_snapshots_product_id
ON stock_snapshots(product_id);

CREATE INDEX idx_stock_snapshots_device_id
ON stock_snapshots(device_id);

CREATE INDEX idx_stock_snapshots_snapshot_time
ON stock_snapshots(snapshot_time DESC);

CREATE INDEX idx_prediction_results_product_id
ON prediction_results(product_id);

CREATE INDEX idx_prediction_results_created_at
ON prediction_results(created_at DESC);

CREATE INDEX idx_stock_transactions_product_id
ON stock_transactions(product_id);

CREATE INDEX idx_stock_transactions_admin_id
ON stock_transactions(admin_id);

CREATE INDEX idx_stock_transactions_created_at
ON stock_transactions(created_at DESC);