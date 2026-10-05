#!/usr/bin/env python3
"""Parche 0180 (reaplicable, idempotente): gesto «deslizar hacia abajo» en el escritorio.

Abre el panel de notificaciones (mitad izquierda) o los ajustes rápidos (mitad derecha), configurable. Apagado por
defecto. La lógica y el controlador táctil son código propio (app/.../gesture/: HomeSwipeLogic, HomeSwipeController,
StatusBarPanels, HomeSwipePrefs); este script solo toca AOSP:
 - Launcher.createTouchControllers: añade HomeSwipeController tras AllAppsSwipeController (que en el estado NORMAL solo
   detecta hacia arriba; el nuevo solo hacia abajo, así no se pisan).
 - launcher_preferences.xml: tres SwitchPreference (pref_ul_swipe_down, pref_ul_swipe_split, pref_ul_swipe_swap).
Permiso nuevo: android.permission.EXPAND_STATUS_BAR (normal), declarado en app/src/main/AndroidManifest.xml (docs/09)."""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
HS = "com.qtekfun.ultimatelauncher.gesture.HomeSwipeController"


def sub(f, old, new, marker):
    p = R / f
    t = p.read_text()
    if marker in t:
        return
    assert t.count(old) == 1, (f, old, t.count(old))
    p.write_text(t.replace(old, new, 1))


sub("src/com/android/launcher3/Launcher.java",
    "        return new TouchController[] {getDragController(), new AllAppsSwipeController(this)};",
    "        return new TouchController[] {getDragController(), new AllAppsSwipeController(this),\n"
    "                new " + HS + "(this)}; // UltimateLauncher 0180",
    "UltimateLauncher 0180")

px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_swipe_down" not in t:
    add = '''    <!-- UltimateLauncher 0180 -->
    <SwitchPreference
        android:key="pref_ul_swipe_down"
        android:title="@string/ul_pref_swipe_down"
        android:summary="@string/ul_pref_swipe_down_summary"
        android:defaultValue="false"
        android:persistent="true" />

    <SwitchPreference
        android:key="pref_ul_swipe_split"
        android:title="@string/ul_pref_swipe_split"
        android:summary="@string/ul_pref_swipe_split_summary"
        android:defaultValue="true"
        android:dependency="pref_ul_swipe_down"
        android:persistent="true" />

    <SwitchPreference
        android:key="pref_ul_swipe_swap"
        android:title="@string/ul_pref_swipe_swap"
        android:summary="@string/ul_pref_swipe_swap_summary"
        android:defaultValue="false"
        android:dependency="pref_ul_swipe_down"
        android:persistent="true" />

'''
    px.write_text(t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1))
