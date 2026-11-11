-- ========================================================
-- V2: SEED SYSTEM DEFAULT CATEGORIES
-- ========================================================

-- INCOME CATEGORIES
INSERT INTO categories (id, user_id, name, category_type, icon, color, is_system, is_deleted) VALUES
('00000000-0000-0000-0001-000000000001', NULL, 'Salary', 'INCOME', 'banknotes', '#10B981', TRUE, FALSE),
('00000000-0000-0000-0001-000000000002', NULL, 'Bonus', 'INCOME', 'gift', '#059669', TRUE, FALSE),
('00000000-0000-0000-0001-000000000003', NULL, 'Investment', 'INCOME', 'chart-bar', '#047857', TRUE, FALSE),
('00000000-0000-0000-0001-000000000004', NULL, 'Business', 'INCOME', 'building-office-2', '#065F46', TRUE, FALSE),
('00000000-0000-0000-0001-000000000005', NULL, 'Other Income', 'INCOME', 'plus-circle', '#34D399', TRUE, FALSE);

-- EXPENSE CATEGORIES
INSERT INTO categories (id, user_id, name, category_type, icon, color, is_system, is_deleted) VALUES
('00000000-0000-0000-0002-000000000001', NULL, 'Food & Drink', 'EXPENSE', 'cake', '#EF4444', TRUE, FALSE),
('00000000-0000-0000-0002-000000000002', NULL, 'Transportation', 'EXPENSE', 'truck', '#F97316', TRUE, FALSE),
('00000000-0000-0000-0002-000000000003', NULL, 'Shopping', 'EXPENSE', 'shopping-bag', '#EC4899', TRUE, FALSE),
('00000000-0000-0000-0002-000000000004', NULL, 'Entertainment', 'EXPENSE', 'film', '#8B5CF6', TRUE, FALSE),
('00000000-0000-0000-0002-000000000005', NULL, 'Healthcare', 'EXPENSE', 'heart', '#06B6D4', TRUE, FALSE),
('00000000-0000-0000-0002-000000000006', NULL, 'Education', 'EXPENSE', 'academic-cap', '#3B82F6', TRUE, FALSE),
('00000000-0000-0000-0002-000000000007', NULL, 'Housing', 'EXPENSE', 'home', '#6366F1', TRUE, FALSE),
('00000000-0000-0000-0002-000000000008', NULL, 'Utilities', 'EXPENSE', 'bolt', '#F59E0B', TRUE, FALSE),
('00000000-0000-0000-0002-000000000009', NULL, 'Travel', 'EXPENSE', 'globe-americas', '#14B8A6', TRUE, FALSE),
('00000000-0000-0000-0002-000000000010', NULL, 'Other Expense', 'EXPENSE', 'minus-circle', '#6B7280', TRUE, FALSE);

