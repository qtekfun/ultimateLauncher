#!/usr/bin/env python3
"""T5.5 (parcial): compara un perfil de animación con las mediciones y dice qué eventos faltan.

Para cada evento del alcance (docs/04, sección B) informa de:
  - medición: mediana de ráfagas (private-measurements/burst-summary.json si existe, si no `rawBurstsMs` del propio perfil);
  - perfil: durationMs / curva (bézier o muelle) o null;
  - coherencia: |perfil - medición| <= max(precisión de 2 fotogramas, 5 %) -> OK; si no, DESVÍO;
  - gancho: cómo se aplica (assets/animations/hooks.json) y si el XML generado
    (animprofile/res/values/ul_animation_profile.xml) coincide con lo que el perfil debería producir.
Salida 0 si no hay DESVÍO ni XML desactualizado; 1 si los hay; con --estricto también 1 si falta algo del alcance.
Uso: tools/validate-anim.py [id_perfil] [--medidas ruta/burst-summary.json] [--estricto]
Solo lectura: no escribe nada.
"""
import json, pathlib, re, subprocess, sys

root = pathlib.Path(__file__).resolve().parent.parent
adir = root / "assets/animations"
argv = sys.argv[1:]
estricto = "--estricto" in argv
medidas = None
if "--medidas" in argv:
    medidas = pathlib.Path(argv[argv.index("--medidas") + 1])
pos = [a for i, a in enumerate(argv) if not a.startswith("--") and (i == 0 or argv[i - 1] != "--medidas")]
pid = pos[0] if pos else "oppo-medido"

# Alcance pedido para M5 (docs/04) y resto de eventos de la plantilla
ALCANCE = ["folder.open", "folder.close", "drawer.open", "drawer.close", "app.launch"]
TODOS = ALCANCE + ["app.returnHome", "home.pageScroll", "home.pageSnap", "icon.pressScale", "icon.drag.pickup", "widget.resize"]

def load(i): return json.load(open(adir / f"{i}.json"))
def chain(i):
    p = load(i); return (chain(p["extends"]) if p.get("extends") else []) + [p]

prof = load(pid)
hooks = json.load(open(adir / "hooks.json"))

def find_medidas():
    if medidas:
        return medidas
    cands = [root / "private-measurements/burst-summary.json"]
    try:  # worktree: las mediciones privadas viven en el repositorio principal
        common = subprocess.run(["git", "-C", str(root), "rev-parse", "--git-common-dir"], capture_output=True, text=True).stdout.strip()
        cands.append((root / common).resolve().parent / "private-measurements/burst-summary.json")
    except Exception:
        pass
    return next((c for c in cands if c.exists()), None)

mpath = find_medidas()
burst = {}
if mpath and mpath.exists():
    burst = json.load(open(mpath))
    origen = str(mpath)
else:
    origen = "rawBurstsMs del perfil (private-measurements/burst-summary.json no disponible)"
for k, v in prof.get("rawBurstsMs", {}).items():
    burst.setdefault(k, v)

precision = prof.get("source", {}).get("precisionMs", 17)

# XML generado y lo que debería contener
xml_path = root / "animprofile/res/values/ul_animation_profile.xml"
xml = xml_path.read_text() if xml_path.exists() else ""
m = re.search(r"perfil (\S+), multiplicador ([0-9.]+)(, incluye duraciones aproximadas)?", xml)
gen_vals = dict(re.findall(r'<integer name="(\w+)">(\d+)</integer>', xml))

def esperado(perfil_id, aprox):
    profiles = [load("aosp-por-defecto")] + [p for p in chain(perfil_id) if p["id"] != "aosp-por-defecto"]
    mult = profiles[-1].get("speedMultiplier", 1.0)
    dur = {}
    for p in profiles:
        for k, e in p.get("events", {}).items():
            if e.get("durationMs") is not None and (aprox or not e.get("approximate")):
                dur[k] = e["durationMs"]
    out = {}
    for k, h in hooks.items():
        if not k.startswith("_") and h.get("modo") == "recurso" and k in dur:
            for n in h["recursos"]:
                out[n] = str(max(1, round(dur[k] * mult)))
    return out

problemas, faltas = [], []
print(f"Perfil: {pid}   mediciones: {origen}   precisión: ±{precision} ms\n")
if m:
    print(f"XML generado: perfil {m.group(1)}, x{m.group(2)}{', con aproximadas' if m.group(3) else ''}")
    exp = esperado(m.group(1), bool(m.group(3)))
    if exp != gen_vals:
        problemas.append(f"el XML generado no coincide con el perfil {m.group(1)} (esperado {exp}, hay {gen_vals}): ejecutar tools/apply-anim-profile.py")
else:
    print("XML generado: ilegible o ausente")
    problemas.append("no hay XML generado: ejecutar tools/apply-anim-profile.py")
print()

filas = []
for k in TODOS:
    e = prof.get("events", {}).get(k)
    h = hooks.get(k, {"modo": "ninguno", "motivo": "sin entrada en hooks.json"})
    med = burst.get(k, {}).get("medianaMs")
    d = e.get("durationMs") if e else None
    curva = None
    if e:
        curva = "bezier" if e.get("bezier") and all(x is not None for x in e["bezier"]) else None
        if e.get("type") == "spring" and e.get("dampingRatio") is not None and e.get("stiffness") is not None:
            curva = "muelle"
    estado = []
    if med is None:
        estado.append("sin medición")
    if d is None:
        estado.append("sin duración en el perfil")
    if med is not None and d is not None:
        tol = max(precision, 0.05 * med)
        if abs(d - med) <= tol:
            estado.append(f"coherente (Δ{d - med:+.1f} ms)")
        else:
            estado.append(f"DESVÍO (Δ{d - med:+.1f} ms > {tol:.1f})")
            problemas.append(f"{k}: perfil {d} ms vs medición {med} ms")
    if curva is None:
        estado.append("sin curva (null)")
    if h["modo"] == "ninguno":
        estado.append("sin gancho")
    elif h["modo"] == "compartido":
        estado.append(f"comparte recurso con {h['con']}")
    else:
        aplicado = [gen_vals.get(n) for n in h["recursos"]]
        estado.append("gancho en recurso " + "/".join(h["recursos"]) + f" (XML={'/'.join(map(str, aplicado))})")
    filas.append((k, med, d, "aprox." if e and e.get("approximate") else "", estado))
    if k in ALCANCE:
        falta = []
        if med is None: falta.append("medición")
        if d is None: falta.append("duración")
        if curva is None: falta.append("curva")
        if h["modo"] == "ninguno": falta.append("gancho")
        if falta:
            faltas.append(f"{k} [{', '.join(falta)}]")

w = max(len(f[0]) for f in filas)
for k, med, d, ap, estado in filas:
    alc = "*" if k in ALCANCE else " "
    print(f"{alc} {k:<{w}}  medición={med if med is not None else 'null':<7} perfil={d if d is not None else 'null':<5} {ap:<6} -> " + "; ".join(estado))
print("\n(* = alcance de M5: abrir/cerrar carpeta, cajón, abrir app)")
print("\nFaltan por completar en el alcance:", ", ".join(faltas) if faltas else "nada")
if problemas:
    print("\nPROBLEMAS:")
    for p in problemas:
        print(" -", p)
sys.exit(1 if problemas or (estricto and faltas) else 0)
