param([switch]$SetupOnly, [switch]$Test)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$envFile = Join-Path $projectRoot '.env'
if (!(Test-Path -LiteralPath $envFile)) {
    function New-Secret {
        $bytes = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        $rng.GetBytes($bytes)
        $rng.Dispose()
        return [Convert]::ToBase64String($bytes)
    }
    # Generate once; never overwrite an existing database password or JWT key.
    $content = @('DB_NAME=pashe_db', 'DB_USERNAME=pashe', 'DB_HOST_PORT=5433',
        "DB_PASSWORD=$(New-Secret)", "JWT_SECRET=$(New-Secret)",
        'SPRING_PROFILES_ACTIVE=dev', 'SEED_TEST_USER=true',
        'SEED_TEST_PASSWORD=Pashe-Local-Test-2026!')
    [System.IO.File]::WriteAllLines($envFile, $content)
}
foreach ($line in [System.IO.File]::ReadAllLines($envFile)) {
    if ($line -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
if (!$env:JWT_SECRET) { throw 'Add a random Base64 JWT_SECRET (32+ bytes) to .env.' }
$docker = Get-Command docker.exe -ErrorAction SilentlyContinue
if ($docker) { $dockerPath=$docker.Source }
else { $dockerPath=Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe' }
& $dockerPath compose up -d --wait postgres mailpit
if ($LASTEXITCODE -ne 0) { throw 'Docker services failed to start.' }
if ($SetupOnly) { exit 0 }
if (Test-Path 'C:\Program Files\Java\jdk-24') { $env:JAVA_HOME='C:\Program Files\Java\jdk-24' }
$env:Path="$env:JAVA_HOME\bin;$env:Path"
Set-Location (Join-Path $projectRoot 'backend')
if ($Test) { $env:SPRING_PROFILES_ACTIVE='test'; & .\mvnw.cmd test } else { & .\mvnw.cmd spring-boot:run }
exit $LASTEXITCODE
