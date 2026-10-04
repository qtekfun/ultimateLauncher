#!/usr/bin/env python3
"""T5.2/T5.3: aplica un perfil de animación (assets/animations/<id>.json) como superposición de recursos enteros.

Genera animprofile/res/values/ul_animation_profile.xml, que las variantes default y sync superponen a res/values/config.xml
de Launcher3 (sin editar archivos de AOSP). Reglas:
  - base = aosp-por-defecto; el perfil elegido (y su `extends`) sustituye solo eventos con durationMs no nulo y no 'approximate'
    (las duraciones medidas solo por vídeo/ráfagas no se aplican a ciegas);
  - duración final = round(durationMs * speedMultiplier).
Uso: tools/apply-anim-profile.py [id_perfil]    (por defecto aosp-por-defecto)
Alcance: solo eventos que Launcher3 define como recursos (ver docs/anim-inventory.md, sección A).
"""
import json, pathlib, sys
root = pathlib.Path(__file__).resolve().parent.parent
adir = root / "assets/animations"
pid = sys.argv[1] if len(sys.argv) > 1 else "aosp-por-defecto"
RES = {"home.pageSnap": ["config_pageSnapAnimationDuration"], "drawer.open": ["config_allAppsOpenDuration"],
       "drawer.close": ["config_allAppsCloseDuration"], "folder.open": ["config_materialFolderExpandDuration"]}
def load(i): return json.load(open(adir / f"{i}.json"))
def chain(i):
    p = load(i); return (chain(p["extends"]) if p.get("extends") else []) + [p]
profiles = [load("aosp-por-defecto")] + [p for p in chain(pid) if p["id"] != "aosp-por-defecto"]
mult = profiles[-1].get("speedMultiplier", 1.0)
dur = {}
for p in profiles:
    for k, e in p.get("events", {}).items():
        if e.get("durationMs") is not None and not e.get("approximate"):
            dur[k] = e["durationMs"]
lines = ['<?xml version="1.0" encoding="utf-8"?>', f"<!-- GENERADO por tools/apply-anim-profile.py: perfil {pid}, multiplicador {mult} -->", "<resources>"]
for k, names in RES.items():
    if k in dur:
        for n in names:
            lines.append(f'    <integer name="{n}">{max(1, round(dur[k] * mult))}</integer>')
lines.append("</resources>")
out = root / "animprofile/res/values/ul_animation_profile.xml"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("\n".join(lines) + "\n")
print(pid, "x", mult, {k: round(v * mult) for k, v in dur.items()})
