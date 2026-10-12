#!/usr/bin/env python3
"""Parche 0220 (reaplicable, idempotente): Dual apps (docs/10, M9).

La lógica es código propio (app/.../dualapps/: DualAppsLogic, DualApps, DualAppShortcuts, DualSwipeController,
DualAppTileService). Requiere el flag `enable_private_space` activo en tools/gen-flags.py (sección de Private Space del
cajón) y el permiso normal ACCESS_HIDDEN_PROFILES en app/src/main/AndroidManifest.xml (docs/09). Este script solo toca
AOSP:
 - AppInfo.java: AOSP marca las apps del Private Space como no fijables (FLAG_NOT_PINNABLE); se permite fijarlas en el
   escritorio mientras Dual apps esté activo (`DualApps.pinPrivateApps`). Un icono fijado de un perfil bloqueado ya sale
   en gris (FLAG_DISABLED_QUIET_USER de AOSP) y al tocarlo el sistema pide el desbloqueo.
 - Launcher.java: `DualApps.init` en onCreate; «Abrir en la otra cuenta» y «Añadir cuenta 2 al escritorio» arriba del
   menú contextual (getSupportedShortcuts); `DualSwipeController` antes de AllAppsSwipeController (solo intercepta
   sobre iconos con par).
 - ItemClickHandler.java: recuerda la última app emparejada abierta (para el mosaico) y, si el icono es de un Private
   Space bloqueado, pide el desbloqueo y abre la app al desbloquear (Android no lo hace para perfiles privados).
 - launcher_preferences.xml + SettingsActivity.java: categoría «Apps dobles» (se quita en API < 35).
 - SettingsCache.java: una clave @hide no legible (SecurityException) usa su valor por defecto en vez de cerrar el
   launcher (hide_privatespace_entry_point al bloquear el Private Space).
"""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
P = "com.qtekfun.ultimatelauncher.dualapps."
MARK = "UltimateLauncher 0220"


def sub(f, old, new, count=1):
    p = R / f
    t = p.read_text()
    if new in t:
        return
    assert t.count(old) == count, (f, old, t.count(old))
    p.write_text(t.replace(old, new))


# Los ajustes @hide (p. ej. hide_privatespace_entry_point, «ocultar el espacio privado al bloquearlo») no se pueden
# leer desde una app de terceros desde Android 12: Settings lanza SecurityException y el launcher se cerraba al bloquear
# el Private Space (visto en el Pixel 8, Android 17). Se usa el valor por defecto; Dual apps ofrece «ocultar pares».
sub("src/com/android/launcher3/util/SettingsCache.java",
    "        int defaultValue = mUrisEnabledByDefault.contains(keyUri) ? 1 : 0;\n"
    "        if (keyUri.toString().startsWith(SYSTEM_URI_PREFIX)) {",
    "        int defaultValue = mUrisEnabledByDefault.contains(keyUri) ? 1 : 0;\n"
    "        try { // " + MARK + ": claves @hide no legibles por terceros\n"
    "        if (keyUri.toString().startsWith(SYSTEM_URI_PREFIX)) {")
sub("src/com/android/launcher3/util/SettingsCache.java",
    "            newVal = Settings.Secure.getInt(mResolver, key, defaultValue) == 1;\n        }\n",
    "            newVal = Settings.Secure.getInt(mResolver, key, defaultValue) == 1;\n        }\n"
    "        } catch (SecurityException e) { // " + MARK + "\n"
    "            newVal = defaultValue == 1;\n"
    "        }\n")

sub("src/com/android/launcher3/model/data/AppInfo.java",
    "        if (userIconInfo.isPrivate()) {\n            info.runtimeStatusFlags |= FLAG_NOT_PINNABLE;",
    "        if (userIconInfo.isPrivate() && !" + P + "DualApps.pinPrivateApps) { // " + MARK + "\n"
    "            info.runtimeStatusFlags |= FLAG_NOT_PINNABLE;")

L = "src/com/android/launcher3/Launcher.java"
sub(L, "        com.qtekfun.ultimatelauncher.oem.FirstRun.maybeShow(this); // UltimateLauncher 0010\n",
    "        com.qtekfun.ultimatelauncher.oem.FirstRun.maybeShow(this); // UltimateLauncher 0010\n"
    "        " + P + "DualApps.init(this); // " + MARK + "\n")
sub(L, "        return new TouchController[] {getDragController(), new AllAppsSwipeController(this),",
    "        return new TouchController[] {getDragController(),\n"
    "                new " + P + "DualSwipeController(this), // " + MARK + "\n"
    "                new AllAppsSwipeController(this),")
p = R / L
t = p.read_text()
start = t.index("public Stream<SystemShortcut.Factory> getSupportedShortcuts(ItemInfo itemInfo) {")
end = t.index("\n    }\n", start)
body = t[start:end]
dual = P + "OpenOtherCopyShortcut.FACTORY, " + P + "AddOtherCopyShortcut.FACTORY, "
if dual not in body:
    assert body.count("Stream.of(APP_INFO, ") == 4, body
    body = body.replace("Stream.of(APP_INFO, ", "Stream.of(" + dual + "APP_INFO, ")
    body = body.replace("// UltimateLauncher 0182", "// UltimateLauncher 0182, 0220")
    p.write_text(t[:start] + body + t[end:])

sub("src/com/android/launcher3/touch/ItemClickHandler.java",
    "        }\n        launcher.startActivitySafely(v, intent, item);\n    }\n",
    "        }\n        if (" + P + "DualApps.interceptLaunch(launcher, item)) return; // " + MARK + "\n"
    "        launcher.startActivitySafely(v, intent, item);\n    }\n")

sub("src/com/android/launcher3/settings/SettingsActivity.java",
    '                case "pref_ul_hidden_apps_open": // UltimateLauncher 0183\n',
    '                case "pref_ul_cat_dual": // ' + MARK + '\n'
    '                    return ' + P + 'DualApps.isSupported(getContext());\n'
    '                case "pref_ul_hidden_apps_open": // UltimateLauncher 0183\n')

sub("res/xml/launcher_preferences.xml",
    '    <PreferenceCategory\n        android:key="pref_ul_cat_privacy"',
    '''    <!-- UltimateLauncher 0220: Dual apps (docs/10) -->
    <PreferenceCategory
        android:key="pref_ul_cat_dual"
        android:title="@string/ul_pref_cat_dual"
        android:persistent="false">

        <SwitchPreference
            android:key="pref_ul_dual_apps"
            android:title="@string/ul_pref_dual_apps"
            android:summary="@string/ul_pref_dual_apps_summary"
            android:defaultValue="true"
            android:persistent="true" />

        <SwitchPreference
            android:key="pref_ul_dual_swipe"
            android:title="@string/ul_pref_dual_swipe"
            android:summary="@string/ul_pref_dual_swipe_summary"
            android:defaultValue="true"
            android:dependency="pref_ul_dual_apps"
            android:persistent="true" />

        <SwitchPreference
            android:key="pref_ul_dual_hide"
            android:title="@string/ul_pref_dual_hide"
            android:summary="@string/ul_pref_dual_hide_summary"
            android:defaultValue="false"
            android:dependency="pref_ul_dual_apps"
            android:persistent="true" />
    </PreferenceCategory>

    <PreferenceCategory
        android:key="pref_ul_cat_privacy"''')
