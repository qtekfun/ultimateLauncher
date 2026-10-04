#!/usr/bin/env python3
"""Parche 0007 (reaplicable): compuerta de versión para AppWidgetHostView.start/stopVisibilityTracking (API de Android 17).

En Android 16 (API 36) provoca NoSuchMethodError al añadir un widget (R1 de docs/08, hallado en M3).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/widget/WidgetVisibilityTracker.kt"
t = p.read_text()
old = """            if (inNormalState && noFloatingViews && pageIndex in visiblePages) {
                view.startVisibilityTracking()
            } else {
                view.stopVisibilityTracking()
            }"""
new = """            // UltimateLauncher 0007: API de Android 17 (SDK 37); en versiones anteriores no existe.
            if (android.os.Build.VERSION.SDK_INT < 37) return@forEach
            try {
                if (inNormalState && noFloatingViews && pageIndex in visiblePages) {
                    view.startVisibilityTracking()
                } else {
                    view.stopVisibilityTracking()
                }
            } catch (e: NoSuchMethodError) {
                // El framework del dispositivo no trae la API (SDK 37 sin el flag activado).
            }"""
if new not in t:
    assert old in t
    p.write_text(t.replace(old, new))
