#!/usr/bin/env python3
"""Parche 0060 (reaplicable, idempotente): tema claro con texto legible en el cajon.
El cajon de UltimateLauncher (parche 0020) ya no es una hoja clara sino el fondo de pantalla desenfocado con un velo
oscuro, como el de OPPO. En el tema claro de AOSP `LauncherTheme` fija `textColorSecondary` = #DE000000 (texto negro, pensado
para la hoja clara), lo que dejaba las etiquetas ilegibles sobre el velo. Se cambia a blanco (el tema oscuro ya usaba
`text_color_secondary_dark`, blanco).
Ademas `BaseIcon.AllApps` fijaba `textColor` = materialColorOnSurface (oscuro en tema claro): se usa el blanco del escritorio
(`workspace_text_color_light`).
Archivo: launcher3-base/res/values/styles.xml"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/res/values/styles.xml"
t = p.read_text()
old = '<style name="LauncherTheme" parent="@style/DynamicColorsBaseLauncherTheme">\n        <item name="android:textColorSecondary">#DE000000</item>'
new = ('<style name="LauncherTheme" parent="@style/DynamicColorsBaseLauncherTheme">\n'
       '        <item name="android:textColorSecondary">#FFFFFFFF</item> <!-- UltimateLauncher 0060 -->')
if old in t:
    t = t.replace(old, new, 1)
old2 = '<item name="android:textColor">@color/materialColorOnSurface</item>\n        <item name="android:fontFamily">variable-title-small</item>'
new2 = '<item name="android:textColor">@color/workspace_text_color_light</item> <!-- UltimateLauncher 0060 -->\n        <item name="android:fontFamily">variable-title-small</item>'
if old2 in t:
    t = t.replace(old2, new2, 1)
p.write_text(t)
