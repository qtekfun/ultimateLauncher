#!/usr/bin/env python3
"""Parche 0046 (reaplicable, idempotente): interruptor «Contrato del gesto de inicio» (GestureNavContract), apagado por defecto.

Con el gesto de inicio ColorOS manda en el intent HOME el contrato; Launcher.handleGestureContract muestra una
FloatingSurfaceView que OCULTA el icono real hasta que el sistema termina. En ColorOS esa vía no cierra bien (mismo
síntoma que documenta Lawnchair: «Use GestureNavContract API» apagado). Con el botón atrás no hay contrato y los
iconos salen al instante. Apagado = se ignora el contrato y los iconos quedan visibles."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
p = R / "src/com/android/launcher3/Launcher.java"; t = p.read_text()
old = "        GestureNavContract gnc = GestureNavContract.fromIntent(intent);\n        if (gnc != null) {"
new = ("        GestureNavContract gnc = GestureNavContract.fromIntent(intent);\n"
       "        if (gnc != null && getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, MODE_PRIVATE)\n"
       "                .getBoolean(\"pref_ul_gesture_contract\", false)) { // UltimateLauncher 0046")
if "pref_ul_gesture_contract" not in t:
    assert old in t; p.write_text(t.replace(old, new, 1))
px = R / "res/xml/launcher_preferences.xml"; t = px.read_text()
if "pref_ul_gesture_contract" not in t:
    add = '''
    <!-- UltimateLauncher 0046 -->
    <SwitchPreference
        android:key="pref_ul_gesture_contract"
        android:title="@string/ul_pref_gesture_contract"
        android:summary="@string/ul_pref_gesture_contract_summary"
        android:defaultValue="false"
        android:persistent="true" />

'''
    px.write_text(t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1))
