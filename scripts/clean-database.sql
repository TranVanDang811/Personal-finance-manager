-- =====================================================================
-- DangCap (Personal Finance Manager) — Clean Database Script
-- Purges all test users, accounts, transactions, and session tokens
-- Preserves system schema structure and 15 default system categories
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE transactions;
TRUNCATE TABLE recurring_transactions;
TRUNCATE TABLE saving_goal_contributions;
TRUNCATE TABLE saving_goals;
TRUNCATE TABLE budgets;
TRUNCATE TABLE accounts;
TRUNCATE TABLE notifications;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE password_reset_tokens;

-- Remove custom user categories, keep 15 default system categories
DELETE FROM categories WHERE is_system = FALSE OR user_id IS NOT NULL;

-- Remove all registered test users
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;
