#!/usr/bin/env python3
"""Parche 0160 (reaplicable, idempotente): sección «Copia de seguridad» en Ajustes de inicio.

 - launcher_preferences.xml: PreferenceCategory `pref_ul_backup_category` con tres Preference
   (`pref_ul_backup_save`, `pref_ul_backup_restore`, `pref_ul_backup_import`), sin valor persistido.
 - SettingsActivity.initPreference: la categoría engancha los clics de sus hijos (initPreference solo recorre el primer
   nivel, por eso se hace aquí) y llama a app/.../layoutsync/BackupActivity (código propio):
   Guardar/Restaurar abren el selector de documentos del sistema; «Traer mi pantalla de inicio» abre ForeignImportActivity.
Cadenas en app/src/main/res/values{,-es}/strings_ul.xml. Sin permisos nuevos ni red."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_backup_category" not in t:
    add = '''    <!-- UltimateLauncher 0160: copia de seguridad -->
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

'''
    end = "</androidx.preference.PreferenceScreen>"
    assert end in t
    px.write_text(t.replace(end, add + end, 1))
ps = R / "src/com/android/launcher3/settings/SettingsActivity.java"
t = ps.read_text()
old = "        protected boolean initPreference(Preference preference) {\n            LauncherDisplayInfo info = DisplayController.INSTANCE.get(getContext()).getInfo();\n            switch (preference.getKey()) {\n"
new = old + """                case "pref_ul_backup_category": // UltimateLauncher 0160
                    PreferenceGroup backup = (PreferenceGroup) preference;
                    for (int i = 0; i < backup.getPreferenceCount(); i++) {
                        Preference p = backup.getPreference(i);
                        p.setOnPreferenceClickListener(c -> {
                            Context ctx = getContext();
                            switch (c.getKey()) {
                                case "pref_ul_backup_save":
                                    com.qtekfun.ultimatelauncher.layoutsync.BackupActivity.launch(ctx,
                                            com.qtekfun.ultimatelauncher.layoutsync.BackupActivity.ACTION_SAVE);
                                    break;
                                case "pref_ul_backup_restore":
                                    com.qtekfun.ultimatelauncher.layoutsync.BackupActivity.launch(ctx,
                                            com.qtekfun.ultimatelauncher.layoutsync.BackupActivity.ACTION_RESTORE);
                                    break;
                                default:
                                    com.qtekfun.ultimatelauncher.layoutsync.BackupActivity.launchImport(ctx);
                            }
                            return true;
                        });
                    }
                    return true;
"""
if "UltimateLauncher 0160" not in t:
    assert old in t
    t = t.replace(old, new, 1)
    imp = "import android.content.Intent;\n"
    assert imp in t
    if "import android.content.Context;" not in t:
        t = t.replace(imp, "import android.content.Context;\n" + imp, 1)
    ps.write_text(t)
