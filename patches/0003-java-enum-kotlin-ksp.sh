#!/usr/bin/env bash
# Parche 0003 (reaplicable): KSP no resuelve en Java el import estático de un enum Kotlin
# (LightweightBackgroundPriority.UI) y Dagger falla con NonExistentClass.
# Se sustituye `priority = UI` por el nombre calificado. Archivos: SettingsCache.java, ScreenOnTracker.java
set -euo pipefail
cd "$(dirname "$0")/../launcher3-base/src/com/android/launcher3/util"
for f in SettingsCache.java ScreenOnTracker.java; do
  sed -i -e '/^import static com.android.launcher3.concurrent.annotations.LightweightBackgroundPriority.UI;/d' \
         -e 's/priority = UI)/priority = com.android.launcher3.concurrent.annotations.LightweightBackgroundPriority.UI)/' "$f"
done
