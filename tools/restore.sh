#!/usr/bin/env bash
# Restaura el launcher original de OPPO como predeterminado.
# Uso: ANDROID_SERIAL=<serie> tools/restore.sh  (o pasa la serie como argumento)
set -euo pipefail
S="${1:-${ANDROID_SERIAL:-<ADB_SERIE_OPPO>}}"
adb -s "$S" shell cmd role add-role-holder --user 0 android.app.role.HOME com.android.launcher
adb -s "$S" shell input keyevent KEYCODE_HOME
adb -s "$S" shell cmd role get-role-holders android.app.role.HOME
