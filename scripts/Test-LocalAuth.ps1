# Runs against the explicitly seeded local test account. Resets its password to the same test value.
$ErrorActionPreference='Stop'
$api='http://127.0.0.1:8080/api/v1'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$password='Pashe-Local-Test-2026!'
function Send-AccountRequest([string]$path,[object]$body) {
    $csrf=Invoke-RestMethod "$api/auth/csrf" -WebSession $session -TimeoutSec 15
    $json=if ($null -eq $body) { '{}' } else { $body | ConvertTo-Json -Compress }
    Invoke-RestMethod "$api$path" -Method Post -WebSession $session -Headers @{'X-CSRF-TOKEN'=$csrf.token} -ContentType 'application/json' -Body $json -TimeoutSec 15
}
$user=Send-AccountRequest '/auth/login' @{email='tester@pashe.local';password=$password}
if (!$user.emailVerified -or $user.roles -notcontains 'USER') { throw 'Seed account verification failed.' }
$profile=Invoke-RestMethod "$api/users/me" -WebSession $session -TimeoutSec 15
if ($profile.email -ne 'tester@pashe.local') { throw 'Profile lookup failed.' }
$refreshed=Send-AccountRequest '/auth/refresh' $null
if ($refreshed.id -ne $user.id) { throw 'Refresh identity mismatch.' }
try { Invoke-RestMethod "$api/admin/access" -WebSession $session -TimeoutSec 15 | Out-Null; throw 'Role authorization failed.' }
catch [System.Net.WebException] { if ([int]$_.Exception.Response.StatusCode -ne 403) { throw } }
Send-AccountRequest '/auth/forgot-password' @{email='tester@pashe.local'} | Out-Null
$message=$null
for ($attempt=0; $attempt -lt 20; $attempt++) {
    $messages=Invoke-RestMethod 'http://localhost:8025/api/v1/messages' -TimeoutSec 15
    $message=$messages.messages | Where-Object { $_.Subject -eq 'Reset your Pashe password' -and (@($_.To | ForEach-Object { $_.Address }) -contains 'tester@pashe.local') } | Select-Object -First 1
    if ($message) { break }
    Start-Sleep -Milliseconds 250
}
if (!$message) { throw 'Reset email was not delivered to Mailpit.' }
$mail=Invoke-RestMethod "http://localhost:8025/api/v1/message/$($message.ID)" -TimeoutSec 15
if ($mail.Text -notmatch 'token=([A-Za-z0-9_-]+)') { throw 'Reset link missing.' }
$resetToken=$matches[1]
Send-AccountRequest '/auth/reset-password' @{token=$resetToken;password=$password} | Out-Null
Send-AccountRequest '/auth/login' @{email='tester@pashe.local';password=$password} | Out-Null
Send-AccountRequest '/auth/logout' $null | Out-Null
try { Invoke-RestMethod "$api/auth/session" -WebSession $session -TimeoutSec 15 | Out-Null; throw 'Logout failed.' }
catch [System.Net.WebException] { if ([int]$_.Exception.Response.StatusCode -ne 401) { throw } }
Write-Output 'PASS: seeded login, profile, refresh, role denial, SMTP reset email, password reset, re-login, logout.'
