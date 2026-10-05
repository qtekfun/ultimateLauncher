#!/usr/bin/env python3
"""Parche 0092 (reaplicable): «Borrar los recientes» en los ajustes del dock de tablet.

 - launcher_preferences.xml: Preference `pref_ul_dock_recents_clear` (visible solo en tablet, `ul_huawei_dock`).
 - SettingsActivity.initPreference: al pulsarla llama a RecentApps.clear(...) y avisa con un Toast.
(La pulsación larga sobre un reciente vive en app/.../dock/UlDockView.kt, código propio.)
"""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_dock_recents_clear" not in t:
    add = '''    <!-- UltimateLauncher 0092 -->
    <Preference
        android:key="pref_ul_dock_recents_clear"
        android:title="@string/ul_pref_dock_recents_clear"
        android:summary="@string/ul_pref_dock_recents_clear_summary"
        android:persistent="false"
        app:isPreferenceVisible="@bool/ul_huawei_dock"
        xmlns:app="http://schemas.android.com/apk/res-auto" />

'''
    px.write_text(t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1))
ps = R / "src/com/android/launcher3/settings/SettingsActivity.java"
t = ps.read_text()
old = "        protected boolean initPreference(Preference preference) {\n            LauncherDisplayInfo info = DisplayController.INSTANCE.get(getContext()).getInfo();\n            switch (preference.getKey()) {\n"
new = old + """                case "pref_ul_dock_recents_clear": // UltimateLauncher 0092
                    preference.setOnPreferenceClickListener(p -> {
                        com.qtekfun.ultimatelauncher.dock.RecentApps.clear(getContext());
                        android.widget.Toast.makeText(getContext(), R.string.ul_dock_recents_cleared,
                                android.widget.Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    return true;
"""
if "UltimateLauncher 0092" not in t:
    assert old in t
    ps.write_text(t.replace(old, new, 1))
