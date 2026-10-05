#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
# SPDX-License-Identifier: GPL-3.0-or-later
# Restaura el launcher original de OPPO como predeterminado.
# Uso: tools/restore.sh [serie] [paquete_original]   (o ANDROID_SERIAL / ORIGINAL_LAUNCHER)
set -euo pipefail
S="${1:-${ANDROID_SERIAL:-}}"
if ! adb -s "$S" get-state >/dev/null 2>&1; then
  echo "El dispositivo '$S' no está conectado. Dispositivos:" >&2; adb devices >&2
  echo "Pasa la serie correcta como primer argumento (no se elige ninguna automáticamente para no tocar otro teléfono)." >&2; exit 1
fi
ORIG="${2:-${ORIGINAL_LAUNCHER:-com.android.launcher}}"   # Huawei: com.huawei.android.launcher
adb -s "$S" shell cmd role add-role-holder --user 0 android.app.role.HOME "$ORIG"
adb -s "$S" shell input keyevent KEYCODE_HOME
adb -s "$S" shell cmd role get-role-holders android.app.role.HOME
