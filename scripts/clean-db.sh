#!/usr/bin/env bash
# =====================================================================
# DangCap PFM — Clean Database Script (Linux / Production)
# Usage: ./scripts/clean-db.sh
# Purges all test users, accounts, transactions, and session tokens
# Preserves schema migrations and 15 default system categories
# =====================================================================

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Source .env file if present
if [ -f "${PROJECT_ROOT}/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "${PROJECT_ROOT}/.env"
    set +a
fi

DB_CONTAINER="${DB_CONTAINER:-dangcap-mysql}"
REDIS_CONTAINER="${REDIS_CONTAINER:-dangcap-redis}"
MYSQL_DB="${MYSQL_DATABASE:-pfm_db}"
ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}"
REDIS_PASS="${REDIS_PASSWORD}"

if [ -z "${ROOT_PASSWORD}" ]; then
    echo "[ERROR] MYSQL_ROOT_PASSWORD is not set in .env" >&2
    exit 1
fi

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Cleaning database '${MYSQL_DB}'..."

docker exec -i "${DB_CONTAINER}" mysql -u root -p"${ROOT_PASSWORD}" "${MYSQL_DB}" << 'EOF'
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
DELETE FROM categories WHERE is_system = FALSE OR user_id IS NOT NULL;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;
EOF

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Database cleaned successfully."

# Flush Redis
if [ -n "${REDIS_PASS}" ]; then
    echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Flushing Redis cache..."
    docker exec "${REDIS_CONTAINER}" redis-cli -a "${REDIS_PASS}" FLUSHALL >/dev/null 2>&1 || true
fi

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] All cleaning operations completed."
