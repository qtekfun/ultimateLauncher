#!/usr/bin/env python3
"""Parche 0031 (reaplicable): `LinkedHashSet.addLast/removeLast` (SequencedCollection, API 35) -> `add` / `remove(last())`.

Solo en FirstScreenBroadcastHelper (código inalcanzable desde el parche 0006), pero Lint NewApi lo marcaba como error.
`add` en un LinkedHashSet ya añade al final, y `remove(last())` equivale a `removeLast()`.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/model/FirstScreenBroadcastHelper.kt"
t = p.read_text()
for old, new in [
    ("installedWidgets.addLast(packageName)", "installedWidgets.add(packageName) // 0031: antes addLast (API 35)"),
    ("installedWidgets.isNotEmpty() -> installedWidgets.removeLast()",
     "installedWidgets.isNotEmpty() -> installedWidgets.remove(installedWidgets.last()) // 0031: antes removeLast (API 35)"),
]:
    if new in t: continue
    assert old in t, old
    t = t.replace(old, new)
p.write_text(t)
