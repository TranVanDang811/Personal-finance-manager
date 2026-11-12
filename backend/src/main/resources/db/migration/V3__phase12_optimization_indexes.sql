-- ========================================================
-- V3: PHASE 12 QUERY & PERFORMANCE OPTIMIZATION INDEXES
-- ========================================================

-- Accelerate category expense aggregation queries by user, category, type and date range
CREATE INDEX idx_trans_user_cat_type_date
    ON transactions (user_id, category_id, transaction_type, transaction_date);

-- Accelerate refresh token expiry / revocation scans
CREATE INDEX idx_refresh_tokens_expiry_revoked
    ON refresh_tokens (expiry_date, revoked);

