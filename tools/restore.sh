#!/usr/bin/env bash
# Restaura el launcher original de OPPO como predeterminado.
# Uso: ANDROID_SERIAL=<serie> tools/restore.sh  (o pasa la serie como argumento)
set -euo pipefail
S="${1:-${ANDROID_SERIAL:-<ADB_SERIE_OPPO>}}"
if ! adb -s "$S" get-state >/dev/null 2>&1; then
  echo "El dispositivo '$S' no está conectado. Dispositivos:" >&2; adb devices >&2
  echo "Pasa la serie correcta como primer argumento (no se elige ninguna automáticamente para no tocar otro teléfono)." >&2; exit 1
fi
adb -s "$S" shell cmd role add-role-holder --user 0 android.app.role.HOME com.android.launcher
adb -s "$S" shell input keyevent KEYCODE_HOME
adb -s "$S" shell cmd role get-role-holders android.app.role.HOME
