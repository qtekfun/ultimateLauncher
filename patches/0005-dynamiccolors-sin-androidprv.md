# Parche 0005 — dynamiccolors sin recursos privados

Archivos: `systemui-libs/dynamiccolors/res/values/colors.xml` y `values-night/colors.xml`.
Cambio: se eliminan las 27 líneas que apuntan a `@androidprv:color/...` (recursos privados de la plataforma:
brand, clock, shade). Un APK normal no puede enlazarlos. Ninguno lo usa Launcher3 (comprobado: la compilación enlaza).
Reaplicar: `sed -i '/androidprv:color/d'` sobre esos dos archivos tras cada actualización de AOSP.

Excepción: `customColorSurfaceEffect0..3` y `customColorSurfaceEffect0Fallback` SÍ los usa Launcher3 (res/values/styles.xml, fondo de carpeta).
Se redefinen con colores públicos `@android:color/system_surface_container_{high,highest}_{light,dark}` (aproximación; se sustituirán por tokens medidos en M4).
