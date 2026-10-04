#!/usr/bin/env python3
"""Parche 0006 (reaplicable): LoaderTask.sendFirstScreenActiveInstallsBroadcast() pasa a no hacer nada.

Motivo: (1) privacidad (docs/09): difunde por broadcast el contenido de la primera pantalla (apps instaladas) a
otra app (el instalador de la plataforma/Google); (2) fuera de AOSP lanza NullPointerException
(userKeyToSessionMap nulo) y mata el hilo del cargador al arrancar.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/model/LoaderTask.java"
t = p.read_text()
old = "    private void sendFirstScreenActiveInstallsBroadcast() {\n"
new = old + "        // UltimateLauncher 0006: desactivado (privacidad y NPE fuera de AOSP).\n        if (true) return;\n"
if new not in t:
    assert old in t
    p.write_text(t.replace(old, new))
