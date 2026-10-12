#!/usr/bin/env python3
"""Parche 0230 (reaplicable): detección de plegables (docs/11, M10). Requiere 0018, 0040, 0042, 0120 y 0200.

La base sin Quickstep solo estimaba la pantalla activa (`WindowManagerProxy.estimateInternalDisplayBounds`), así que un
plegable nunca era `TYPE_MULTI_DISPLAY`: abierto parecía tablet y cerrado móvil, con rejilla y base de datos distintas en
cada pantalla y el dock de tablet donde no tocaba.

 - dagger/Modules.kt (src_no_quickstep): `WindowManagerProxyModule` enlaza `UlWindowManagerProxy`, que añade las demás
   pantallas internas (API pública de Android 17 o tamaños recordados antes).
 - DeviceProfile.java (0040) y HotseatProfileInitialValues.kt (0042): el dock de tablet se decide con
   `Foldables.tabletDock(res, inv.deviceType)` (recursos de tablet y NO plegable) en vez de solo `R.bool.ul_huawei_dock`.
 - ThemeManager.kt (0018): la máscara de icono de tablet sale de `Foldables.tabletIconMask` (vacía en plegables).
 - SettingsActivity.java (0200): la categoría del dock de tablet se oculta en plegables; «iconos hasta el borde» (0120)
   también, porque no se aplica con dos paneles.
"""
import pathlib
ROOT = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
B = ROOT / "src/com/android/launcher3"


def sub(p, old, new, count=1):
    t = p.read_text()
    if new in t:
        return
    assert t.count(old) == count, (p, old, t.count(old))
    p.write_text(t.replace(old, new))


sub(ROOT / "src_no_quickstep/com/android/launcher3/dagger/Modules.kt",
    "@Module abstract class WindowManagerProxyModule\n",
    "@Module\n"
    "abstract class WindowManagerProxyModule {\n"
    "    // UltimateLauncher 0230: todas las pantallas internas (plegables), no solo la activa.\n"
    "    @Binds\n"
    "    abstract fun bindWindowManagerProxy(\n"
    "        impl: com.qtekfun.ultimatelauncher.foldable.UlWindowManagerProxy\n"
    "    ): com.android.launcher3.util.window.WindowManagerProxy\n"
    "}\n")

sub(B / "DeviceProfile.java",
    "if (!isVerticalBarLayout() && context.getResources().getBoolean(R.bool.ul_huawei_dock)) { // UltimateLauncher 0040",
    "if (!isVerticalBarLayout() && com.qtekfun.ultimatelauncher.foldable.Foldables.tabletDock(\n"
    "                context.getResources(), inv.deviceType)) { // UltimateLauncher 0040, 0230")

sub(B / "deviceprofile/HotseatProfileInitialValues.kt",
    "else if (res.getBoolean(R.bool.ul_huawei_dock)) // UltimateLauncher 0042",
    "else if (com.qtekfun.ultimatelauncher.foldable.Foldables.tabletDock(res, inv.deviceType)) // UltimateLauncher 0042, 0230",
    count=2)

sub(B / "graphics/ThemeManager.kt",
    "                context.resources.getString(com.android.launcher3.R.string.ul_icon_mask).isNotEmpty() ->\n"
    "                    context.resources.getString(com.android.launcher3.R.string.ul_icon_mask) // UL 0018\n",
    "                com.qtekfun.ultimatelauncher.foldable.Foldables.tabletIconMask(context).isNotEmpty() ->\n"
    "                    com.qtekfun.ultimatelauncher.foldable.Foldables.tabletIconMask(context) // UL 0018, 0230\n")

sub(B / "settings/SettingsActivity.java",
    "                case \"pref_ul_cat_dual\": // UltimateLauncher 0220\n",
    "                case \"pref_ul_cat_dock\": // UltimateLauncher 0230: dock de tablet, no en plegables\n"
    "                    return com.qtekfun.ultimatelauncher.foldable.Foldables.tabletDock(getContext());\n"
    "                case \"pref_ul_cat_dual\": // UltimateLauncher 0220\n")

sub(B / "settings/SettingsActivity.java",
    "                    return info.isLargeScreen(info.realBounds);\n",
    "                    return info.isLargeScreen(info.realBounds)\n"
    "                            && info.getDeviceType() != TYPE_MULTI_DISPLAY; // UltimateLauncher 0230\n")
