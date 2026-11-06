#!/usr/bin/env bash
# Checks every connected phone is demo-ready in seconds. Works in Git Bash on the laptop.
#
#   bash scripts/demo-prep.sh                 one line per phone: model, Android, Tenet
#                                             version, airplane-mode radios, NFC, BT, battery
#   bash scripts/demo-prep.sh --install x.apk also installs that APK on each phone
#
# Tenet is checked under both application ids: release com.nushhack26.app and the
# debug side-load com.nushhack26.app.debug.
set -uo pipefail

apk=""
if [ "${1:-}" = "--install" ]; then
  apk="${2:-}"
  [ -n "$apk" ] && [ -f "$apk" ] || { echo "usage: $0 [--install <apk>]" >&2; exit 1; }
fi

adb="adb"
if ! command -v adb >/dev/null 2>&1; then
  sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
  adb="$sdk/platform-tools/adb"
fi
command -v "$adb" >/dev/null 2>&1 || { echo "adb not found" >&2; exit 1; }

serials=$("$adb" devices | tr -d '\r' | awk 'NR>1 && $2=="device" {print $1}')
[ -n "$serials" ] || { echo "No devices. Check adb devices." >&2; exit 1; }

if [ -n "$apk" ]; then
  for s in $serials; do
    echo "== $s: installing $apk"
    "$adb" -s "$s" install -r "$apk" | tr -d '\r' | tail -1
  done
fi

for s in $serials; do
  model=$("$adb" -s "$s" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
  android=$("$adb" -s "$s" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')

  rel=$("$adb" -s "$s" shell dumpsys package com.nushhack26.app 2>/dev/null | tr -d '\r' | grep -m1 versionName | sed 's/.*=//')
  dbg=$("$adb" -s "$s" shell dumpsys package com.nushhack26.app.debug 2>/dev/null | tr -d '\r' | grep -m1 versionName | sed 's/.*=//')
  ver="${rel:-not installed}"
  [ -n "$dbg" ] && ver="$ver (+debug $dbg)"

  radios=$("$adb" -s "$s" shell settings get global airplane_mode_radios 2>/dev/null | tr -d '\r ')
  case ",$radios," in
    *bluetooth*|*nfc*) radio="airplane cuts BT/NFC (run: settings put global airplane_mode_radios cell,wifi,wimax)" ;;
    *) radio="airplane keeps BT/NFC" ;;
  esac

  nfc=$("$adb" -s "$s" shell settings get global nfc_on 2>/dev/null | tr -d '\r')
  case "$nfc" in
    1) nfc="NFC on" ;;
    0) nfc="NFC OFF" ;;
    *)
      if "$adb" -s "$s" shell pm list features 2>/dev/null | tr -d '\r' | grep -qi 'android.hardware.nfc'; then
        nfc="NFC ?"
      else
        nfc="no NFC"
      fi ;;
  esac

  bt=$("$adb" -s "$s" shell settings get global bluetooth_on 2>/dev/null | tr -d '\r')
  case "$bt" in 1) bt="BT on" ;; 0) bt="BT OFF" ;; *) bt="BT ?" ;; esac

  bat=$("$adb" -s "$s" shell dumpsys battery 2>/dev/null | tr -d '\r' | grep -m1 'level:' | sed 's/.*: *//')

  printf '%-16s | Android %-3s | Tenet %-22s | %s | %s | %s | battery %s%%\n' \
    "${model:-?}" "${android:-?}" "$ver" "$radio" "$nfc" "$bt" "${bat:-?}"
done
