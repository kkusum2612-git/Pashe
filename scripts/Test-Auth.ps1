$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$frontend = Join-Path $projectRoot 'frontend'

if (!(Test-Path (Join-Path $frontend 'node_modules'))) {
    throw 'Frontend dependencies are missing. Run npm.cmd ci in the frontend directory first.'
}

Push-Location $frontend
try {
    foreach ($arguments in @(
        @('test'),
        @('run', 'build'),
        @('run', 'lint')
    )) {
        & npm.cmd @arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend check failed: npm $($arguments -join ' ')"
        }
    }
}
finally {
    Pop-Location
}

$startScript = Join-Path $PSScriptRoot 'Start-Local.ps1'
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $startScript -Test
if ($LASTEXITCODE -ne 0) {
    throw 'Backend authentication integration tests failed.'
}

Write-Output 'PASS: frontend auth tests, frontend build/lint, and backend integration tests.'
