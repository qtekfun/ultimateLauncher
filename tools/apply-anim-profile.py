#!/usr/bin/env python3
"""T5.2/T5.3: aplica un perfil de animación (assets/animations/<id>.json) como superposición de recursos enteros.

Genera animprofile/res/values/ul_animation_profile.xml, que las variantes default y sync superponen a res/values/config.xml
de Launcher3 (sin editar archivos de AOSP). Reglas:
  - base = aosp-por-defecto; el perfil elegido (y su `extends`) sustituye solo eventos con durationMs no nulo y no 'approximate'
    (las duraciones medidas solo por ráfagas de fotogramas no se aplican a ciegas), salvo con --aproximadas;
  - duración final = round(durationMs * speedMultiplier);
  - qué eventos tienen recurso lo dice assets/animations/hooks.json (modo "recurso"); un evento "compartido" (folder.close) no
    se aplica por separado: si su valor difiere del de su gemelo se avisa.
Uso: tools/apply-anim-profile.py [id_perfil] [--aproximadas]    (por defecto aosp-por-defecto)
Alcance: solo eventos que Launcher3 define como recursos (ver docs/anim-inventory.md, sección A).
"""
import json, pathlib, sys
root = pathlib.Path(__file__).resolve().parent.parent
adir = root / "assets/animations"
args = [a for a in sys.argv[1:] if not a.startswith("--")]
aprox = "--aproximadas" in sys.argv
pid = args[0] if args else "aosp-por-defecto"
HOOKS = json.load(open(adir / "hooks.json"))
def load(i): return json.load(open(adir / f"{i}.json"))
def chain(i):
    p = load(i); return (chain(p["extends"]) if p.get("extends") else []) + [p]
profiles = [load("aosp-por-defecto")] + [p for p in chain(pid) if p["id"] != "aosp-por-defecto"]
mult = profiles[-1].get("speedMultiplier", 1.0)
dur = {}
for p in profiles:
    for k, e in p.get("events", {}).items():
        if e.get("durationMs") is not None and (aprox or not e.get("approximate")):
            dur[k] = e["durationMs"]
lines = ['<?xml version="1.0" encoding="utf-8"?>',
         f"<!-- GENERADO por tools/apply-anim-profile.py: perfil {pid}, multiplicador {mult}"
         + (", incluye duraciones aproximadas" if aprox else "") + " -->", "<resources>"]
applied = {}
for k, h in HOOKS.items():
    if k.startswith("_") or h.get("modo") != "recurso" or k not in dur:
        continue
    v = max(1, round(dur[k] * mult))
    applied[k] = v
    for n in h["recursos"]:
        lines.append(f'    <integer name="{n}">{v}</integer>')
for k, h in HOOKS.items():
    if not k.startswith("_") and h.get("modo") == "compartido" and k in dur and dur.get(h["con"]) != dur[k]:
        print(f"AVISO: {k} ({dur[k]} ms) comparte recurso con {h['con']} ({dur.get(h['con'])} ms): se usa el de {h['con']}", file=sys.stderr)
lines.append("</resources>")
out = root / "animprofile/res/values/ul_animation_profile.xml"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("\n".join(lines) + "\n")
print(pid, "x", mult, ("(con aproximadas) " if aprox else ""), applied)
