#!/usr/bin/env python3
"""Parche 0011 — PREPARADO, NO APLICADO (no se pudo verificar en dispositivo). Reaplicable: getParcelable/getParcelableExtra con clase (API 33) -> BundleCompat/IntentCompat (androidx.core).

Evita NoSuchMethodError en Android 12 (API 31-32). Lint NewApi lo señalaba en WidgetPickerActivity y WidgetPickerComposeWrapperImpl.
NOTA: no probado en API < 33 (no hay emulador utilizable en este host, ver docs/progress.md).
"""
import pathlib
B = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/widgetpicker"
def sub(f, old, new, imp=None):
    p = B / f; t = p.read_text()
    if new in t: return
    assert old in t, (f, old)
    t = t.replace(old, new)
    if imp and imp not in t:
        t = t.replace("\nimport ", f"\n{imp}\nimport ", 1)
    p.write_text(t)
sub("WidgetPickerActivity.kt", "intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java)",
    "androidx.core.content.IntentCompat.getParcelableExtra(intent, Intent.EXTRA_USER, UserHandle::class.java)")
sub("WidgetPickerComposeWrapperImpl.kt", """extras?.getParcelable(
                                AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                                RemoteViews::class.java,
                            )""", """extras?.let {
                                androidx.core.os.BundleCompat.getParcelable(
                                    it,
                                    AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                                    RemoteViews::class.java,
                                )
                            }""")
