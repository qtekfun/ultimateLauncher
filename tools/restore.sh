#!/usr/bin/env bash
# Restaura el launcher original de OPPO como predeterminado.
# Uso: tools/restore.sh [serial_adb]   (por defecto 897201dc)
set -euo pipefail
S="${1:-897201dc}"
adb -s "$S" shell cmd role add-role-holder --user 0 android.app.role.HOME com.android.launcher
adb -s "$S" shell cmd package set-home-activity --user 0 com.android.launcher/.Launcher || true
adb -s "$S" shell input keyevent KEYCODE_HOME
adb -s "$S" shell cmd role get-role-holders android.app.role.HOME
