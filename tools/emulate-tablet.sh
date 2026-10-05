#!/usr/bin/env bash
# Hace que un MÓVIL se comporte como tablet para diseñar (densidad 280 dpi => ≈1810×823 dp, horizontal => clase tablet-landscape).
# Es REVERSIBLE: guarda los valores originales (densidad, tamaño, rotación) y `off` los restaura exactamente.
# Uso: tools/emulate-tablet.sh on|off|status [serie]
set -euo pipefail
cd "$(dirname "$0")/.."
CMD="${1:?uso: on|off|status [serie]}"
S="${2:-${ANDROID_SERIAL:-<ADB_SERIE_OPPO>}}"
A="adb -s $S"
STATE="private-measurements/emulate-tablet.state"
mkdir -p private-measurements
if ! $A get-state >/dev/null 2>&1; then echo "Dispositivo '$S' no conectado" >&2; exit 1; fi
dens_override() { $A shell wm density | tr -d '\r' | sed -n 's/^Override density: //p'; }
size_override()  { $A shell wm size    | tr -d '\r' | sed -n 's/^Override size: //p'; }
case "$CMD" in
  on)
    if [ -f "$STATE" ]; then echo "Ya activo (estado en $STATE)"; exit 0; fi
    {
      echo "density=$(dens_override)"
      echo "size=$(size_override)"
      echo "accel=$($A shell settings get system accelerometer_rotation | tr -d '\r')"
      echo "rot=$($A shell settings get system user_rotation | tr -d '\r')"
    } > "$STATE"
    cat "$STATE"
    $A shell wm density 280
    $A shell settings put system accelerometer_rotation 0
    $A shell settings put system user_rotation 1
    echo "Modo tablet activado en $S (restaurar: tools/emulate-tablet.sh off)";;
  off)
    [ -f "$STATE" ] || { echo "No hay estado guardado: nada que restaurar"; exit 0; }
    # shellcheck disable=SC1090
    . "$STATE"
    if [ -n "${density:-}" ]; then $A shell wm density "$density"; else $A shell wm density reset; fi
    if [ -n "${size:-}" ]; then $A shell wm size "$size"; fi
    $A shell settings put system accelerometer_rotation "${accel:-1}"
    $A shell settings put system user_rotation "${rot:-0}"
    rm -f "$STATE"; echo "Restaurado";;
  status)
    echo "densidad: $($A shell wm density | tr '\n' ' ')"; echo "tamaño: $($A shell wm size | tr '\n' ' ')"
    [ -f "$STATE" ] && { echo "estado guardado:"; cat "$STATE"; } || echo "(modo tablet inactivo)";;
  *) echo "uso: on|off|status [serie]" >&2; exit 2;;
esac
