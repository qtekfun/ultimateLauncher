#!/usr/bin/env python3
"""Parche 0061 (reaplicable, idempotente): interruptor «Girar la pantalla de inicio» (pref_ul_allow_rotation).
En telefono, RotationHelper deja el launcher en portrait fijo (SCREEN_ORIENTATION_NOSENSOR) salvo en pantallas grandes.
Con el interruptor activo el launcher sigue la orientacion del sistema (UNSPECIFIED), lo que permite usar y probar la
cuadricula, el dock y las carpetas en horizontal. Apagado por defecto (comportamiento de AOSP / OPPO: inicio vertical).
Archivos: RotationHelper.java, launcher_preferences.xml."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
p = R / "src/com/android/launcher3/states/RotationHelper.java"
t = p.read_text()
old = "                || mForceAllowRotationForTesting\n        ) {"
new = ("                || mForceAllowRotationForTesting\n"
       "                || com.qtekfun.ultimatelauncher.RotationPref.allowed(mActivity) // UltimateLauncher 0061\n        ) {")
if "UltimateLauncher 0061" not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_allow_rotation" not in t:
    add = '''    <!-- UltimateLauncher 0061 -->
    <SwitchPreference
        android:key="pref_ul_allow_rotation"
        android:title="@string/ul_pref_allow_rotation"
        android:summary="@string/ul_pref_allow_rotation_summary"
        android:defaultValue="false"
        android:persistent="true" />

'''
    px.write_text(t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1))
