#!/usr/bin/env bash
# Instala UltimateLauncher (variante default) y lo fija como launcher predeterminado.
# Uso: tools/install.sh [serie_adb] [apk]     (por defecto el CPH2841 por wifi y dist/ultimatelauncher-default-debug.apk)
# Para volver al launcher original: tools/restore.sh
set -euo pipefail
cd "$(dirname "$0")/.."
S="${1:-${ANDROID_SERIAL:-<ADB_SERIE_OPPO>}}"
APK="${2:-dist/ultimatelauncher-default-debug.apk}"
PKG=com.qtekfun.ultimatelauncher
echo "Launcher actual: $(adb -s "$S" shell cmd role get-role-holders android.app.role.HOME | tr -d '\r')"
adb -s "$S" install -r --user 0 "$APK"
adb -s "$S" shell cmd role add-role-holder --user 0 android.app.role.HOME "$PKG"
adb -s "$S" shell input keyevent KEYCODE_HOME
sleep 2
echo "Launcher ahora: $(adb -s "$S" shell cmd role get-role-holders android.app.role.HOME | tr -d '\r')"
echo "Si algo falla: tools/restore.sh"
