#!/usr/bin/env python3
"""Parche 0183 (reaplicable, idempotente): accesos a la pantalla «Apps ocultas» (HiddenAppsActivity, código propio).

 - WorkspaceLongPressOptions: fila «Apps ocultas» en el menú del fondo del escritorio (con el icono ul_ic_hide).
 - launcher_preferences.xml + SettingsActivity.initPreference: preferencia «Apps ocultas» en Ajustes de inicio que abre
   la pantalla. La pantalla exige BiometricPrompt (huella/rostro o PIN/patrón/contraseña) antes de mostrar nada."""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
ACT = "com.qtekfun.ultimatelauncher.hidden.HiddenAppsActivity"


def sub(f, old, new, marker):
    p = R / f
    t = p.read_text()
    if marker in t:
        return
    assert t.count(old) == 1, (f, old, t.count(old))
    p.write_text(t.replace(old, new, 1))


sub("src/com/android/launcher3/popup/WorkspaceLongPressOptions.kt",
    "        if (Flags.condoPlanner()) {\n",
    "        // UltimateLauncher 0183: acceso a las apps ocultas (pide autenticarse en la propia pantalla).\n"
    "        add(\n"
    "            PopupData(\n"
    "                R.drawable.ul_ic_hide,\n"
    "                R.string.ul_hidden_apps,\n"
    "                SYSTEM_SHORTCUT,\n"
    "                IGNORE,\n"
    "            ) { ac, _, _ ->\n"
    "                ac.asContext()\n"
    "                    .startActivity(\n"
    "                        Intent()\n"
    "                            .setClassName(ac.asContext().packageName, \"" + ACT + "\")\n"
    "                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)\n"
    "                    )\n"
    "            }\n"
    "        )\n\n"
    "        if (Flags.condoPlanner()) {\n",
    "UltimateLauncher 0183")

px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_hidden_apps_open" not in t:
    add = '''    <!-- UltimateLauncher 0183 -->
    <Preference
        android:key="pref_ul_hidden_apps_open"
        android:title="@string/ul_hidden_apps"
        android:summary="@string/ul_pref_hidden_apps_summary"
        android:persistent="false" />

'''
    px.write_text(t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1))

sub("src/com/android/launcher3/settings/SettingsActivity.java",
    "            switch (preference.getKey()) {\n",
    "            switch (preference.getKey()) {\n"
    "                case \"pref_ul_hidden_apps_open\": // UltimateLauncher 0183\n"
    "                    preference.setOnPreferenceClickListener(p -> {\n"
    "                        startActivity(new android.content.Intent().setClassName(\n"
    "                                getContext().getPackageName(), \"" + ACT + "\"));\n"
    "                        return true;\n"
    "                    });\n"
    "                    return true;\n",
    "UltimateLauncher 0183")
