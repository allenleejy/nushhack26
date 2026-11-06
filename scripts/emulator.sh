#!/usr/bin/env bash
# Starts an emulator if none is running, waits for it to boot, and prints its serial.
# Uses $AVD if set, else the first AVD. Works in Git Bash on Windows and on Linux.
set -euo pipefail

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$sdk" ]; then
  if [ -n "${LOCALAPPDATA:-}" ]; then sdk="$LOCALAPPDATA/Android/Sdk"; else sdk="$HOME/Android/Sdk"; fi
fi
emulator="$sdk/emulator/emulator"
[ -x "$emulator" ] || emulator="$sdk/emulator/emulator.exe"
adb="adb"
command -v adb >/dev/null 2>&1 || adb="$sdk/platform-tools/adb"

running() { "$adb" devices | tr -d '\r' | awk '$1 ~ /^emulator-/ && $2 == "device" { print $1; exit }'; }

serial="$(running || true)"
if [ -z "$serial" ]; then
  avd="${AVD:-$("$emulator" -list-avds 2>/dev/null | tr -d '\r' | grep -v '^INFO' | head -n 1)}"
  if [ -z "$avd" ]; then echo "No AVD found. Create one or set \$AVD." >&2; exit 1; fi
  echo "Starting emulator $avd..." >&2
  nohup "$emulator" -avd "$avd" -no-snapshot-save -no-boot-anim >/dev/null 2>&1 &
  for _ in $(seq 1 90); do
    serial="$(running || true)"
    [ -n "$serial" ] && break
    sleep 2
  done
  if [ -z "$serial" ]; then echo "Emulator didn't appear in adb." >&2; exit 1; fi
fi

for _ in $(seq 1 150); do
  booted="$("$adb" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  [ "$booted" = "1" ] && { echo "$serial"; exit 0; }
  sleep 2
done
echo "Emulator $serial didn't finish booting." >&2
exit 1
