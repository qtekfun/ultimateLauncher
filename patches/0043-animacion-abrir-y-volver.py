#!/usr/bin/env python3
"""Parche 0043 (reaplicable): animación de abrir (escala desde el icono) y de volver (pop del icono) del launcher.

 - ActivityContext.getActivityLaunchOptions: makeClipRevealAnimation -> OpenReturnAnim.makeOptions (escala o revelado).
 - Launcher.startActivitySafely: OpenReturnAnim.remember(v).
 - Launcher.onResume: OpenReturnAnim.playReturn(this).
 - launcher_preferences.xml: interruptores «Animación de apertura con escala» y «Animación al volver al icono».
"""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
def sub(f, old, new):
    p = R / f; t = p.read_text()
    if new in t: return
    assert old in t, (f, old)
    p.write_text(t.replace(old, new, 1))
sub("src/com/android/launcher3/views/ActivityContext.java",
    "allowBGLaunch(ActivityOptions.makeClipRevealAnimation(v, left, top, width, height));",
    "allowBGLaunch(com.qtekfun.ultimatelauncher.anim.OpenReturnAnim.makeOptions(v, left, top, width, height)); // UltimateLauncher 0043")
sub("src/com/android/launcher3/Launcher.java",
    "        com.qtekfun.ultimatelauncher.dock.RecentApps.record(this, intent, item); // UltimateLauncher 0040\n",
    "        com.qtekfun.ultimatelauncher.dock.RecentApps.record(this, intent, item); // UltimateLauncher 0040\n"
    "        com.qtekfun.ultimatelauncher.anim.OpenReturnAnim.remember(v); // UltimateLauncher 0043\n")
pf = R / "src/com/android/launcher3/Launcher.java"
t = pf.read_text()
if "OpenReturnAnim.playReturn" not in t:
    import re
    m = re.search(r"(    protected void onResume\(\) \{\n(?:.*\n)*?        super\.onResume\(\);\n)", t)
    assert m, "onResume no encontrado"
    t = t.replace(m.group(1), m.group(1) + "        com.qtekfun.ultimatelauncher.anim.OpenReturnAnim.playReturn(this); // UltimateLauncher 0043\n", 1)
    pf.write_text(t)
px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_open_scale" not in t:
    add = '''
    <!-- UltimateLauncher 0043: animaciones de abrir/volver -->
    <SwitchPreference
        android:key="pref_ul_open_scale"
        android:title="@string/ul_pref_open_scale"
        android:summary="@string/ul_pref_open_scale_summary"
        android:defaultValue="true"
        android:persistent="true" />

    <SwitchPreference
        android:key="pref_ul_return_pop"
        android:title="@string/ul_pref_return_pop"
        android:summary="@string/ul_pref_return_pop_summary"
        android:defaultValue="true"
        android:persistent="true" />

'''
    t = t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1)
    px.write_text(t)
