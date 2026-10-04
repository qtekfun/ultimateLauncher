#!/usr/bin/env python3
"""Parche 0010 (reaplicable): Launcher.onCreate llama a FirstRun.maybeShow(this) (asistente de M6, una vez)."""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/Launcher.java"
t = p.read_text()
old = "        super.onCreate(savedInstanceState);\n"
new = old + "        com.qtekfun.ultimatelauncher.oem.FirstRun.maybeShow(this); // UltimateLauncher 0010\n"
if "FirstRun.maybeShow" not in t:
    assert t.count(old) >= 1
    p.write_text(t.replace(old, new, 1))
