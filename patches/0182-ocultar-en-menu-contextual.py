#!/usr/bin/env python3
"""Parche 0182 (reaplicable, idempotente): entrada «Ocultar» en el menú contextual de las apps.

La entrada es código propio (app/.../hidden/HideAppShortcut.kt, icono ul_ic_hide.xml); sale con el estilo unificado de
menús (parche 0150, ContextMenuStyle) porque es una fila más de SystemShortcut. Launcher.getSupportedShortcuts la añade
para el escritorio, el dock, las carpetas y el cajón. Solo aplica a apps del perfil principal (HideAppShortcut.applies).
Si no hay bloqueo de pantalla, la acción avisa y no oculta (docs/09)."""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
F = "com.qtekfun.ultimatelauncher.hidden.HideAppShortcut.FACTORY"
p = R / "src/com/android/launcher3/Launcher.java"
t = p.read_text()
if "UltimateLauncher 0182" not in t:
    reps = [
        ("            return Stream.of(APP_INFO, WIDGETS, INSTALL, REMOVE);",
         "            return Stream.of(APP_INFO, WIDGETS, INSTALL, " + F + ", REMOVE); // UltimateLauncher 0182"),
        ("                return Stream.of(APP_INFO, WIDGETS, INSTALL, ADD_TO_HOME_SCREEN);",
         "                return Stream.of(APP_INFO, WIDGETS, INSTALL, ADD_TO_HOME_SCREEN, " + F + "); // UltimateLauncher 0182"),
        ("                return Stream.of(APP_INFO, WIDGETS, INSTALL);\n            }\n        }\n        return Stream.of(APP_INFO, WIDGETS, INSTALL);",
         "                return Stream.of(APP_INFO, WIDGETS, INSTALL, " + F + "); // UltimateLauncher 0182\n            }\n        }\n"
         "        return Stream.of(APP_INFO, WIDGETS, INSTALL, " + F + "); // UltimateLauncher 0182"),
    ]
    for old, new in reps:
        assert t.count(old) == 1, old
        t = t.replace(old, new, 1)
    p.write_text(t)
