#!/usr/bin/env bash
# Auditoría de dependencias resueltas (docs/09 «Sin GMS» y «Dependencias»; lista en docs/dependencies.md).
# Uso: tools/check-dependencies.sh [configuración]    (por defecto defaultReleaseRuntimeClasspath, la que se empaqueta)
#   TXT=archivo  usa un volcado ya hecho de `gradlew :app:dependencies` (sin ejecutar Gradle).
# Falla si aparece (1) un artefacto prohibido (Google Play/Firebase/ML Kit/clientes de red; tools/forbidden-deps.txt) o
# (2) un grupo que no está en tools/allowed-dependency-groups.txt (hay que revisarlo y anotarlo en docs/dependencies.md).
set -euo pipefail
cd "$(dirname "$0")/.."
CONF="${1:-defaultReleaseRuntimeClasspath}"
strip() { grep -v '^[[:space:]]*#' "$1" | sed 's/[[:space:]]*#.*//' | grep -v '^[[:space:]]*$' || true; }
TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
if [ -n "${TXT:-}" ]; then cp "$TXT" "$TMP"; else ./gradlew --offline -q :app:dependencies --configuration "$CONF" > "$TMP" 2>/dev/null; fi
grep -q 'Project' "$TMP" || { echo "ERROR: no se obtuvo el árbol de dependencias" >&2; exit 2; }

coords="$(grep -oE '[-+\\]--- [A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+' "$TMP" | sed 's/.*--- //' | sort -u)"
echo "== Dependencias resueltas ($CONF): $(echo "$coords" | wc -l) artefactos, $(echo "$coords" | cut -d: -f1 | sort -u | wc -l) grupos"
fail=0
bad="$(echo "$coords" | grep -E "$(strip tools/forbidden-deps.txt | paste -sd'|')" || true)"
if [ -n "$bad" ]; then echo "FALLO: dependencias prohibidas:" >&2; echo "$bad" >&2; fail=1; fi
unknown=""
while read -r g; do
  ok=0
  while read -r a; do
    case "$a" in
      *'*') [[ "$g" == ${a%\*}* ]] && ok=1 ;;
      *) [ "$g" = "$a" ] && ok=1 ;;
    esac
  done < <(strip tools/allowed-dependency-groups.txt)
  [ $ok -eq 1 ] || unknown="$unknown $g"
done < <(echo "$coords" | cut -d: -f1 | sort -u)
if [ -n "$unknown" ]; then
  echo "FALLO: grupos no revisados (añádelos a tools/allowed-dependency-groups.txt y docs/dependencies.md tras revisar licencia y red):$unknown" >&2
  fail=1
fi
[ $fail -eq 0 ] && echo "OK: sin dependencias prohibidas ni grupos sin revisar"
exit $fail
