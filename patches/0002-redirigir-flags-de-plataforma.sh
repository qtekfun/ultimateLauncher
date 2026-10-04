#!/usr/bin/env bash
# Parche 0002 (reaplicable): redirige las clases Flags de la plataforma (aconfig) a stubs propios.
# Motivo: esas clases existen (ocultas) en el framework del dispositivo; si el APK las trae con el mismo nombre
# se cargaría la del framework (parent-first) y métodos nuevos darían NoSuchMethodError en versiones anteriores.
# Archivos: todo launcher3-base/src, shared, modules, dagger (solo cambia el nombre de paquete en imports/usos).
set -euo pipefail
cd "$(dirname "$0")/.."
FILES=$(grep -rlE "com\.android\.window\.flags|com\.android\.wm\.shell\.Flags|com\.android\.systemui\.shared\.Flags|com\.android\.providers\.media\.flags|android\.multiuser\.Flags|android\.security\.Flags|android\.appwidget\.flags\.Flags" launcher3-base/src launcher3-base/shared launcher3-base/modules launcher3-base/dagger --include=*.java --include=*.kt || true)
[ -z "$FILES" ] && exit 0
sed -i \
 -e 's/com\.android\.window\.flags\.Flags/com.qtekfun.stubs.window.flags.Flags/g' \
 -e 's/com\.android\.wm\.shell\.Flags/com.qtekfun.stubs.wmshell.Flags/g' \
 -e 's/com\.android\.systemui\.shared\.Flags/com.qtekfun.stubs.sysuishared.Flags/g' \
 -e 's/com\.android\.providers\.media\.flags\.Flags/com.qtekfun.stubs.media.flags.Flags/g' \
 -e 's/android\.multiuser\.Flags/com.qtekfun.stubs.multiuser.Flags/g' \
 -e 's/android\.security\.Flags/com.qtekfun.stubs.security.Flags/g' \
 -e 's/android\.appwidget\.flags\.Flags/com.qtekfun.stubs.appwidget.flags.Flags/g' $FILES
