# Registro de parches sobre AOSP

Base importada sin modificar en el commit "Importación AOSP sin modificar" (ver UPSTREAM.md). Todo cambio sobre
`launcher3-base/` y `systemui-libs/` se lista aquí con archivo, motivo y cómo reaplicarlo tras actualizar AOSP.

| N.º | Qué | Archivos | Reaplicar |
|---|---|---|---|
| 0001 | Manifiesto propio recortado según docs/09 (sin QUERY_ALL_PACKAGES, almacenamiento, llamadas, notificaciones, AppFunctions service, proveedores exportados); `<queries>` limitado a actividades de lanzador. Sustituye a AndroidManifest*.xml de AOSP, que ya no se usan | `app/src/main/AndroidManifest.xml` (nuevo; los de AOSP quedan sin tocar) | Comparar con AndroidManifest*.xml de AOSP al actualizar |
| 0002 | Redirige las clases `Flags` de plataforma (window, wm.shell, systemui.shared, media, multiuser, security, appwidget) a stubs `com.qtekfun.stubs.*` para evitar colisión con las clases ocultas del framework | `patches/0002-*.sh`, `tools/gen-stub-flags.py` | Ejecutar ambos |
| 0003 | KSP no resuelve en Java el import estático de un enum Kotlin (Dagger falla) | SettingsCache.java, ScreenOnTracker.java | `patches/0003-*.sh` |
| 0004 | APIs no públicas: `SvgPathParser` (forma de reserva) y App Lock (desactivado) | ShapeDelegate.kt, ApplicationInfoWrapper.kt, AppLockShortcut.kt | `patches/0004-*.py` |
| 0005 | dynamiccolors sin `@androidprv:` y con `customColorSurfaceEffect*` públicos | `systemui-libs/dynamiccolors/res/**/colors.xml` | ver `0005-*.md` |
| 0006 | `LoaderTask.sendFirstScreenActiveInstallsBroadcast` desactivado (privacidad + NPE) | LoaderTask.java | `patches/0006-*.py` |

Cambios que NO son parches sobre archivos de AOSP (andamiaje propio): `build.gradle*`, `settings.gradle.kts`, `gradle/`, `app/`,
`platform-stubs/` (incluye copias sin modificar de `plugin_core` y `log/core` de frameworks/base), `launcher3-base/modules/widgetpicker/ul-build.gradle`,
`systemui-libs/*/build.gradle`, `tools/gen-flags.py` (flags aconfig → Flags.java; todos false).

Piezas de AOSP no importadas o excluidas: `quickstep/` (recientes/gestos), `viewcapturelib`, `displaylib`, `mechanics`, `cuebarlib`,
`contextualeducationlib`, `iconloaderlib/src_full_lib`, pruebas.
