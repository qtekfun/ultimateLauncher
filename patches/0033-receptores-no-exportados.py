#!/usr/bin/env python3
"""Parche 0033 (reaplicable): `SimpleBroadcastReceiver.register` sin indicador -> RECEIVER_NOT_EXPORTED (API 33+).

Con targetSdk >= 34, en Android 14+ `registerReceiver` sin RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED lanza SecurityException salvo que
el filtro sea SOLO de difusiones del sistema. Hoy todos los usos lo son (idioma, usuarios, pantalla, paquetes, fondo...), pero un
filtro nuevo o un cambio de lista de difusiones protegidas lo rompería al arrancar. Las difusiones del sistema llegan igual a un
receptor no exportado, así que es seguro. En API 31-32 el indicador no existe: se deja 0.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/util/SimpleBroadcastReceiver.kt"
t = p.read_text()
old = "            context.registerReceiver(this, filter, permission, callbackExecutor.handler, flags)\n"
new = """            // UltimateLauncher 0033: sin indicador explícito, no exportado (API 33+; las difusiones del sistema llegan igual).
            val safeFlags =
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    (flags and (Context.RECEIVER_EXPORTED or Context.RECEIVER_NOT_EXPORTED)) == 0
                ) {
                    flags or Context.RECEIVER_NOT_EXPORTED
                } else {
                    flags
                }
            context.registerReceiver(this, filter, permission, callbackExecutor.handler, safeFlags)
"""
if new not in t:
    assert old in t
    p.write_text(t.replace(old, new))
