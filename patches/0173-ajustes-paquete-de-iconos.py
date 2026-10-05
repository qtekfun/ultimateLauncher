#!/usr/bin/env python3
"""Parche 0173 (reaplicable, idempotente): ajustes «Paquete de iconos» y «Aplicar fondo del pack a iconos sin tema».

- `launcher_preferences.xml`: `pref_ul_icon_pack` (Preference, abre el selector con el estilo unificado) y
  `pref_ul_icon_pack_back` (SwitchPreference, por defecto activo).
- `SettingsActivity.initPreference`: delega en `IconPackSettings` (app/.../iconpack/).
Archivos: launcher3-base/res/xml/launcher_preferences.xml, .../settings/SettingsActivity.java
"""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"

p = R / "res/xml/launcher_preferences.xml"
t = p.read_text()
if "pref_ul_icon_pack" not in t:
    add = '''    <!-- UltimateLauncher 0173 -->
    <Preference
        android:key="pref_ul_icon_pack"
        android:title="@string/ul_pref_icon_pack"
        android:summary="@string/ul_icon_pack_default"
        android:persistent="false" />

    <SwitchPreference
        android:key="pref_ul_icon_pack_back"
        android:title="@string/ul_pref_icon_pack_back"
        android:summary="@string/ul_pref_icon_pack_back_summary"
        android:defaultValue="true"
        android:persistent="true" />

'''
    marker = "    <!-- UltimateLauncher 0061 -->"
    assert marker in t
    p.write_text(t.replace(marker, add + marker, 1))

p = R / "src/com/android/launcher3/settings/SettingsActivity.java"
t = p.read_text()
if "pref_ul_icon_pack" not in t:
    old = '                case "pref_ul_dock_recents_clear": // UltimateLauncher 0092\n'
    new = '''                case "pref_ul_icon_pack": // UltimateLauncher 0173
                    return com.qtekfun.ultimatelauncher.iconpack.IconPackSettings.initPicker(preference, getContext());
                case "pref_ul_icon_pack_back": // UltimateLauncher 0173
                    return com.qtekfun.ultimatelauncher.iconpack.IconPackSettings.initBackSwitch(preference, getContext());
''' + old
    assert t.count(old) == 1
    p.write_text(t.replace(old, new, 1))
