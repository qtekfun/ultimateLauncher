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
| 0011 | `getParcelable(Extra)` con clase (API 33) → `BundleCompat`/`IntentCompat` en el selector de widgets (aplicado el 2026-10-05) | WidgetPickerActivity.kt, WidgetPickerComposeWrapperImpl.kt | `patches/0011-*.py` (compila; sin verificar en dispositivo) |
| 0012 | El dock no reserva el hueco de la barra de búsqueda (≈64 dp) cuando no hay QSB | HotseatProfileInitialValues.kt | `patches/0012-*.py` |
| 0013 | Márgenes superior/inferior/laterales del área de iconos desde recursos de tokens | WorkspaceProfileNonResponsiveFactory.kt | `patches/0013-*.py` + `tools/apply-theme-tokens.py` |
| 0014 | Indicador de páginas: punto visible con una página, elevado según tokens | PageIndicatorDots.java, Workspace.java | `patches/0014-*.py` |
| 0004b | Forma de icono genérica con el trazado real de la máscara del sistema (PathParser) | ShapeDelegate.kt | `patches/0004-*.py` |
| 0015 | Colores de sistema de API 34 → paleta de API 31 (Android 12–13) en `values-v31` y `dynamiccolors` | res/values{,-night}-v31, dynamiccolors | `patches/0015-*.py` |
| 0016 | `OseWidgetView` no pinta el widget de búsqueda sin proveedor (sin GMS) | qsb/OseWidgetView.kt | `patches/0016-*.py` |
| 0030 | WidgetPickerActivity y AddItemActivity sin `OnBackAnimationCallback` (API 34): `OnBackInvokedCallback` solo con API ≥ 33, `onBackPressed` + `OnBackPressedDispatcher` único en 31–32 | WidgetPickerActivity.kt, AddItemActivity.kt | `patches/0030-*.py` (compila; sin verificar en dispositivo) |
| 0031 | `LinkedHashSet.addLast/removeLast` (API 35) → `add` / `remove(last())` (código inalcanzable) | FirstScreenBroadcastHelper.kt | `patches/0031-*.py` |
| 0032 | Colores propios `@color/system_*` que solo estaban en `values-v34` → alternativa generada en `values/ul_system_colors_fallback.xml` (paleta API 31, igual que 0015). Requiere 0008 y 0015 | `launcher3-base/res/values/ul_system_colors_fallback.xml` (nuevo; `values-v34` sin tocar) | `patches/0032-*.py` |
| 0033 | `SimpleBroadcastReceiver.register` sin indicador → `RECEIVER_NOT_EXPORTED` en API ≥ 33 | SimpleBroadcastReceiver.kt | `patches/0033-*.py` |
| 0020 | Cajón a pantalla completa: hoja desde y=0, sin esquinas ni asa, velo de tokens (`ul_drawer_scrim_*`), hueco superior (`ul_drawer_top_gap`); `Launcher` aplica `blur behind` de ventana proporcional al progreso (reserva: velo más opaco si `isCrossWindowBlurEnabled()` es false) | ActivityAllAppsContainerView.java, all_apps_bottom_sheet_background.xml, AllAppsState.java (src_no_quickstep), Launcher.java | `patches/0020-*.py` + `tools/apply-theme-tokens.py` |
| 0021 | Buscador del cajón abajo (píldora translúcida, dentro del cajón), lista recortada sobre él, texto a la izquierda | ActivityAllAppsContainerView.java, AppsSearchContainerLayout.java, FloatingHeaderView.java, search_container_all_apps.xml | `patches/0021-*.py` + generador (superpone `bg_all_apps_searchbox` y `all_apps_search_hint` en `themetokens/res`) |
| 0022 | Margen lateral de la rejilla del cajón (móvil) desde tokens (14,9 dp) | AllAppsProfile.kt | `patches/0022-*.py` + generador |
| 0023 | Barra A-Z siempre visible a la derecha (activa el flag `letter_fast_scroller` en `tools/gen-flags.py`; letras en `FrameLayout` con paso fijo de tokens, sin pulgar en reposo) | AllAppsRecyclerView.java, FastScrollRecyclerView.java, LetterListTextView.java, RecyclerViewFastScroller.java, ActivityAllAppsContainerView.java, all_apps_fast_scroller.xml | `python3 tools/gen-flags.py` + `patches/0023-*.py` + generador |
| 0024 | Indicador con UNA página: punto redondo (no píldora), tamaño y desplazamiento de tokens | PageIndicatorDots.java | `patches/0024-*.py` + generador (`page_indicator_dot_size`) |
| 0025 | El buscador de abajo sube con el teclado (`WindowInsetsAnimation.Callback`) y la barra A-Z se oculta al buscar | AppsSearchContainerLayout.java, ActivityAllAppsContainerView.java | `patches/0025-*.py` |

| 0048-0050 | Carpeta abierta/cerrada al estilo OPPO (ver progress.md). 2026-10-05: 0048 pasa también la altura a `FolderStyle.position` (no sale por abajo en horizontal); 0049 usa el lado corto (<600 dp) para decidir «teléfono» y reduce las filas por página en horizontal (`FolderStyle.rowsFor`) | Folder.java, FolderProfile.kt | `patches/0048-*.py`, `0049-*.py` |
| 0060 | Tema claro legible: `textColorSecondary` y el color de etiqueta de `BaseIcon.AllApps` en blanco (el cajón es ya fondo desenfocado con velo, no hoja clara) | launcher3-base/res/values/styles.xml | `patches/0060-*.py` |
| 0061 | Interruptor «Girar la pantalla de inicio» (apagado por defecto): el launcher sigue la orientación del sistema en teléfono | RotationHelper.java, launcher_preferences.xml, `app/.../RotationPref.kt` | `patches/0061-*.py` |

Además: `coreLibraryDesugaringEnabled` (desugar_jdk_libs 2.1.5) en `app/build.gradle` para `Stream.toList` (API 34) en Android 12–13.
| 0017 | Iconos: adaptativos sin fondo → baldosa clara; heredados opacos → a sangre con el color de su borde | iconloaderlib BaseIconFactory.kt | `patches/0017-*.py` |
| 0018 | Máscara de icono desde tokens (`ul_icon_mask`) | ThemeManager.kt | `patches/0018-*.py` + `tools/apply-tablet-tokens.py` |
| 0019 | Forma de icono propia también fuera de iconos temáticos (cajón) | ItemInfoWithIcon.java | `patches/0019-*.py` |
| 0040 | Dock de tablet estilo Huawei (hotseat centrado + `UlDockView` + `RecentApps`) | Launcher.java, DeviceProfile.java | `patches/0040-*.py` |
| 0041 | Ajustes del dock (estilo de fondo y recientes) | launcher_preferences.xml | `patches/0041-*.py` |
Además: `coreLibraryDesugaringEnabled` (desugar_jdk_libs 2.1.5) en `app/build.gradle` para `Stream.toList` (API 34) en Android 12–13. `lint { ... }` en `app/build.gradle`: `NewApi` como error y ruido heredado de AOSP desactivado (docs/compat-android12-14.md).
| 0043 | Animación de abrir (escala desde el icono) y de volver (pop del icono) + 2 ajustes | ActivityContext.java, Launcher.java, launcher_preferences.xml | `patches/0043-*.py` |
| 0044 | Diagnóstico de ciclo de vida e iconos (solo debug, `ULDIAG`) | Launcher.java | `patches/0044-*.py` |

Cambios que NO son parches sobre archivos de AOSP (andamiaje propio): `build.gradle*`, `settings.gradle.kts`, `gradle/`, `app/`,
`platform-stubs/` (incluye copias sin modificar de `plugin_core` y `log/core` de frameworks/base), `launcher3-base/modules/widgetpicker/ul-build.gradle`,
`systemui-libs/*/build.gradle`, `tools/gen-flags.py` (flags aconfig → Flags.java; todos false salvo `letter_fast_scroller`, ver 0023), `themetokens/res` (valores y drawables generados por `tools/apply-theme-tokens.py`).

Piezas de AOSP no importadas o excluidas: `quickstep/` (recientes/gestos), `viewcapturelib`, `displaylib`, `mechanics`, `cuebarlib`,
`contextualeducationlib`, `iconloaderlib/src_full_lib`, pruebas.
