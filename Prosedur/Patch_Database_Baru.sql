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
