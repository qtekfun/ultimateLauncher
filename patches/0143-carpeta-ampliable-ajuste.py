#!/usr/bin/env python3
"""Parche 0143 (reaplicable, idempotente): interruptor «Carpetas ampliables» (pref_ul_folder_expand, activo por defecto).
Sin él, la pulsación larga de una carpeta no abre menú ni ofrece «Ampliar carpeta» (las ya ampliadas siguen pudiendo
reducirse). Archivo: launcher_preferences.xml."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_folder_expand" not in t:
    add = '''    <!-- UltimateLauncher 0143 -->
    <SwitchPreference
        android:key="pref_ul_folder_expand"
        android:title="@string/ul_pref_folder_expand"
        android:summary="@string/ul_pref_folder_expand_summary"
        android:defaultValue="true"
        android:persistent="true" />

'''
    marker = "    <!-- UltimateLauncher 0061 -->"
    assert marker in t
    px.write_text(t.replace(marker, add + marker, 1))
