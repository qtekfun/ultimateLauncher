#!/usr/bin/env bash
# Comprobación de docs/09: la variante default no puede declarar INTERNET.
set -euo pipefail
cd "$(dirname "$0")/.."
APK="${1:-dist/ultimatelauncher-default-debug.apk}"
AA="${ANDROID_HOME:-$HOME/Android/Sdk}/cmdline-tools/latest/bin/apkanalyzer"
echo "Permisos de $APK:"; "$AA" manifest permissions "$APK"
if "$AA" manifest permissions "$APK" | grep -q "android.permission.INTERNET"; then
  echo "ERROR: la variante default declara INTERNET" >&2; exit 1
fi
echo "OK: sin INTERNET"
