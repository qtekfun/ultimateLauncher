#!/usr/bin/env python3
"""Estima la duración (aproximada) de los eventos del launcher a partir de una traza Perfetto.

Método: se agrupan en ráfagas los fotogramas presentados por la ventana del launcher
(actual_frame_timeline_slice) separados por menos de 50 ms. Cada ráfaga = un evento.
La duración es último-primer fotograma de la ráfaga: incluye el arrastre del dedo en los
gestos y 1-2 fotogramas de latencia, por eso es APROXIMADA. No da la forma de la curva.
Uso: analyze_bursts.py traza.pftrace [paquete_launcher] [salida.json]
Secuencia esperada por repetición (ver capture.sh): 6 eventos en el orden de EVENTS.
"""
import json, statistics, sys
from perfetto.trace_processor import TraceProcessor

EVENTS = ["drawer.open", "drawer.close", "folder.open", "folder.close", "app.launch", "app.returnHome"]
trace = sys.argv[1]
pkg = sys.argv[2] if len(sys.argv) > 2 else "com.android.launcher"
out = sys.argv[3] if len(sys.argv) > 3 else None
tp = TraceProcessor(file_path=trace)
rows = list(tp.query(f"select ts, jank_type from actual_frame_timeline_slice where layer_name like '%{pkg}/%' order by ts"))
bursts, cur = [], [rows[0]]
for r in rows[1:]:
    if r.ts - cur[-1].ts > 50e6:
        bursts.append(cur); cur = [r]
    else:
        cur.append(r)
bursts.append(cur)
bursts = [b for b in bursts if len(b) >= 10]          # ruido de pocos fotogramas
reps = len(bursts) // len(EVENTS)
res = {e: [] for e in EVENTS}
for i, b in enumerate(bursts[: reps * len(EVENTS)]):
    e = EVENTS[i % len(EVENTS)]
    dur = (b[-1].ts - b[0].ts) / 1e6
    janky = sum(1 for f in b if f.jank_type not in ("None", None)) / len(b)
    res[e].append({"durationMs": round(dur, 1), "jankyFrac": round(janky, 3)})
summary = {}
for e, v in res.items():
    good = [x["durationMs"] for x in v if x["jankyFrac"] <= 0.25]   # descarta repeticiones con fotogramas perdidos
    summary[e] = {"n": len(v), "usadas": len(good), "medianaMs": statistics.median(good) if good else None,
                  "todas": [x["durationMs"] for x in v]}
print(json.dumps(summary, indent=1))
if out:
    json.dump(summary, open(out, "w"), indent=1)
