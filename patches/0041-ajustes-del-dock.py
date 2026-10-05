#!/usr/bin/env python3
"""Parche 0041 (reaplicable): ajustes del dock de tablet en la pantalla de ajustes del launcher.

Añade a launcher_preferences.xml: fondo del dock (mostrar / sutil) y «últimas apps usadas» (si se
desactiva, se borran y deja de registrarse). Las cadenas están en app/src/main/res/values{,-es}/strings_ul.xml.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/res/xml/launcher_preferences.xml"
t = p.read_text()
if "pref_ul_dock_background" not in t:
    add = '''
    <!-- UltimateLauncher 0041: dock de tablet -->
    <SwitchPreference
        android:key="pref_ul_dock_background"
        android:title="@string/ul_pref_dock_background"
        android:summary="@string/ul_pref_dock_background_summary"
        android:defaultValue="true"
        android:persistent="true"
        app:isPreferenceVisible="@bool/ul_huawei_dock"
        xmlns:app="http://schemas.android.com/apk/res-auto" />

    <SwitchPreference
        android:key="pref_ul_dock_subtle"
        android:title="@string/ul_pref_dock_subtle"
        android:summary="@string/ul_pref_dock_subtle_summary"
        android:defaultValue="false"
        android:persistent="true"
        android:dependency="pref_ul_dock_background"
        app:isPreferenceVisible="@bool/ul_huawei_dock"
        xmlns:app="http://schemas.android.com/apk/res-auto" />

    <SwitchPreference
        android:key="pref_ul_dock_recents"
        android:title="@string/ul_pref_dock_recents"
        android:summary="@string/ul_pref_dock_recents_summary"
        android:defaultValue="true"
        android:persistent="true"
        app:isPreferenceVisible="@bool/ul_huawei_dock"
        xmlns:app="http://schemas.android.com/apk/res-auto" />

'''
    t = t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1)
    p.write_text(t)
