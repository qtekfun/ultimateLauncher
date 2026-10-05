#!/usr/bin/env python3
"""Parche 0004 (reaplicable, idempotente): neutraliza APIs que no están en el SDK público 37.0.

- ShapeDelegate.GenericPathShape: androidx.graphics.shapes.SvgPathParser solo existe en el tip de AOSP,
  no en graphics-shapes 1.1.0 publicado. Se sustituye por un rectángulo redondeado (forma de reserva).
- App Lock (ApplicationInfo.isAppLockSupported/Enabled, PackageManager.getEnableAppLockIntentForPackage):
  API de Android 17 con FlaggedApi, ausente del SDK público. Se desactiva (siempre falso / sin intent).
"""
import pathlib
B = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
def sub(path, old, new):
    p = B / path; t = p.read_text()
    if old in t: p.write_text(t.replace(old, new))
    elif new not in t: raise SystemExit(f"no encontrado en {path}: {old[:50]}")
sub("graphics/ShapeDelegate.kt", "import androidx.graphics.shapes.SvgPathParser\n", "")
sub("graphics/ShapeDelegate.kt", """            RoundedPolygon(
                features = SvgPathParser.parseFeatures(pathString),
                centerX = 50f,
                centerY = 50f,
            )""", """            // UltimateLauncher 0004: SvgPathParser no está en el SDK público; forma de reserva.
            createRoundedRect(0f, 0f, 100f, 100f, 25f)""")
sub("util/ApplicationInfoWrapper.kt", """    fun isAppLockSupported() =
        android.security.Flags.appLockApis() && appInfo?.isAppLockSupported ?: false""", """    fun isAppLockSupported() = false // UltimateLauncher 0004""")
sub("util/ApplicationInfoWrapper.kt", """    fun isAppLockEnabled() =
        android.security.Flags.appLockApis() && appInfo?.isAppLockEnabled ?: false""", """    fun isAppLockEnabled() = false // UltimateLauncher 0004""")
sub("popup/AppLockShortcut.kt", """                        mTarget
                            .asContext()
                            .packageManager
                            .getEnableAppLockIntentForPackage(packageName, newAppLockEnabled)""", """                        null as PendingIntent? // UltimateLauncher 0004: API App Lock no pública""")

# 0004b: la forma genérica se dibuja con el trazado real de la máscara del sistema (PathParser de androidx.core, público),
# en lugar de un rectángulo redondeado fijo. El poly redondeado se mantiene solo para la animación de revelado.
sub("graphics/ShapeDelegate.kt", """        private val basePath =
            Path().apply {
                Morph(poly, createRoundedRect(0f, 0f, 100f, 100f, 25f)).toPath(0f, this)
            }""", """        private val basePath =
            androidx.core.graphics.PathParser.createPathFromPathData(pathString)
                ?: Path().apply {
                    Morph(poly, createRoundedRect(0f, 0f, 100f, 100f, 25f)).toPath(0f, this)
                }""")
