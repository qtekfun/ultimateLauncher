#!/usr/bin/env bash
# Auditoría de privacidad sobre un APK (docs/09, «Red» y «Permisos»).
# Uso: tools/check-permissions.sh [apk] [default|sync]
#   Por defecto: app/build/outputs/apk/default/release/app-default-release.apk (variante deducida del nombre).
# Comprueba, y sale con 1 si falla alguna:
#   1. Variante default: sin INTERNET ni ningún permiso de red (tools/permissions-forbidden.txt).
#   2. Los permisos del manifiesto fusionado coinciden EXACTAMENTE con tools/permissions-allowed.txt (default) o con
#      esa lista + INTERNET (sync). Un permiso nuevo o ausente rompe la comprobación.
#   3. Cada permiso permitido figura nombrado en la tabla de docs/09-privacidad.md (la tabla y la lista no se separan).
#   4. Manifiesto: sin cleartext, sin allowBackup, sin servicios de Google ni servicios exportados de red.
#   5. Variante default: el código del APK no define ni referencia clases de cliente HTTP (HttpURLConnection, okhttp…).
set -euo pipefail
cd "$(dirname "$0")/.."

APK="${1:-app/build/outputs/apk/default/release/app-default-release.apk}"
VARIANT="${2:-}"
if [ -z "$VARIANT" ]; then case "$APK" in *sync*) VARIANT=sync ;; *) VARIANT=default ;; esac; fi
AA="${APKANALYZER:-}"
[ -z "$AA" ] && AA="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}/cmdline-tools/latest/bin/apkanalyzer"
[ -x "$AA" ] || AA="$(command -v apkanalyzer || true)"
[ -x "$AA" ] || { echo "ERROR: no se encuentra apkanalyzer (cmdline-tools del SDK)" >&2; exit 2; }
[ -f "$APK" ] || { echo "ERROR: no existe $APK" >&2; exit 2; }

fail=0
err() { echo "FALLO: $*" >&2; fail=1; }
strip() { grep -v '^[[:space:]]*#' "$1" | sed 's/[[:space:]]*#.*//' | grep -v '^[[:space:]]*$' || true; }

echo "== Auditoría de $APK (variante $VARIANT)"

# --- 1 y 2: permisos --------------------------------------------------------
# El permiso propio de androidx (DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION) lleva el applicationId delante: se normaliza.
actual="$("$AA" manifest permissions "$APK" | sed -E 's/^[A-Za-z0-9_.]+\.(DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION)$/<applicationId>.\1/' | sort -u)"
expected="$(strip tools/permissions-allowed.txt)"
[ "$VARIANT" = sync ] && expected="$(printf '%s\nandroid.permission.INTERNET\n%s\n' "$expected" "$(strip tools/permissions-allowed-sync.txt)")"
expected="$(printf '%s\n' "$expected" | sort -u)"
echo "Permisos declarados:"; printf '  %s\n' $actual

if [ "$VARIANT" = default ]; then
  while read -r p; do
    if printf '%s\n' "$actual" | grep -qx "$p"; then err "la variante default declara $p"; fi
  done < <(strip tools/permissions-forbidden.txt)
fi
extra="$(comm -13 <(printf '%s\n' "$expected") <(printf '%s\n' "$actual"))"
missing="$(comm -23 <(printf '%s\n' "$expected") <(printf '%s\n' "$actual"))"
[ -n "$extra" ] && err "permisos NO permitidos por tools/permissions-allowed.txt: $(echo $extra)"
[ -n "$missing" ] && err "permisos esperados que ya no están (¿actualizar la lista y docs/09?): $(echo $missing)"
if [ "$VARIANT" = sync ] && ! printf '%s\n' "$actual" | grep -qx "android.permission.INTERNET"; then err "la variante sync debería declarar INTERNET"; fi

# --- 3: la tabla de docs/09 nombra cada permiso -------------------------------
while read -r p; do
  short="${p##*.}"
  grep -q "$short" docs/09-privacidad.md || err "el permiso $short no figura en la tabla de docs/09-privacidad.md"
done < <(strip tools/permissions-allowed.txt; strip tools/permissions-allowed-sync.txt)

# --- 4: manifiesto fusionado ----------------------------------------------------
MF="$("$AA" manifest print "$APK")"
if [ "$VARIANT" = default ] && echo "$MF" | grep -q 'usesCleartextTraffic="true"'; then err "usesCleartextTraffic=true"; fi
echo "$MF" | grep -q 'allowBackup="true"' && err "allowBackup=true (docs/09: sin copia en la nube del sistema)"
echo "$MF" | grep -qiE 'com\.google\.android\.gms|com\.google\.firebase|com\.google\.android\.play' && err "el manifiesto referencia servicios de Google"
echo "$MF" | grep -q 'QUERY_ALL_PACKAGES' && err "QUERY_ALL_PACKAGES declarado"

# --- 5: clases de red en el código (solo default) --------------------------------
if [ "$VARIANT" = default ]; then
  DEX="$("$AA" dex packages "$APK")"
  hits="$(echo "$DEX" | grep -E '^C [dr] ' | grep -E "$(strip tools/forbidden-classes.txt | paste -sd'|')" || true)"
  if [ -n "$hits" ]; then err "el código referencia clientes de red:"; echo "$hits" | head -20 >&2; fi
  echo "Referencias java.net.* (informativo; sin INTERNET no pueden abrir conexiones):"
  echo "$DEX" | grep -E '^C r .*java\.net\.(Socket|URL)$' | awk '{print "  " $NF}' | sort -u || true
fi

if [ "$fail" -eq 0 ]; then echo "OK: auditoría de $VARIANT superada"; else echo "ERROR: auditoría de $VARIANT fallida" >&2; fi
exit "$fail"
