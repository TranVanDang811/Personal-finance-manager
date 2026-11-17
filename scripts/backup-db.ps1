# =====================================================================
# DangCap PFM — Automated Database Backup Script (Windows / PowerShell)
# Usage: .\scripts\backup-db.ps1
# =====================================================================

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$BackupDir = Join-Path $ProjectRoot "backups"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupFile = Join-Path $BackupDir "pfm_db_$Timestamp.sql"
$RetentionDays = 14

# Parse .env file
$EnvFile = Join-Path $ProjectRoot ".env"
$RootPassword = ""
$DbName = "pfm_db"

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim()
            if ($key -eq "MYSQL_ROOT_PASSWORD") { $RootPassword = $val }
            if ($key -eq "MYSQL_DATABASE") { $DbName = $val }
        }
    }
}

if (-not $RootPassword) {
    Write-Error "MYSQL_ROOT_PASSWORD is not set in .env"
    exit 1
}

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

Write-Host "[$((Get-Date).ToUniversalTime().ToString("u"))] Starting database backup for '$DbName'..."

# Execute mysqldump inside dangcap-mysql container
$cmd = "docker exec dangcap-mysql mysqldump -u root -p`"$RootPassword`" --single-transaction --quick --routines --triggers --default-character-set=utf8mb4 `"$DbName`""
cmd.exe /c "$cmd > `"$BackupFile`""

if ((Test-Path $BackupFile) -and (Get-Item $BackupFile).Length -gt 0) {
    $sizeKb = [math]::Round((Get-Item $BackupFile).Length / 1KB, 2)
    Write-Host "[$((Get-Date).ToUniversalTime().ToString("u"))] Backup completed successfully: $BackupFile ($sizeKb KB)"
} else {
    Write-Error "Backup failed: Output file is empty or missing."
    if (Test-Path $BackupFile) { Remove-Item $BackupFile }
    exit 1
}

# Purge old backups
$cutoffDate = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem -Path $BackupDir -Filter "pfm_db_*.sql" | Where-Object { $_.LastWriteTime -lt $cutoffDate } | ForEach-Object {
    Write-Host "Removing old backup: $($_.FullName)"
    Remove-Item $_.FullName -Force
}

Write-Host "[$((Get-Date).ToUniversalTime().ToString("u"))] Backup process finished."
