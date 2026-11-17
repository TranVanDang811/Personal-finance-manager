# DangCap PFM — Database Restore Script (PowerShell / Windows)
param(
    [string]$BackupFile
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $ProjectRoot ".env"
$BackupDir = Join-Path $ProjectRoot "backups"

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
        }
    }
}

$Container = if ($env:DB_CONTAINER) { $env:DB_CONTAINER } else { "dangcap-mysql" }
$Database  = if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "pfm_db" }
$RootPass  = $env:MYSQL_ROOT_PASSWORD

if (-not $RootPass) {
    Write-Error "MYSQL_ROOT_PASSWORD is not set in .env"
    exit 1
}

if (-not $BackupFile) {
    $latest = Get-ChildItem -Path $BackupDir -Filter "pfm_db_*.sql*" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($latest) {
        $BackupFile = $latest.FullName
    }
}

if (-not (Test-Path $BackupFile)) {
    Write-Error "Backup file not found: $BackupFile"
    exit 1
}

Write-Host "Restoring database '$Database' from $BackupFile..."
Get-Content $BackupFile | docker exec -i $Container mysql -u root -p"$RootPass" $Database

Write-Host "Database restored successfully."
