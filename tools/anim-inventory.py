#!/usr/bin/env python3
"""T5.1: inventario de constantes de animación de Launcher3 (genera docs/anim-inventory.md)."""
import pathlib, re
root = pathlib.Path(__file__).resolve().parent.parent
base = root / "launcher3-base"
out = ["# Inventario de constantes de animación (Launcher3 android17-release)", "",
       "Generado por `tools/anim-inventory.py`. «Redirigible» = se puede cambiar sin tocar código (recurso entero superpuesto por `tools/apply-anim-profile.py`).", "",
       "## A. Recursos enteros (`res/values/config.xml`) — redirigibles", "", "| Recurso | Valor AOSP | Evento del perfil |", "|---|---|---|"]
ev = {"config_allAppsOpenDuration": "drawer.open", "config_allAppsCloseDuration": "drawer.close",
      "config_materialFolderExpandDuration": "folder.open / folder.close", "config_pageSnapAnimationDuration": "home.pageSnap",
      "config_bottomSheetOpenDuration": "(hoja inferior: widgets/popups)", "config_bottomSheetCloseDuration": "(hoja inferior)",
      "config_dropAnimMinDuration": "icon.drop (mín)", "config_dropAnimMaxDuration": "icon.drop (máx)",
      "config_caretAnimationDuration": "(caret del cajón)", "config_folderDelay": "(retardo carpeta)",
      "config_keyboardTaskFocusSnapAnimationDuration": "(teclado, tareas)"}
for m in re.finditer(r'<integer name="(config_\w*(?:Duration|Delay)\w*)">(\d+)</integer>', (base / "res/values/config.xml").read_text()):
    out.append(f"| `{m.group(1)}` | {m.group(2)} ms | {ev.get(m.group(1), '—')} |")
out += ["", "## B. Constantes en código — NO redirigidas (requieren parche en AOSP)", "", "| Archivo | Constante | Valor |", "|---|---|---|"]
pat = re.compile(r'(?:static final|const val|val)\s+(?:\w+\s+)?(\w*(?:DURATION|ANIM)\w*)\s*(?::\s*\w+)?\s*=\s*(\d+[fFLl]?)')
rows = []
for f in sorted(base.joinpath("src").rglob("*")):
    if f.suffix in (".java", ".kt"):
        for m in pat.finditer(f.read_text()):
            rows.append((str(f.relative_to(base / "src")), m.group(1), m.group(2)))
out += [f"| `{a}` | `{b}` | {c} |" for a, b, c in rows]
out += ["", "## C. Interpoladores y muelles", "",
        "Viven repartidos en `com.android.launcher3.anim.Interpolators`, `PendingAnimation`, `SpringAnimationBuilder`, `StateAnimationConfig` y `LauncherAnimUtils`. "
        "No se han redirigido: requieren parches individuales (pendiente de M5 T5.2 completo)."]
(root / "docs/anim-inventory.md").write_text("\n".join(out) + "\n")
print(len(rows), "constantes en código")
