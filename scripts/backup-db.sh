#!/usr/bin/env bash
# =====================================================================
# DangCap PFM — Automated Database Backup Script (Linux / Production)
# Usage: ./scripts/backup-db.sh
# Add to crontab for daily backup at 02:00 AM:
# 0 2 * * * /path/to/personal-finance-manager/scripts/backup-db.sh >> /var/log/pfm-backup.log 2>&1
# =====================================================================

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/backups"
TIMESTAMP="$(date +'%Y%m%d_%H%M%S')"
BACKUP_FILE="${BACKUP_DIR}/pfm_db_${TIMESTAMP}.sql.gz"
RETENTION_DAYS=14

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

mkdir -p "${BACKUP_DIR}"

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Starting database backup for database '${MYSQL_DB}'..."

# Dump database using mysqldump with single transaction (consistent snapshot without locking tables)
docker exec "${DB_CONTAINER}" mysqldump \
    -u root \
    -p"${ROOT_PASSWORD}" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --default-character-set=utf8mb4 \
    "${MYSQL_DB}" | gzip -9 > "${BACKUP_FILE}"

if [ -s "${BACKUP_FILE}" ]; then
    FILE_SIZE="$(du -h "${BACKUP_FILE}" | cut -f1)"
    echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Backup completed successfully: ${BACKUP_FILE} (${FILE_SIZE})"
else
    echo "[ERROR] Backup failed: output file is empty" >&2
    rm -f "${BACKUP_FILE}"
    exit 1
fi

# Rotate backups older than RETENTION_DAYS
echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Purging backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -name "pfm_db_*.sql.gz" -type f -mtime +"${RETENTION_DAYS}" -print -delete

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] All backup operations finished."
