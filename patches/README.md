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
| 0007 | Compuerta de versión/try-catch para `AppWidgetHostView.start/stopVisibilityTracking` (API de Android 17) | WidgetVisibilityTracker.kt | `patches/0007-*.py` |
| 0008 | 12 colores `@android:color/system_*` solo de API 37 → `values-v37` + línea base M3 | `launcher3-base/res/values-v34/colors.xml`, dynamiccolors | `patches/0008-*.py` |
| 0009 | Rejilla por defecto = tokens medidos (`ultimate_phone`) | `device_profiles.xml` (bloque generado), `InvariantDeviceProfile.java` (1 línea) | `tools/apply-theme-tokens.py` |
| 0010 | Gancho `FirstRun.maybeShow(this)` en `Launcher.onCreate` | Launcher.java | `patches/0010-*.py` |
| 0011 | **Preparado, NO aplicado**: `getParcelable(Extra)` con clase (API 33) → `BundleCompat`/`IntentCompat` en el selector de widgets | WidgetPickerActivity.kt, WidgetPickerComposeWrapperImpl.kt | `patches/0011-*.py` (compila; sin verificar en dispositivo) |
| 0012 | El dock no reserva el hueco de la barra de búsqueda (≈64 dp) cuando no hay QSB | HotseatProfileInitialValues.kt | `patches/0012-*.py` |
| 0013 | Márgenes superior/inferior/laterales del área de iconos desde recursos de tokens | WorkspaceProfileNonResponsiveFactory.kt | `patches/0013-*.py` + `tools/apply-theme-tokens.py` |
| 0014 | Indicador de páginas: punto visible con una página, elevado según tokens | PageIndicatorDots.java, Workspace.java | `patches/0014-*.py` |
| 0004b | Forma de icono genérica con el trazado real de la máscara del sistema (PathParser) | ShapeDelegate.kt | `patches/0004-*.py` |

Cambios que NO son parches sobre archivos de AOSP (andamiaje propio): `build.gradle*`, `settings.gradle.kts`, `gradle/`, `app/`,
`platform-stubs/` (incluye copias sin modificar de `plugin_core` y `log/core` de frameworks/base), `launcher3-base/modules/widgetpicker/ul-build.gradle`,
`systemui-libs/*/build.gradle`, `tools/gen-flags.py` (flags aconfig → Flags.java; todos false).

Piezas de AOSP no importadas o excluidas: `quickstep/` (recientes/gestos), `viewcapturelib`, `displaylib`, `mechanics`, `cuebarlib`,
`contextualeducationlib`, `iconloaderlib/src_full_lib`, pruebas.
