#!/usr/bin/env bash
# Prueba de humo en emulador (AVD ul31 = Android 12 / ul34 = Android 14, imágenes AOSP «default», sin GMS).
# Hace todo en una sola ejecución: arranca, instala, abre el launcher, abre el cajón, busca, abre el selector de widgets,
# guarda capturas y logcat en private-measurements/emu-<avd>/ y cierra el emulador.
# Uso: tools/emu-test.sh ul31 [apk]
set -uo pipefail
cd "$(dirname "$0")/.."
AVD="${1:-ul31}"; APK="${2:-dist/ultimatelauncher-default-debug.apk}"
PORT=5570; S="emulator-$PORT"; OUT="private-measurements/emu-$AVD"; mkdir -p "$OUT"
~/Android/Sdk/emulator/emulator -avd "$AVD" -no-window -no-audio -no-snapshot -gpu swiftshader_indirect -port $PORT -verbose 2>&1 | cat > "$OUT/emulator.log" &
EMU=$!
trap 'adb -s $S emu kill >/dev/null 2>&1; kill $EMU 2>/dev/null' EXIT
for i in $(seq 1 100); do [ "$(adb -s $S shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break; sleep 4; done
echo "SDK=$(adb -s $S shell getprop ro.build.version.sdk | tr -d '\r') GMS_paquetes=$(adb -s $S shell pm list packages | grep -c gms)"
adb -s $S shell settings put global window_animation_scale 0 >/dev/null; sleep 2
adb -s $S install -r "$APK" 2>&1 | tail -1
adb -s $S shell cmd role add-role-holder android.app.role.HOME com.qtekfun.ultimatelauncher >/dev/null 2>&1
adb -s $S logcat -c
adb -s $S shell am start -n com.qtekfun.ultimatelauncher/com.android.launcher3.Launcher >/dev/null; sleep 12
echo "foco1: $(adb -s $S shell dumpsys window | grep mCurrentFocus)"
adb -s $S exec-out screencap -p > "$OUT/01-first.png"
# el asistente de primer arranque se muestra encima: cerrarlo con «Hecho» si está
adb -s $S shell input keyevent KEYCODE_BACK; sleep 2; adb -s $S shell input keyevent KEYCODE_HOME; sleep 3
echo "foco2: $(adb -s $S shell dumpsys window | grep mCurrentFocus)"
adb -s $S exec-out screencap -p > "$OUT/02-home.png"
adb -s $S shell input swipe 540 2000 540 600 250; sleep 3
adb -s $S exec-out screencap -p > "$OUT/03-drawer.png"
adb -s $S shell input keyevent KEYCODE_HOME; sleep 2
adb -s $S shell input swipe 540 1200 540 1200 1000; sleep 2
adb -s $S exec-out screencap -p > "$OUT/04-longpress.png"
adb -s $S logcat -d > "$OUT/logcat.txt"
echo "FATAL: $(grep -c 'FATAL EXCEPTION' "$OUT/logcat.txt")"; grep -E "FATAL EXCEPTION" -A8 "$OUT/logcat.txt" | head -30
