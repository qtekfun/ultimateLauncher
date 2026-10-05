#!/usr/bin/env python3
"""Parche 0200 (reaplicable, idempotente): Ajustes de inicio agrupados por categorías y refresco tras restaurar una copia.

 - launcher_preferences.xml: las preferencias (las de AOSP y las añadidas por 0041..0183) pasan a siete
   `PreferenceCategory`: Pantalla de inicio, Dock de tablet (solo si `ul_huawei_dock`), Carpetas, Animaciones,
   Iconos y fondos, Gestos, Privacidad, y la ya existente Copia de seguridad (0160). Las claves no cambian (los
   scripts anteriores siguen siendo no-ops porque comprueban la clave). Si AOSP añade preferencias nuevas al actualizar,
   incorporarlas a mano a la categoría que corresponda.
 - SettingsActivity: `initPreference` se aplica también a los hijos de las categorías (antes solo al primer nivel) y se
   quitan las categorías que quedan vacías; al volver a la pantalla tras restaurar una copia
   (`BackupPrefs.restoreCount()` cambió) se recrea la actividad para que los interruptores muestren el estado restaurado.
Cadenas en app/src/main/res/values{,-es}/strings_ul.xml. Sin permisos nuevos ni red."""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
MARK = "UltimateLauncher 0200"

BODY = '''

    <!-- UltimateLauncher 0200: ajustes agrupados por categorías -->

    <PreferenceCategory
        android:key="pref_ul_cat_home"
        android:title="@string/ul_pref_cat_home"
        android:persistent="false">

        <com.android.launcher3.settings.NotificationDotsPreference
            android:key="pref_icon_badging"
            android:title="@string/notification_dots_title"
            android:persistent="false"
            android:widgetLayout="@layout/notification_pref_warning" />

        <!--
          LAUNCHER_ADD_NEW_APPS_TO_HOME_SCREEN_ENABLED(613)
          LAUNCHER_ADD_NEW_APPS_TO_HOME_SCREEN_DISABLED(614)
        -->
        <SwitchPreference
            android:key="pref_add_icon_to_home"
            android:title="@string/auto_add_shortcuts_label"
            android:summary="@string/auto_add_shortcuts_description"
            android:defaultValue="true"
            android:persistent="true"
            launcher:logIdOn="613"
            launcher:logIdOff="614" />

        <!-- UltimateLauncher 0061 -->
        <SwitchPreference
            android:key="pref_ul_allow_rotation"
            android:title="@string/ul_pref_allow_rotation"
            android:summary="@string/ul_pref_allow_rotation_summary"
            android:defaultValue="false"
            android:persistent="true" />

        <!-- UltimateLauncher 0120 (solo tablet: SettingsActivity la quita en pantallas pequeñas) -->
        <SwitchPreference
            android:key="pref_ul_edge_grid"
            android:title="@string/ul_pref_edge_grid"
            android:summary="@string/ul_pref_edge_grid_summary"
            android:defaultValue="false"
            android:persistent="true" />
    </PreferenceCategory>

    <!-- UltimateLauncher 0041 / 0092: dock de tablet (la categoría entera solo se ve en tablet) -->
    <PreferenceCategory
        android:key="pref_ul_cat_dock"
        android:title="@string/ul_pref_cat_dock"
        android:persistent="false"
        launcher:isPreferenceVisible="@bool/ul_huawei_dock">

        <SwitchPreference
            android:key="pref_ul_dock_background"
            android:title="@string/ul_pref_dock_background"
            android:summary="@string/ul_pref_dock_background_summary"
            android:defaultValue="true"
            android:persistent="true" />

        <SwitchPreference
            android:key="pref_ul_dock_subtle"
            android:title="@string/ul_pref_dock_subtle"
            android:summary="@string/ul_pref_dock_subtle_summary"
            android:defaultValue="false"
            android:persistent="true"
            android:dependency="pref_ul_dock_background" />

        <SwitchPreference
            android:key="pref_ul_dock_recents"
            android:title="@string/ul_pref_dock_recents"
            android:summary="@string/ul_pref_dock_recents_summary"
            android:defaultValue="true"
            android:persistent="true" />

        <!-- UltimateLauncher 0092 -->
        <Preference
            android:key="pref_ul_dock_recents_clear"
            android:title="@string/ul_pref_dock_recents_clear"
            android:summary="@string/ul_pref_dock_recents_clear_summary"
            android:persistent="false" />
    </PreferenceCategory>

    <PreferenceCategory
        android:key="pref_ul_cat_folders"
        android:title="@string/ul_pref_cat_folders"
        android:persistent="false">

        <!-- UltimateLauncher 0048 -->
        <SwitchPreference
            android:key="pref_ul_folder_style"
            android:title="@string/ul_pref_folder_style"
            android:summary="@string/ul_pref_folder_style_summary"
            android:defaultValue="true"
            android:persistent="true" />

        <!-- UltimateLauncher 0143 -->
        <SwitchPreference
            android:key="pref_ul_folder_expand"
            android:title="@string/ul_pref_folder_expand"
            android:summary="@string/ul_pref_folder_expand_summary"
            android:defaultValue="true"
            android:persistent="true" />
    </PreferenceCategory>

    <PreferenceCategory
        android:key="pref_ul_cat_animations"
        android:title="@string/ul_pref_cat_animations"
        android:persistent="false">

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

        <!-- UltimateLauncher 0046 -->
        <SwitchPreference
            android:key="pref_ul_gesture_contract"
            android:title="@string/ul_pref_gesture_contract"
            android:summary="@string/ul_pref_gesture_contract_summary"
            android:defaultValue="false"
            android:persistent="true" />
    </PreferenceCategory>

    <PreferenceCategory
        android:key="pref_ul_cat_icons"
        android:title="@string/ul_pref_cat_icons"
        android:persistent="false">

        <!-- UltimateLauncher 0170 -->
        <Preference
            android:key="pref_ul_wallpapers"
            android:title="@string/ul_pref_wallpapers"
            android:summary="@string/ul_pref_wallpapers_summary"
            android:persistent="false" />

        <!-- UltimateLauncher 0173 -->
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
    </PreferenceCategory>

    <PreferenceCategory
        android:key="pref_ul_cat_gestures"
        android:title="@string/ul_pref_cat_gestures"
        android:persistent="false">

        <!-- UltimateLauncher 0180 -->
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
    </PreferenceCategory>

    <PreferenceCategory
        android:key="pref_ul_cat_privacy"
        android:title="@string/ul_pref_cat_privacy"
        android:persistent="false">

        <!-- UltimateLauncher 0183 -->
        <Preference
            android:key="pref_ul_hidden_apps_open"
            android:title="@string/ul_hidden_apps"
            android:summary="@string/ul_pref_hidden_apps_summary"
            android:persistent="false" />
    </PreferenceCategory>

    <!-- UltimateLauncher 0160: copia de seguridad -->
    <PreferenceCategory
        android:key="pref_ul_backup_category"
        android:title="@string/ul_pref_backup_category"
        android:persistent="false">
        <Preference
            android:key="pref_ul_backup_save"
            android:title="@string/ul_pref_backup_save"
            android:summary="@string/ul_pref_backup_save_summary"
            android:persistent="false" />
        <Preference
            android:key="pref_ul_backup_restore"
            android:title="@string/ul_pref_backup_restore"
            android:summary="@string/ul_pref_backup_restore_summary"
            android:persistent="false" />
        <Preference
            android:key="pref_ul_backup_import"
            android:title="@string/ul_pref_backup_import"
            android:summary="@string/ul_pref_backup_import_summary"
            android:persistent="false" />
    </PreferenceCategory>

</androidx.preference.PreferenceScreen>
'''

px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if MARK not in t:
    start = t.index("<androidx.preference.PreferenceScreen")
    end_open = t.index(">", start) + 1
    px.write_text(t[:end_open] + BODY)

ps = R / "src/com/android/launcher3/settings/SettingsActivity.java"
t = ps.read_text()
if MARK not in t:
    old_loop = """            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
            }
"""
    new_loop = """            PreferenceScreen screen = getPreferenceScreen();
            initGroup(screen); // UltimateLauncher 0200
"""
    assert old_loop in t
    t = t.replace(old_loop, new_loop, 1)
    anchor = "        private boolean isKeyInPreferenceGroup("
    helper = """        /** UltimateLauncher 0200: aplica initPreference también dentro de las categorías y quita las que quedan vacías. */
        private void initGroup(PreferenceGroup group) {
            for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = group.getPreference(i);
                if (!initPreference(preference)) {
                    group.removePreference(preference);
                } else if (preference instanceof PreferenceGroup && !(preference instanceof PreferenceScreen)) {
                    PreferenceGroup child = (PreferenceGroup) preference;
                    initGroup(child);
                    if (child.getPreferenceCount() == 0) {
                        group.removePreference(child);
                    }
                }
            }
        }

"""
    assert anchor in t
    t = t.replace(anchor, helper + anchor, 1)
    old_field = "        private boolean mRestartOnResume = false;\n"
    new_field = old_field + """
        // UltimateLauncher 0200: restauraciones de copia vistas al crear la pantalla; si cambia, se recrea al volver.
        private final int mRestoreCountSeen =
                com.qtekfun.ultimatelauncher.layoutsync.BackupPrefs.restoreCount();
"""
    assert old_field in t
    t = t.replace(old_field, new_field, 1)
    old_resume = """            if (mRestartOnResume) {
                recreateActivityNow();
            }
"""
    new_resume = """            if (mRestartOnResume
                    || mRestoreCountSeen != com.qtekfun.ultimatelauncher.layoutsync.BackupPrefs.restoreCount()) {
                recreateActivityNow(); // UltimateLauncher 0200
            }
"""
    assert old_resume in t
    t = t.replace(old_resume, new_resume, 1)
    ps.write_text(t)
