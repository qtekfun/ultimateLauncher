#!/usr/bin/env python3
"""Parche 0171 (reaplicable, idempotente): ganchos de paquetes de iconos en iconloaderlib.

1) `PackIcon.kt` (archivo NUEVO): interfaz marcadora para iconos que vienen de un pack y ya traen su propia forma.
2) `IconProvider.getIcon(ComponentInfo, int)`: tras cargar el icono normal llama al gancho protegido
   `applyIconPack(info, original, dpi)` (por defecto devuelve el original). La sustitución real vive en
   `app/.../iconpack/IconPackManager.kt` y la conecta el parche 0172 en `LauncherIconProvider`.
3) `BaseIconFactory.createBadgedIconBitmap`: un `PackIcon` NO opaco (círculos, formas propias del pack) se dibuja tal cual,
   sin envolverlo en un adaptativo con baldosa clara; uno opaco sigue el camino normal, es decir, recibe la forma de icono
   medida (superelipse) como cualquier icono heredado (parche 0017b).
Archivos: systemui-libs/iconloaderlib/src/com/android/launcher3/icons/{PackIcon.kt (nuevo),IconProvider.java,BaseIconFactory.kt}
"""
import pathlib

D = pathlib.Path(__file__).resolve().parent.parent / "systemui-libs/iconloaderlib/src/com/android/launcher3/icons"

# 1) marcador
p = D / "PackIcon.kt"
if not p.exists():
    p.write_text('''package com.android.launcher3.icons

/**
 * UltimateLauncher 0171: marca los iconos que proceden de un paquete de iconos de terceros. Si no son opacos se asume que
 * ya traen su forma y la fábrica de iconos no los envuelve en un icono adaptativo (ver BaseIconFactory).
 */
interface PackIcon
''')

# 2) IconProvider
p = D / "IconProvider.java"
t = p.read_text()
if "applyIconPack" not in t:
    old = """    public Drawable getIcon(ComponentInfo info, int iconDpi) {
        return getIcon(info, info.applicationInfo, iconDpi);
    }
"""
    new = """    public Drawable getIcon(ComponentInfo info, int iconDpi) {
        // UltimateLauncher 0171: paquetes de iconos
        return applyIconPack(info, getIcon(info, info.applicationInfo, iconDpi), iconDpi);
    }

    /**
     * UltimateLauncher 0171: gancho para paquetes de iconos. Recibe el icono normal de la actividad y devuelve el que se
     * debe usar (por defecto, el mismo).
     */
    protected Drawable applyIconPack(ComponentInfo info, Drawable original, int iconDpi) {
        return original;
    }
"""
    assert t.count(old) == 1
    p.write_text(t.replace(old, new, 1))

# 3) BaseIconFactory
p = D / "BaseIconFactory.kt"
t = p.read_text()
if "rawPackIcon" not in t:
    old = """        tempIcon = fillOpaqueLegacy(tempIcon) // UltimateLauncher 0017b
        if (options.wrapNonAdaptiveIcon) tempIcon = wrapToAdaptiveIcon(tempIcon, options)
        tempIcon = fillTransparentBackground(tempIcon) // UltimateLauncher 0017
"""
    new = """        // UltimateLauncher 0171: un icono de pack no opaco ya trae su forma: no se envuelve ni se rellena
        val rawPackIcon = tempIcon is PackIcon && !isMostlyOpaque(tempIcon)
        tempIcon = fillOpaqueLegacy(tempIcon) // UltimateLauncher 0017b
        if (options.wrapNonAdaptiveIcon && !rawPackIcon) tempIcon = wrapToAdaptiveIcon(tempIcon, options)
        if (!rawPackIcon) tempIcon = fillTransparentBackground(tempIcon) // UltimateLauncher 0017
"""
    assert t.count(old) == 1, "ejecuta antes el parche 0017"
    p.write_text(t.replace(old, new, 1))
