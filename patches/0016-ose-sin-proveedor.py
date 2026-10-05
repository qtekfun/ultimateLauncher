#!/usr/bin/env python3
"""Parche 0016 (reaplicable): OseWidgetView no pinta el widget de búsqueda si no hay proveedor (sin GMS).

En la tablet Huawei (Android 12, sin GMS) updateAppWidget() se llamaba con AppWidgetProviderInfo nulo -> NullPointerException
en AppWidgetHostView.getRemoteContext y el launcher se cerraba al arrancar.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/qsb/OseWidgetView.kt"
t = p.read_text()
old = "            oseWidgetManager.views.forEach(activityContext.uiExecutor) {\n                updateAppWidget(it)\n"
new = "            oseWidgetManager.views.forEach(activityContext.uiExecutor) {\n                if (appWidgetInfo == null) return@forEach // UltimateLauncher 0016: sin proveedor (sin GMS)\n                updateAppWidget(it)\n"
if new not in t:
    assert old in t
    p.write_text(t.replace(old, new))

old2 = "            oseWidgetManager.providerInfo.forEach(activityContext.uiExecutor) {\n                setAppWidget(INVALID_APPWIDGET_ID, it)\n"
new2 = "            oseWidgetManager.providerInfo.forEach(activityContext.uiExecutor) {\n                if (it == null) return@forEach // UltimateLauncher 0016\n                setAppWidget(INVALID_APPWIDGET_ID, it)\n"
t2 = p.read_text()
if new2 not in t2:
    assert old2 in t2
    p.write_text(t2.replace(old2, new2))
