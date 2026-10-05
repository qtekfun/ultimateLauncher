#!/usr/bin/env python3
"""Parche 0045 (reaplicable, idempotente): la ventana del launcher no usa animaciones de actividad/tarea del sistema
(taskToFront, activityOpen...). Hipótesis: el retraso de ~0,9 s hasta el foco tras el gesto de inicio incluye la
animación de tarea de ColorOS sobre nuestra ventana. Experimento; si no mejora, se revierte."""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/res/values/styles.xml"
t = p.read_text()
if "UlHomeWindowAnim" not in t:
    anchor = '        <item name="android:windowShowWallpaper">true</item>\n'
    assert anchor in t
    t = t.replace(anchor, anchor + '        <item name="android:windowAnimationStyle">@style/UlHomeWindowAnim</item> <!-- UltimateLauncher 0045 -->\n', 1)
    style = '''
    <!-- UltimateLauncher 0045: sin animaciones de ventana propias de la actividad de inicio. -->
    <style name="UlHomeWindowAnim" parent="@android:style/Animation.Activity">
        <item name="android:activityOpenEnterAnimation">@null</item>
        <item name="android:activityOpenExitAnimation">@null</item>
        <item name="android:activityCloseEnterAnimation">@null</item>
        <item name="android:activityCloseExitAnimation">@null</item>
        <item name="android:taskOpenEnterAnimation">@null</item>
        <item name="android:taskOpenExitAnimation">@null</item>
        <item name="android:taskCloseEnterAnimation">@null</item>
        <item name="android:taskCloseExitAnimation">@null</item>
        <item name="android:taskToFrontEnterAnimation">@null</item>
        <item name="android:taskToFrontExitAnimation">@null</item>
        <item name="android:taskToBackEnterAnimation">@null</item>
        <item name="android:taskToBackExitAnimation">@null</item>
        <item name="android:wallpaperOpenEnterAnimation">@null</item>
        <item name="android:wallpaperOpenExitAnimation">@null</item>
        <item name="android:wallpaperCloseEnterAnimation">@null</item>
        <item name="android:wallpaperCloseExitAnimation">@null</item>
        <item name="android:wallpaperIntraOpenEnterAnimation">@null</item>
        <item name="android:wallpaperIntraOpenExitAnimation">@null</item>
        <item name="android:wallpaperIntraCloseEnterAnimation">@null</item>
        <item name="android:wallpaperIntraCloseExitAnimation">@null</item>
    </style>
'''
    t = t.replace("</resources>", style + "</resources>")
    p.write_text(t)
