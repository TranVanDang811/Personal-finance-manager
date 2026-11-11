-- ========================================================
-- V1: INITIALIZE DATABASE SCHEMA (MYSQL 8+)
-- ========================================================

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. REFRESH_TOKENS
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_info VARCHAR(255) DEFAULT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- 3. PASSWORD_RESET_TOKENS
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pwd_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_pwd_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_pwd_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pwd_reset_user ON password_reset_tokens (user_id);

-- 4. ACCOUNTS
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    initial_balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    current_balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    description VARCHAR(255) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    allow_negative BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_accounts_user_deleted ON accounts (user_id, is_deleted);
CREATE INDEX idx_accounts_status ON accounts (status);

-- 5. CATEGORIES
CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    category_type VARCHAR(20) NOT NULL,
    icon VARCHAR(50) NOT NULL DEFAULT 'tag',
    color VARCHAR(20) NOT NULL DEFAULT '#6B7280',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_categories_user_lookup ON categories (user_id, is_system, is_deleted);
CREATE INDEX idx_categories_type ON categories (category_type);

-- 6. RECURRING_TRANSACTIONS
CREATE TABLE IF NOT EXISTS recurring_transactions (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    to_account_id VARCHAR(36) DEFAULT NULL,
    category_id VARCHAR(36) DEFAULT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    description VARCHAR(255) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE DEFAULT NULL,
    next_execution_date DATE NOT NULL,
    last_executed_at TIMESTAMP NULL DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_recurring_trans PRIMARY KEY (id),
    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_to_account FOREIGN KEY (to_account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_recurring_scheduler ON recurring_transactions (status, next_execution_date);
CREATE INDEX idx_recurring_user ON recurring_transactions (user_id);

-- 7. TRANSACTIONS
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    to_account_id VARCHAR(36) DEFAULT NULL,
    category_id VARCHAR(36) DEFAULT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    description VARCHAR(255) NOT NULL,
    note TEXT DEFAULT NULL,
    transaction_date TIMESTAMP NOT NULL,
    attachment_url VARCHAR(255) DEFAULT NULL,
    recurring_transaction_id VARCHAR(36) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_trans_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_trans_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trans_to_account FOREIGN KEY (to_account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trans_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trans_recurring FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_trans_user_date ON transactions (user_id, transaction_date DESC);
CREATE INDEX idx_trans_account ON transactions (account_id);
CREATE INDEX idx_trans_to_account ON transactions (to_account_id);
CREATE INDEX idx_trans_category ON transactions (category_id);
CREATE INDEX idx_trans_user_type_date ON transactions (user_id, transaction_type, transaction_date);

-- 8. BUDGETS
CREATE TABLE IF NOT EXISTS budgets (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    limit_amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT uq_budget_user_cat_period UNIQUE (user_id, category_id, month, year),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_budgets_user_period ON budgets (user_id, year, month);

-- 9. SAVING_GOALS
CREATE TABLE IF NOT EXISTS saving_goals (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    target_amount DECIMAL(19, 4) NOT NULL,
    current_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    start_date DATE NOT NULL,
    target_date DATE NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    color VARCHAR(20) NOT NULL DEFAULT '#10B981',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_saving_goals PRIMARY KEY (id),
    CONSTRAINT fk_saving_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_saving_goals_user_status ON saving_goals (user_id, status);

-- 10. SAVING_GOAL_CONTRIBUTIONS
CREATE TABLE IF NOT EXISTS saving_goal_contributions (
    id VARCHAR(36) NOT NULL,
    saving_goal_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    transaction_id VARCHAR(36) DEFAULT NULL,
    contribution_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    note VARCHAR(255) DEFAULT NULL,
    contribution_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_saving_contributions PRIMARY KEY (id),
    CONSTRAINT fk_saving_contrib_goal FOREIGN KEY (saving_goal_id) REFERENCES saving_goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_saving_contrib_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_saving_contrib_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_saving_contrib_trans FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_saving_contrib_goal ON saving_goal_contributions (saving_goal_id);
CREATE INDEX idx_saving_contrib_user ON saving_goal_contributions (user_id);

-- 11. NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    reference_id VARCHAR(36) DEFAULT NULL,
    reference_type VARCHAR(30) DEFAULT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read, created_at DESC);

