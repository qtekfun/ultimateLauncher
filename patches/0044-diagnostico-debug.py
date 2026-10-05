#!/usr/bin/env python3
"""Parche 0044 (reaplicable): diagnóstico de ciclo de vida/iconos (solo debug, etiqueta ULDIAG). Para localizar «iconos que
desaparecen unos segundos al volver con el gesto de inicio»."""
import pathlib, re
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/Launcher.java"
t = p.read_text()
if "UlDiag" in t:
    raise SystemExit(0)
def hook(sig, call, t):
    m = re.search(r"(    (?:public|protected) void " + re.escape(sig) + r"\{\n)", t)
    assert m, sig
    return t.replace(m.group(1), m.group(1) + "        " + call + " // UltimateLauncher 0044\n", 1)
t = hook("onStart() ", 'com.qtekfun.ultimatelauncher.diag.UlDiag.event("onStart");', t)
t = hook("onStop() ", 'com.qtekfun.ultimatelauncher.diag.UlDiag.event("onStop");', t)
t = hook("onPause() ", 'com.qtekfun.ultimatelauncher.diag.UlDiag.event("onPause");', t)
t = hook("onResume() ", 'com.qtekfun.ultimatelauncher.diag.UlDiag.event("onResume"); com.qtekfun.ultimatelauncher.diag.UlDiag.snapshotAfterResume(this);', t)
t = hook("onNewIntent(Intent intent) ", 'com.qtekfun.ultimatelauncher.diag.UlDiag.event("onNewIntent");', t)
t = hook("onTrimMemory(int level) ", 'com.qtekfun.ultimatelauncher.diag.UlDiag.event("onTrimMemory=" + level);', t)
p.write_text(t)
