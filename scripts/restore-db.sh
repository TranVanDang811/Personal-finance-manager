#!/usr/bin/env bash
# =====================================================================
# DangCap PFM — Database Restore Script (Linux / Production)
# Usage: ./scripts/restore-db.sh [path/to/backup.sql.gz]
# If no file is provided, it restores the latest backup in backups/
# =====================================================================

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/backups"

# Source .env file if present
if [ -f "${PROJECT_ROOT}/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "${PROJECT_ROOT}/.env"
    set +a
fi

DB_CONTAINER="${DB_CONTAINER:-dangcap-mysql}"
MYSQL_DB="${MYSQL_DATABASE:-pfm_db}"
ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}"

if [ -z "${ROOT_PASSWORD}" ]; then
    echo "[ERROR] MYSQL_ROOT_PASSWORD is not set in .env" >&2
    exit 1
fi

TARGET_FILE="$1"
if [ -z "${TARGET_FILE}" ]; then
    # Pick the most recent backup
    TARGET_FILE="$(find "${BACKUP_DIR}" -name "pfm_db_*.sql.gz" -type f | sort -r | head -n 1 || true)"
fi

if [ -z "${TARGET_FILE}" ] || [ ! -f "${TARGET_FILE}" ]; then
    echo "[ERROR] Backup file not found: ${TARGET_FILE}" >&2
    echo "Usage: $0 [path/to/backup.sql.gz]" >&2
    exit 1
fi

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Restoring database '${MYSQL_DB}' from: ${TARGET_FILE}..."

if [[ "${TARGET_FILE}" == *.gz ]]; then
    gzip -dc "${TARGET_FILE}" | docker exec -i "${DB_CONTAINER}" mysql -u root -p"${ROOT_PASSWORD}" "${MYSQL_DB}"
else
    docker exec -i "${DB_CONTAINER}" mysql -u root -p"${ROOT_PASSWORD}" "${MYSQL_DB}" < "${TARGET_FILE}"
fi

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Database restored successfully."
