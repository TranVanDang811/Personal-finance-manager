-- ========================================================
-- ROLLBACK SCRIPT FOR V4: DROP OPTIMIZATION INDEXES
-- Use in emergency rollback procedure if needed
-- ========================================================

DROP INDEX idx_trans_recurring ON transactions;
DROP INDEX idx_saving_contrib_goal_user_date ON saving_goal_contributions;
DROP INDEX idx_accounts_user_name_deleted ON accounts;
