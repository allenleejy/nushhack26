@echo off
rem Records the three demo phones at once, one video each, for editing the demo video.
rem Plug in all three phones first, then double-click. Run the demo (docs/spec.md section 2),
rem then press a key in this window to stop. Files go to Videos\tenet-demo\<date_time>\.
rem MKV, so a recording that's cut off still plays. Each phone's sound is recorded too.
setlocal
cd /d "%~dp0"

set "SCRCPY=scrcpy"
where scrcpy >nul 2>nul || set "SCRCPY=%LOCALAPPDATA%\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v5.0.1\scrcpy.exe"

for /f %%t in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HHmm" ^<nul') do set "STAMP=%%t"
set "OUT=%USERPROFILE%\Videos\tenet-demo\%STAMP%"
mkdir "%OUT%" 2>nul

rem Each phone gets its own port range, and they start 2 s apart, so the three don't collide.
call :rec WKXC5DKNUCU87D7H siti "A - Siti - Redmi 13" 27183:27189
call :rec 34685561460034M ravi "B - Ravi - vivo" 27190:27196
call :rec r4lvf67ldmvctsei weiming "C - Wei Ming - Redmi 11" 27197:27203

echo.
echo Recording to %OUT%
echo Run the demo. Press any key here to stop all the recordings.
pause >nul

rem Ask each scrcpy to close first so it finishes its file, then stop any that didn't.
taskkill /IM scrcpy.exe >nul 2>nul
ping -n 4 127.0.0.1 >nul
taskkill /F /IM scrcpy.exe >nul 2>nul

echo.
echo Saved in %OUT%:
dir /b "%OUT%"
echo.
pause
exit /b

:rec
rem No ( ) blocks here: the phone names would close them early.
adb -s %~1 get-state >nul 2>nul
if errorlevel 1 goto :missing
rem Each scrcpy runs in its own minimised console, with its log next to the video.
start "%~3" /min cmd /c ""%SCRCPY%" -s %~1 --record="%OUT%\%~2.mkv" --window-title="%~3" --window-height=720 --max-size=1080 --video-bit-rate=12M --no-audio-playback --no-control --port=%~4 > "%OUT%\%~2.log" 2>&1"
echo %~3: recording
ping -n 3 127.0.0.1 >nul
exit /b

:missing
echo %~3 is not connected: skipped.
exit /b
