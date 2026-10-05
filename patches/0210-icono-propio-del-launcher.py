#!/usr/bin/env python3
"""Parche 0210 (reaplicable, idempotente): icono propio de UltimateLauncher en lugar del de Launcher3 de AOSP.

F-Droid pide que un fork no reutilice el icono del proyecto original. launcher3-base/res/drawable/ic_launcher_home.xml
(el icono de la aplicación en el manifiesto) pasa a ser un icono adaptable con nuestro vector (app/src/main/res/drawable/
ic_ul_launcher_foreground.xml), fondo índigo liso y capa monocroma para el tema dinámico. Los PNG de AOSP
(mipmap-*/ic_launcher_home_foreground.png) quedan sin uso."""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/res/drawable/ic_launcher_home.xml"
t = p.read_text()
MARK = "UltimateLauncher 0210"
if MARK not in t:
    p.write_text('''<?xml version="1.0" encoding="utf-8"?>
<!-- UltimateLauncher 0210: icono propio (rejilla 2x2 con una carpeta). Sustituye al de Launcher3 de AOSP. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ul_launcher_icon_background" />
    <foreground android:drawable="@drawable/ic_ul_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_ul_launcher_monochrome" />
</adaptive-icon>
''')
