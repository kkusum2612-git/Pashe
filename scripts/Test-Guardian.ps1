param([switch]$IsolatedSchema = $true)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $projectRoot 'frontend')
try {
    foreach ($arguments in @(@('test','--','--maxWorkers=1','--testTimeout=15000'), @('run','build'), @('run','lint'))) {
        & npm.cmd @arguments
        if ($LASTEXITCODE -ne 0) { throw "Frontend check failed: $($arguments -join ' ')" }
    }
} finally { Pop-Location }
$previousSchemas = $env:SPRING_FLYWAY_SCHEMAS
$previousPoolSchema = $env:SPRING_DATASOURCE_HIKARI_SCHEMA
try {
    if ($IsolatedSchema) {
        # A fresh named schema in the local database; never drops existing data.
        $schema = 'guardian_validation_' + (Get-Date -Format 'yyyyMMddHHmmss')
        $env:SPRING_FLYWAY_SCHEMAS = $schema
        $env:SPRING_DATASOURCE_HIKARI_SCHEMA = $schema
        Write-Output "Validation schema: $schema (retained for inspection)"
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Start-Local.ps1') -Test
    if ($LASTEXITCODE -ne 0) { throw 'Backend integration tests failed.' }
} finally {
    $env:SPRING_FLYWAY_SCHEMAS = $previousSchemas
    $env:SPRING_DATASOURCE_HIKARI_SCHEMA = $previousPoolSchema
}
Write-Output 'PASS: Guardian and existing feature tests, frontend build and lint.'
