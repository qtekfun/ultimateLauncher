#!/usr/bin/env python3
"""Parche 0044 (reaplicable, idempotente por gancho): diagnóstico de ciclo de vida/iconos (solo debug, etiqueta ULDIAG).
Para localizar «iconos que desaparecen unos segundos al volver con el gesto de inicio»."""
import pathlib, re
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/Launcher.java"
t = p.read_text()
def hook(sig, call, t):
    if call in t: return t
    m = re.search(r"(    (?:public|protected) void " + re.escape(sig) + r"\{\n)", t)
    assert m, sig
    return t.replace(m.group(1), m.group(1) + "        " + call + " // UltimateLauncher 0044\n", 1)
D = "com.qtekfun.ultimatelauncher.diag.UlDiag"
t = hook("onStart() ", f'{D}.event("onStart");', t)
t = hook("onStop() ", f'{D}.event("onStop");', t)
t = hook("onPause() ", f'{D}.event("onPause");', t)
t = hook("onResume() ", f'{D}.event("onResume"); {D}.snapshotAfterResume(this);', t)
t = hook("onNewIntent(Intent intent) ", f'{D}.event("onNewIntent");', t)
t = hook("onTrimMemory(int level) ", f'{D}.event("onTrimMemory=" + level);', t)
t = hook("onWindowFocusChanged(boolean hasFocus) ", f'{D}.event("focus=" + hasFocus);', t)
p.write_text(t)
