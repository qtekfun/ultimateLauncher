# Compatibilidad Android 12–14 (R1, M1 T1.2)

Fecha: 2026-10-05. Base: Launcher3 `android17-release`, `minSdk` 31, `compileSdk`/`targetSdk` 37. **Verificación sin dispositivo**: compilación,
Android Lint (`NewApi` como error), pruebas unitarias y lectura de código. La tablet Huawei (Android 12, sin GMS) encontró 4 fallos reales
antes (parches 0015, 0016 y desugaring); el resto de esta tabla **no se ha ejecutado en Android 12–14**.

## Resultado de Lint

| Momento | `NewApi` (error) |
|---|---|
| Informe del 2026-10-05 (M3, antes de los parches 0007/0008/0015/desugaring) | 33 |
| Al empezar este trabajo (con 0007, 0008, 0015, desugaring y 0011 sin aplicar) | 8 (+ 12 nuevos por el parche 0032, detectados y corregidos en el acto) |
| Ahora (`./gradlew :app:lintDefaultDebug`) | **0**, y la tarea pasa limpia |

Además se ejecutó Lint **sin** los `@SuppressWarnings("NewApi")`, `@SuppressLint("NewApi")` y `@TargetApi` de AOSP (copia temporal, no
se conserva): salen 12 avisos más, todos explicados en la tabla (retroceso, archivado). Los demás usos suprimidos de AOSP están bien
resueltos con `Utilities.ATLEAST_*` (anotados con `@ChecksSdkIntAtLeast`).

Configuración en `app/build.gradle` (`lint { ... }`): `NewApi` es error; se desactivan por id los errores heredados de AOSP
(`StringFormatMatches` ×972, `MissingTranslation`, `ExtraTranslation`, `ResAuto`, `MissingClass` de quickstep, `ThreadConstraint`,
`UseAppTint`, `ResourceType`, `AppCompatCustomView`, `RestrictedApi`, `GestureBackNavigation`, `Range`, `WrongConstant`,
`NotificationPermission`, `ProtectedPermissions`, `UnusedResources`). Con eso no queda ningún error y **no hace falta línea base**: un error nuevo
de cualquier otra regla rompe la compilación. Si algún día hiciera falta una línea base: añadir `baseline = file('lint-baseline.xml')` y generarla
con `./gradlew :app:updateLintBaselineDefaultDebug`, comprobando que no contiene `NewApi`.

## Tabla de incompatibilidades

| # | Archivo | API / síntoma en Android 12–14 | Solución | Parche | Estado |
|---|---|---|---|---|---|
| 1 | `widgetpicker/WidgetPickerActivity.kt`, `WidgetPickerComposeWrapperImpl.kt` | `getParcelableExtra/getParcelable(clave, Clase)` (API 33): `NoSuchMethodError` en 12 | `IntentCompat` / `BundleCompat` de androidx.core | 0011 | Aplicado, compila; sin probar en 12 |
| 2 | `WidgetPickerActivity.kt`, `dragndrop/AddItemActivity.kt` | Implementaban `OnBackAnimationCallback` (API 34) y llamaban a `onBackInvokedDispatcher` (API 33) sin compuerta: selector de widgets y «añadir elemento» fallaban al abrir en 12–13 | Se quita la interfaz de API 34; con API ≥ 33 se registra un `OnBackInvokedCallback` (hacía solo `finish()`, mismo comportamiento); con API 31–32 rige `onBackPressed()` + `OnBackPressedDispatcher` de androidx (único, antes se creaba uno nuevo en cada lectura) | 0030 | Aplicado, compila; sin probar en 12–13 |
| 3 | `model/FirstScreenBroadcastHelper.kt` | `LinkedHashSet.addLast/removeLast` (API 35) | `add` / `remove(last())`. Código inalcanzable (desactivado por 0006) | 0031 | Aplicado |
| 4 | `widget/WidgetVisibilityTracker.kt` | `AppWidgetHostView.start/stopVisibilityTracking` (API 37): `NoSuchMethodError` en 16 | Compuerta `SDK_INT >= 37` + `try/catch` | 0007 | Verificado en Android 16 |
| 5 | `res/values-v34/colors.xml`, dynamiccolors | 12 `@android:color/system_*` solo de API 37 | `values-v37` + línea base M3 en el resto | 0008 | Verificado en Android 16 |
| 6 | `dynamiccolors`, `res/values*-v31` | `@android:color/system_*_{light,dark}` de API 34: «Can't convert to ComplexColor», no arrancaba en 12 | Alternativa con la paleta tonal de API 31 | 0015 | **Verificado en la tablet (Android 12)** |
| 7 | `launcher3-base/res/values/colors.xml`, `values-night` (+ `values-v34/colors.xml`) | Colores propios `@color/system_*` (cursor y botón «Añadir» del selector de widgets, carpeta de archivos) definidos **solo** en `values-v34`: `Resources.NotFoundException` en 12–13 al leerlos. Lint no lo detecta (nuevo hallazgo) | `values/ul_system_colors_fallback.xml` generado (122 colores, misma correspondencia que 0015); `values-v34` sigue mandando en 14+ | 0032 | Aplicado, compila; sin probar en 12 |
| 8 | todo el código | `Stream.toList` (API 34) y otras APIs de Java 17+: `NoSuchMethodError` en 12–13 | *core library desugaring* (`desugar_jdk_libs` 2.1.5) | (build) | **Verificado en la tablet** |
| 9 | `qsb/OseWidgetView.kt` | NPE sin proveedor de búsqueda (sin GMS) | No se pinta el widget sin proveedor | 0016 | **Verificado en la tablet** |
| 10 | `util/SimpleBroadcastReceiver.kt` | Con targetSdk ≥ 34, en Android 14+ `registerReceiver` sin `RECEIVER_(NOT_)EXPORTED` lanza `SecurityException` si el filtro no es solo de difusiones del sistema (hoy todos lo son) | Sin indicador explícito y API ≥ 33 → `RECEIVER_NOT_EXPORTED` | 0033 | Preventivo; aplicado |
| 11 | `model/LoaderTask.java`, `AppInfo`, `InstallSessionHelper`, `InstallSessionTracker`, `WorkspaceItemProcessor.kt` | `PackageItemInfo.isArchived`, `SessionInfo.isUnarchival` (API 35): se ocultan con `@SuppressWarnings("NewApi")` de AOSP | **No tocado**: todos los usos van detrás de `Flags.enableSupportForArchiving()`, que en los stubs (`platform-stubs`) es siempre `false` | — | Inalcanzable |
| 12 | `Launcher.java`, `AbstractFloatingView.java`, `util/BackPressHandler.java` | Usan `OnBackAnimationCallback` / `BackEvent` (API 34) como tipos. `Launcher.onBackPressed()` (con `@TargetApi(U)`) llama a `getOnBackAnimationCallback()` en todas las versiones | **No tocado**: D8 incluye *stubs* de esas clases de plataforma (`classes33.dex` del APK trae `android.window.OnBackAnimationCallback`, `OnBackInvokedCallback`, `OnBackInvokedDispatcher`), por lo que cargar las clases no falla en 12–13; `BaseActivity.registerBackDispatcher` ya está tras `ATLEAST_T` | — | Comprobado en el dex; **FALLO REAL hallado en la MatePad (Android 12) el 2026-10-05**: el stub de D8 de `OnBackAnimationCallback` está vacío (sin `onBackInvoked`) y `Launcher.onBackPressed` lanzaba `NoSuchMethodError` al pulsar ATRÁS (con R8). Corregido con el parche 0090 (ruta sin interfaz en API < 34); verificado: ATRÁS con cajón abierto y en inicio sin cierre |
| 13 | `UserCache.kt`, `ModelInitializer.kt`, `StringCacheRepository.kt`, `ActivityContext.java`, `Workspace.java`, `SecondaryDragLayer.java` | 10 avisos `InlinedApi`: constantes que el compilador copia (`ACTION_PROFILE_ADDED/REMOVED` 34, `ACTION_PROFILE_AVAILABLE/UNAVAILABLE` 35, `RECEIVER_EXPORTED` 33, `DRAG_FLAG_GLOBAL_SAME_APPLICATION` 35, `CLASSIFICATION_TWO_FINGER_SWIPE` 34, `SPLASH_SCREEN_STYLE_SOLID_COLOR` 33) | Sin cambios: son valores (cadena o entero) que no rompen. En 12–13 solo no llegan esas acciones de perfil; el perfil de trabajo se sigue siguiendo con `ACTION_MANAGED_PROFILE_*`. El splash se llama tras `ATLEAST_T`; `RECEIVER_EXPORTED` solo en builds de estudio | — | Revisado por lectura |

Otras zonas revisadas por lectura sin encontrar nada que corregir: `LauncherApplication`/`MainProcessInitializer`/`ModelDbController`
(sin llamadas de API > 31), `LoaderTask` (archivado tras flag), `WidgetManagerHelper` y `LauncherAppWidgetHostView` (los métodos > 31 están
tras flags o parches 0007/0016), `UserCache` (ver fila 13), iconloaderlib (`getMonochrome`, API 33, solo se llama tras `IconProvider.ATLEAST_T`;
`MonoIconThemeController` se instancia en todas las versiones pero su constructor no usa API nueva), `AndroidManifest` propio (atributos
conocidos desde API 31). Los 8 `GradleDependency`, etc. son avisos, no compatibilidad.

## Cuántas compuertas hicieron falta

- Compuertas de versión (`SDK_INT >= N`) añadidas en este trabajo: **3** (2 en `registerBackDispatcher` de las actividades, 1 en `SimpleBroadcastReceiver`).
  Anteriores: 1 (0007).
- Equivalentes androidx en vez de compuerta: **2** (`IntentCompat`, `BundleCompat`, parche 0011).
- Sustituciones sin API nueva: **2** (`addLast`, `removeLast`, parche 0031).
- Alternativas de recursos: **3 conjuntos** (0008 API 37, 0015 API 34 en dynamiccolors, 0032 API 34 en launcher3-base: 122 colores generados).
- Desugaring de la biblioteca de Java: 1 ajuste de build. Alternativa de comportamiento sin API: 1 (0016).
- Total: **10 cambios de compatibilidad** (5 anteriores: 0007, 0008, 0015, 0016 y el desugaring; 5 de este trabajo: 0011, 0030, 0031, 0032, 0033).
  De los 4 fallos que tiró la tablet en Android 12, 3 eran de recursos/desugaring/ausencia de GMS y solo los de `NoSuchMethodError` eran de API: las
  compuertas de código son la parte pequeña; la mayor parte del riesgo está en recursos por versión y en el entorno sin GMS.
  Lint no ve los recursos definidos solo en `values-vNN` (fila 7); lo cubre `ResourceFallbackTest`.

## Pruebas añadidas

`app/src/test/.../compat/ResourceFallbackTest.kt` (2 pruebas, JVM): ningún recurso (valores, drawables, layouts, colores...) de `launcher3-base`,
`dynamiccolors`, `iconloaderlib`, perfiles de animación o tokens existe **solo** en una configuración `-vNN` con NN > 31; y cada `color/system_*`
de `values-v34` tiene una definición ≤ 31. Total de pruebas unitarias: 16, en verde.

## Recomendación sobre `minSdk`

**Mantener `minSdk` 31.** Datos:

1. La tablet Huawei MRO-W09 del usuario es Android 12 (API 31, EMUI 14.2): subir a 33 o 34 la deja sin launcher. Es el único dispositivo real sin GMS y es
   un dispositivo objetivo del proyecto (móvil y tablet, sin GMS).
2. El coste real fue pequeño: 10 cambios, 3 compuertas de versión, y la mayor parte del trabajo estuvo en recursos y en ausencia de GMS, que no desaparecen
   al subir el mínimo.
3. Subir a **33** casi no ahorra nada: eliminaría solo las compuertas de las filas 1, 2 (parte) y 10, y los 122 colores de 0032, 0015 y el desugaring siguen
   haciendo falta porque son de API 34. Su único beneficio (iconos monocromos nativos) no justifica dejar fuera Android 12/12L.
4. Subir a **34** sí eliminaría los parches 0015/0032 (colores), el desugaring (`Stream.toList`), 0011 y 0030, pero deja fuera Android 12 y 13, justo el dispositivo
   de pruebas real. Se reconsidera si el parque de dispositivos deja de incluir Android 12–13 (decisión de producto, no técnica).
5. Para que mantener 31 siga siendo barato: Lint `NewApi` como error en CI (ya configurado) y la prueba `ResourceFallbackTest`.

Condición para dar M1 T1.2 por cumplido: ejecutar en la tablet Android 12 la lista de abajo y, para API 33/34, en un dispositivo real (no hay ninguno).

## Qué probar en la tablet (Android 12)

1. Arranque y reinicio del launcher; `adb logcat` filtrando `NoSuchMethodError|NoClassDefFoundError|NotFoundException|FATAL`.
2. **Selector de widgets** (pulsación larga en el fondo > Widgets): abre sin cerrarse; el cursor del buscador y el botón «Añadir» tienen color; ATRÁS (botón o gesto) lo cierra; al buscar con teclado, ATRÁS primero lo oculta/limpia (comportamiento del dispatcher) y luego cierra; arrastrar un widget a la pantalla.
3. Previsualización con `RemoteViews` en el selector (widgets que aportan `previewLayout`/`EXTRA_APPWIDGET_PREVIEW`).
4. **ATRÁS** con: cajón abierto, carpeta abierta, menú de pulsación larga, modo edición del inicio, arrastre en curso, y en pantalla de inicio normal (no debe cerrar el launcher).
5. Añadir un widget con pantalla de configuración y un acceso directo fijado desde otra app (`AddItemActivity`), si hay alguna app que lo ofrezca.
6. Tema oscuro/claro del sistema y fondo claro/oscuro: carpetas, cajón, selector de widgets, popups.
7. Rotación vertical/horizontal con el selector de widgets abierto.
8. Pantalla bloqueada/desbloqueada con el selector abierto (se cierra solo, `ScreenOnTracker`).
9. Cambio de zona horaria/hora y de idioma con el launcher activo (receptores registrados con 0033; en 12 el indicador no se usa).
