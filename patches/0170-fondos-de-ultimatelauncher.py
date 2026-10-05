#!/usr/bin/env python3
"""Parche 0170 (reaplicable, idempotente): enlaces al selector «Fondos de UltimateLauncher» (app/.../wallpaper/).

1) Menú del fondo del escritorio (pulsación larga): nueva fila «Fondos de UltimateLauncher» justo debajo de «Fondo de pantalla y
   estilo» (WorkspaceLongPressOptions.getAll). Abre WallpaperPickerActivity.
2) Ajustes de inicio: preferencia de pulsación `pref_ul_wallpapers` (launcher_preferences.xml) que abre lo mismo
   (SettingsActivity.initPreference).
Archivos: WorkspaceLongPressOptions.kt, launcher_preferences.xml, SettingsActivity.java.
"""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"

# 1) menú del escritorio
p = R / "src/com/android/launcher3/popup/WorkspaceLongPressOptions.kt"
t = p.read_text()
if "UL 0170" not in t:
    old = """                startWallpaperPicker(ac, v)
            }
        )
"""
    new = old + """        // UL 0170: fondos propios incluidos en la app (sin red)
        add(
            PopupData(
                R.drawable.ul_ic_wallpapers,
                R.string.ul_wp_menu,
                SYSTEM_SHORTCUT,
                IGNORE,
            ) { ac, _, _ ->
                ac.asContext()
                    .startActivity(
                        com.qtekfun.ultimatelauncher.wallpaper.WallpaperPickerActivity.intent(ac.asContext())
                    )
            }
        )
"""
    assert t.count(old) == 1
    p.write_text(t.replace(old, new, 1))

# 2) ajuste
p = R / "res/xml/launcher_preferences.xml"
t = p.read_text()
if "pref_ul_wallpapers" not in t:
    add = '''    <!-- UltimateLauncher 0170 -->
    <Preference
        android:key="pref_ul_wallpapers"
        android:title="@string/ul_pref_wallpapers"
        android:summary="@string/ul_pref_wallpapers_summary"
        android:persistent="false" />

'''
    marker = "    <!-- UltimateLauncher 0061 -->"
    assert marker in t
    p.write_text(t.replace(marker, add + marker, 1))

p = R / "src/com/android/launcher3/settings/SettingsActivity.java"
t = p.read_text()
if "pref_ul_wallpapers" not in t:
    old = '                case "pref_ul_dock_recents_clear": // UltimateLauncher 0092\n'
    new = '''                case "pref_ul_wallpapers": // UltimateLauncher 0170
                    preference.setOnPreferenceClickListener(p -> {
                        startActivity(com.qtekfun.ultimatelauncher.wallpaper.WallpaperPickerActivity.intent(getContext()));
                        return true;
                    });
                    return true;
''' + old
    assert t.count(old) == 1
    p.write_text(t.replace(old, new, 1))
