#!/usr/bin/env bash
# Auditoría de privacidad automatizada (docs/09 «Auditoría antes de cada versión», parte comprobable sin dispositivo).
# Uso: tools/privacy-audit.sh [--build]     (--build compila antes los APK release de default y sync)
# Ejecuta: permisos y código de red del APK default y del sync, dependencias y exportación sin credenciales.
# Lo que NO cubre (manual, ver docs/privacy-audit.md): prueba de 24 h sin conexiones, dispositivo sin GMS, escáner de trackers.
set -uo pipefail
cd "$(dirname "$0")/.."
[ "${1:-}" = "--build" ] && { ./gradlew --offline -q :app:assembleDefaultRelease && ./gradlew --offline -q :app:assembleSyncRelease || exit 2; }
fail=0
step() { echo; echo "### $*"; }

step "APK default"
tools/check-permissions.sh app/build/outputs/apk/default/release/app-default-release.apk default || fail=1
step "APK sync"
tools/check-permissions.sh app/build/outputs/apk/sync/release/app-sync-release.apk sync || fail=1
step "Dependencias"
tools/check-dependencies.sh || fail=1

step "La exportación de layout no contiene credenciales ni URL de servidores"
if grep -rniE 'password|passw|webdav|https?://|token' app/src/main/java/com/qtekfun/ultimatelauncher/layoutsync/LayoutModel.kt app/src/main/java/com/qtekfun/ultimatelauncher/layoutsync/LayoutStore.kt; then
  echo "FALLO: el serializador del layout menciona credenciales o URL" >&2; fail=1
else echo "OK: LayoutModel.kt y LayoutStore.kt no mencionan credenciales ni URL"; fi

step "Resultado"
if [ $fail -eq 0 ]; then echo "AUDITORÍA SUPERADA"; else echo "AUDITORÍA FALLIDA" >&2; fi
exit $fail
