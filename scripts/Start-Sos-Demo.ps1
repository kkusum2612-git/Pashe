$ErrorActionPreference = 'Stop'
# Keep presentation data in its own schema. Existing schemas and accounts are untouched.
$env:SPRING_FLYWAY_SCHEMAS = 'pashe_sos_demo'
$env:SPRING_DATASOURCE_HIKARI_SCHEMA = 'pashe_sos_demo'
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Start-Local.ps1')
exit $LASTEXITCODE
