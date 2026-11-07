@echo off
rem Puts the server back to the start of the demo (docs/spec.md section 2).
rem Double-click it, or run it from a terminal:
rem   reset-demo.bat            every camera back "In store" with Siti; phones stay enrolled
rem   reset-demo.bat reenrol    also withdraws every phone, so each one shows Enrol after its next sync
rem Reads SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY and DEMO_RESET_TOKEN from local.properties.
setlocal
cd /d "%~dp0"

set "WITHDRAW=%~1"
if "%WITHDRAW%"=="" (
  set /p "ANS=Also withdraw every phone so each one enrols again? [y/N] "
)
if /i "%ANS%"=="y" set "WITHDRAW=reenrol"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p = @{}; Get-Content 'local.properties' | ForEach-Object { if ($_ -match '^([A-Za-z_.]+)=(.*)$') { $p[$matches[1]] = $matches[2].Trim() } };" ^
  "foreach ($k in 'SUPABASE_URL','SUPABASE_PUBLISHABLE_KEY','DEMO_RESET_TOKEN') { if (-not $p[$k]) { Write-Host \"Missing $k in local.properties\"; exit 1 } };" ^
  "$body = @{ p_token = $p['DEMO_RESET_TOKEN']; p_withdraw = ('%WITHDRAW%' -eq 'reenrol') } | ConvertTo-Json;" ^
  "try { $r = Invoke-RestMethod -Method Post -Uri ($p['SUPABASE_URL'].TrimEnd('/') + '/rest/v1/rpc/demo_reset') -Headers @{ apikey = $p['SUPABASE_PUBLISHABLE_KEY'] } -ContentType 'application/json' -Body $body } catch { Write-Host ('Reset failed: ' + $_.Exception.Message); exit 1 };" ^
  "if ($r.result -ne 'reset') { Write-Host ('Reset refused: ' + $r.reason); exit 1 };" ^
  "Write-Host ('Reset done. Handovers deleted: ' + $r.handovers_deleted + '. Phones withdrawn: ' + $r.phones_withdrawn + '.')"
if errorlevel 1 goto end

echo.
if /i "%WITHDRAW%"=="reenrol" (
  echo Now on each phone: open Tenet and let it sync. It shows Enrol: enrol it as its person.
) else (
  echo Now on each phone: Me, tap the version 5 times, Debug, Clear local data, then Sync now.
)

:end
echo.
pause
