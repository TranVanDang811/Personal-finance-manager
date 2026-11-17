-- ========================================================
-- V4: PERFORMANCE & QUERY OPTIMIZATION INDEXES
-- ========================================================

-- 1. Accelerate recurring transaction reference clearing and lookups in transactions table
CREATE INDEX idx_trans_recurring
    ON transactions (recurring_transaction_id);

-- 2. Covering composite index for paginated saving goal contributions sorted by date
CREATE INDEX idx_saving_contrib_goal_user_date
    ON saving_goal_contributions (saving_goal_id, user_id, contribution_date DESC);

-- 3. Composite index for account name uniqueness and lookup per user
CREATE INDEX idx_accounts_user_name_deleted
    ON accounts (user_id, account_name, is_deleted);
