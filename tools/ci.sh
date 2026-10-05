#!/usr/bin/env bash
# Integración continua genérica (sirve igual en local, GitHub Actions o cualquier otro CI). Sin secretos ni red propia:
# compila, pasa las pruebas unitarias y Lint, y ejecuta la auditoría de privacidad (docs/09).
# Uso: tools/ci.sh            Variables: GRADLE_ARGS (por defecto --offline si OFFLINE=1), ANDROID_HOME para apkanalyzer.
set -euo pipefail
cd "$(dirname "$0")/.."
G="./gradlew -q ${GRADLE_ARGS:-}"
[ "${OFFLINE:-0}" = "1" ] && G="$G --offline"

echo "### 1/4 Compilación release (default y sync)"; $G :app:assembleDefaultRelease :app:assembleSyncRelease
echo "### 2/4 Pruebas unitarias";                    $G :app:testDefaultDebugUnitTest
echo "### 3/4 Lint (NewApi es error)";               $G :app:lintDefaultDebug
echo "### 4/4 Auditoría de privacidad";              tools/privacy-audit.sh
echo "CI OK"
