# Progreso (sesión nocturna 2026-10-04)

## Entorno
- JDK: Temurin 21.0.12 (~/.local/jdks/temurin-21)
- Android SDK: ~/Android/Sdk; platforms android-36 y android-37.0; build-tools 36.0.0; platform-tools 37.0.1; NDK 28/29; cmake 3.22/3.31
- Gradle: sin instalación global; distribución en caché `gradle-9.8.0-bin` (~/.gradle/wrapper)
- Python 3 sin OpenCV/SciPy (se instalará en venv si hace falta)
- Host: Fedora, 14 núcleos, 30 GB RAM

## Dispositivos vistos por adb
| Serie | Modelo | Android | Notas |
|---|---|---|---|
| 897201dc (USB) | OPPO PGEM10 | 16, ColorOS V16.0.0 | **NO se toca** (el usuario lo usa para otra cosa). Por error pulsé HOME y hice 1 captura (borrada) antes de recibir el aviso |
| <ADB_SERIE_OPPO> (wifi) | OPPO CPH2841 | 16, CPH2841_16.0.10.500(EX01), ColorOS V16.1.0 | **DISPOSITIVO DE TRABAJO** (indicación del usuario). 1440x3168 físico, density override 560 (física 640), hasta 144 Hz (120 por defecto), GMS presente. Tiene usuario 999 MultiApp: instalar solo con --user 0 |
| <SERIE_PIXEL> (wifi) | Pixel 8 | 17 | NO se toca |

## Launcher de sistema original (VÍA DE VUELTA)
- Paquete: `com.android.launcher` (OplusLauncher, /system_ext/priv-app/OplusLauncher; v16.6.17 en el CPH2841)
- Actividad HOME: `com.android.launcher/.Launcher`
- Titular del rol HOME antes de cambiar nada: `com.android.launcher`
- Restaurar: `tools/restore.sh` o a mano (en el CPH2841):
  `adb -s <ADB_SERIE_OPPO> shell cmd role add-role-holder --user 0 android.app.role.HOME com.android.launcher`
- Escalas de animación: 1.0/1.0/1.0.


## Fase de referencia (hecha 2026-10-04, CPH2841, launcher OPPO activo, tema oscuro)
- Capturas, dumps UI, trazas Perfetto: `private-measurements/` (ignorado por git, contiene datos personales; no se sube).
- `assets/themes/oppo-medido.json`: rejilla 5x7 (inicio), dock 5, icono ≈57 dp, celda 77,7x86,9 dp, dock 89 dp, cajón 5 col, buscador del cajón abajo, carpeta 3 col. Solo móvil vertical y tema oscuro. Radios, etiqueta, blur/scrim: null.
- `assets/animations/oppo-medido.json`: Perfetto SÍ funciona sin root (shell, perfetto v49): frametimeline, SF layers, transiciones WM shell, atrace. Pero cajón/carpetas se animan dentro de la ventana del Launcher, así que SF no da geometría. Duraciones solo por ráfagas de fotogramas = APROXIMADAS (carpeta abrir ≈877 ms, cerrar ≈823 ms, lanzar app ≈588 ms; frecuencia de pantalla 120 Hz). Muelles y bézier: null (no hay seguimiento por vídeo).
- No se hizo grabación de vídeo (no necesaria para lo anterior; queda pendiente para ajustar curvas).

## M0 — HECHO (2026-10-04 ~23:08)
- Andamiaje Gradle propio (AGP 9.4.1, Kotlin 2.4.20, KSP, protobuf-lite) sobre Launcher3 `android17-release` (no-quickstep).
- `./gradlew :app:assembleDefaultDebug` → `app/build/outputs/apk/default/debug/app-default-debug.apk` (copia en `dist/`).
- Permisos del APK (apkanalyzer): BIND_APPWIDGET, REQUEST_DELETE_PACKAGES, VIBRATE, SET_WALLPAPER, SET_WALLPAPER_HINTS. **Sin INTERNET.**
- Variante `sync` declarada (applicationIdSuffix `.sync`, INTERNET en su manifiesto); no se ha compilado ni hay código WebDAV.
- Comprobación en CI: no hay CI en el repo; el comando de `docs/09` (apkanalyzer) se ejecutó a mano.
- Tiempo: M0 llevó ~1 h 15 min de compilación incremental; no hizo falta el plan B.

## M1 — HECHO en el CPH2841 (Android 16, API 36) (~23:11)
- Instalado con `tools/install.sh`; rol HOME = `com.qtekfun.ultimatelauncher` (antes `com.android.launcher`). Restaurar: `tools/restore.sh`.
- Evidencia: `dumpsys window` muestra el foco en `com.qtekfun.ultimatelauncher/com.android.launcher3.Launcher`; captura de inicio y de cajón; logcat sin FATAL/ANR tras arrancar, abrir cajón, buscar y volver con HOME.
- Hallazgos al arrancar (corregidos): (1) falta de SET_WALLPAPER_HINTS → SecurityException; (2) NPE en `FirstScreenBroadcastHelper` (parche 0006);
  (3) sin QUERY_ALL_PACKAGES solo aparecían 5 apps → se añadió `<queries>` de LAUNCHER (36 de ~80 visibles en el volcado; lista completa por scroll).
- NO probado: Android 12 (API 31) ni 14; ningún dispositivo real sin GMS (el CPH2841 tiene GMS; el APK no depende de GMS, pero no se probó sin él); emulador; reinicio del teléfono; 12 h en segundo plano.
- T1.4 (tabla de `02`): la rama trae QSB controlado por `BuildConfig.QSB_ON_FIRST_SCREEN`; no hay fila de previstas ni feed sin quickstep; el cajón es plano. Ver `docs/02` (nota añadida).
- Punto de decisión minSdk: se compiló con minSdk 31 pero **sin** haber probado en API 31; se mantiene 31 sin evidencia de compuertas (Lint NewApi no se ha ejecutado). Pendiente.

## M3 — HECHO en lo comprobable (CPH2841, Android 16) (~23:30)
- Widget con pantalla de configuración: **AntennaPod → PlayerWidget** (`WidgetConfigActivity`). Flujo real: Menú de pulsación larga → Widgets (selector Compose de Android 17) → arrastrar → diálogo de sistema "¿Crear widget y permitir acceso?" (se aceptó SIN marcar "permitir siempre") → se abre `de.danoeh.antennapod/.ui.widget.WidgetConfigActivity` → "Crear widget" → el widget queda en `dumpsys appwidget` con host `com.qtekfun.ultimatelauncher` (hostId 1024). Sin FATAL en logcat.
- También se añadió UltimateNotes (sin configuración): se dibuja.
- Ruta de cancelación: DevCheck (`DashWidgetConfigureActivity`, exige Pro) → ATRÁS → el widget no queda registrado; el launcher sigue vivo.
- Hallazgos: (1) falta del `intent-filter` PICK del selector de widgets → ActivityNotFoundException; (2) `AppWidgetHostView.stopVisibilityTracking()` es API de Android 17 → NoSuchMethodError en Android 16 (parche 0007);
  (3) 12 colores `@android:color/system_*` solo existen desde API 37 (parche 0008, detectado con Lint NewApi).
- Lint (`:app:lintDefaultDebug`): 33 errores NewApi (API 33: back-invoked, getParcelableExtra; API 34: Stream.toList, OnBackAnimationCallback; API 35: addLast/removeLast; colores 34/37), más 972 `StringFormatMatches` heredados de traducciones de AOSP. NO están corregidos los de API 33–35: en Android 12–13 fallarían.
- NO probado: redimensionar y mover widgets; perfil de trabajo (el CPH2841 solo tiene el usuario 999 "MultiApp", no un perfil de trabajo gestionado; sin emulador no hay forma de comprobarlo); widgets de otras marcas; tablet.
- Nota: tras las pruebas quedan 2 widgets de prueba en la pantalla de inicio (se limpiarán al final con `pm clear` del propio launcher).

## M4 — PARCIAL (CPH2841) (~23:45)
- `tools/apply-theme-tokens.py` lee `assets/themes/oppo-medido.json` (clase phone) y genera la rejilla `ultimate_phone` (5 columnas × 7 filas, dock de 5, carpeta de 3 columnas, icono 57,1 dp, celda del cajón 114 dp) en `device_profiles.xml`; parche 0009 la hace rejilla por defecto. Los tokens `null` no se aplican.
- Comprobado con `uiautomator dump`: columnas de 276 px (referencia 272), iconos en 5 columnas. Diferencias que SIGUEN: altura de celda de inicio 334 px (referencia 304), dock 225 px de alto a y=2621 (referencia 312 px a y=2752), márgenes laterales 30 px (referencia 40). Los paddings/dock los calcula AOSP desde `spec_handheld_*.xml`/`paddings_*.xml` y no se han tocado.
- NO aplicado (token null o sin gancho): radios de esquina, tamaño de etiqueta, blur/scrim del dock, cajón y carpeta, estilo del indicador de páginas, clases phone-landscape/tablet.
- No hay carga en tiempo de ejecución de `ThemeTokens` (es en compilación): decisión para que sea fácil de revertir y sin código nuevo en AOSP.
- No se hizo la comparación lado a lado ni el criterio "indistinguible a simple vista": NO cumplido.

## Variante sync
- `:app:assembleSyncDebug` compila; `applicationId` `com.qtekfun.ultimatelauncher.sync`, declara INTERNET. No incluye código WebDAV (M7 T7.4 no hecho).
- `tools/check-permissions.sh` implementa el control de docs/09 para la variante default.

## M5 — PARCIAL (CPH2841) (~00:05)
- T5.1 hecho: `docs/anim-inventory.md` (generado por `tools/anim-inventory.py`): 11 recursos enteros redirigibles + 129 constantes en código (no redirigidas) + interpoladores/muelles (sin redirigir).
- T5.2 parcial: `tools/apply-anim-profile.py <perfil>` genera `animprofile/res/values/ul_animation_profile.xml`, que ambas variantes superponen a `config.xml` de AOSP. Redirige 4 eventos: `home.pageSnap`, `drawer.open`, `drawer.close`, `folder.open/close`. NO hay carga de perfil importado en ejecución ni multiplicador cambiable por el usuario (es en compilación).
- T5.3: perfiles `aosp-por-defecto` (valores de la rama) y `rapido` (extiende `oppo-medido`, ×0,7) en `assets/animations/`. `oppo-medido` aporta solo valores medidos no aproximados: hoy ninguno → equivale a AOSP.
- Verificación en el dispositivo (Perfetto, ráfagas del launcher, 5 repeticiones): cerrar cajón 311 ms con AOSP (300) → 222 ms con `rapido` (210). La apertura por gesto (≈520 ms) no cambia: la dirige la velocidad del dedo, no el recurso. Carpeta y `pageSnap` no se midieron tras el cambio.
- T5.4/T5.5 NO hechos (curvas medidas, validación lado a lado). Criterio de aceptación de M5 NO cumplido.
- Build final entregada con `aosp-por-defecto`.

## M6 — PARCIAL (mínimo) (~00:20)
- `OemAdapter` (+ `HelpText`, `KnownIssue`, `DeviceInfo`), `GenericAdapter`, `ColorOsAdapter`, registro `OemAdapters`, rutas en `assets/oem-intents.json` (con campo `verified`), `FirstRunActivity` (asistente sin Compose, ES/EN) y gancho de una línea en `Launcher.onCreate` (parche 0010, se muestra una vez; se repite desde el icono «Configuración de UltimateLauncher» del cajón).
- Pruebas unitarias: 6 pruebas JUnit (selección de adaptador) en verde (`:app:testDefaultDebugUnitTest`).
- En el dispositivo: el asistente arranca en el primer inicio, detecta `coloros`, el botón de batería abre ajustes de batería, el de autoarranque cae a «detalles de la app», «Hecho» vuelve al inicio.
- NO hecho: `VivoAdapter`, `HyperOsAdapter`, `MagicOsAdapter` (solo hay rutas candidatas sin verificar en el JSON); detección de «fallos conocidos» (RF-41) más allá de texto estático; ruta de autoarranque de ColorOS 16 sin resolver (ver `docs/oem-issues.md`).

## M7 — PARCIAL (T7.1, T7.2, T7.3, T7.5 en lo esencial) (~00:50)
- `app/.../layoutsync/`: modelo del esquema v1 (`LayoutModel.kt`, JSON, rechaza `schema` mayor), `GridReflow` (conserva posiciones si todo cabe; si no, reubica por orden de lectura repartiendo páginas y ajustando spans de widgets), `ImportPlanner` (apps no instaladas, perfil de trabajo, widgets de otra marca, desborde del dock, cambio móvil↔tablet; resumen previo), `LayoutStore` (lee/escribe la tabla `favorites` vía `ModelDbController` y fuerza recarga; copia automática en `files/backups/` antes de importar), `LayoutSyncActivity` (selector de documentos del sistema, sin red; icono «Disposición de UltimateLauncher» en el cajón).
- Pruebas unitarias: 8 de layout-sync (ida y vuelta JSON, esquema mayor rechazado, 4×6→5×6, 5×6→5×4 con 24 apps, widget 6×2 en 4 columnas, apps/trabajo/otra marca, dock desbordado) + 6 de OEM: 14/14 en verde.
- En el dispositivo (CPH2841): exportar con el selector → archivo válido (8 elementos); importar un archivo con una app no instalada, un elemento de trabajo y un widget de otra marca → resumen correcto antes de aplicar; importar un archivo que mueve Gmail → Gmail aparece en la celda indicada (columna 5, fila 4) y se crea la copia en `files/backups/`.
- NO hecho: variante `sync`/WebDAV (T7.4), cifrado del archivo (el aviso en pantalla lo dice), exportar ajustes más allá de la rejilla (forma de icono, pack, perfil de animación se escriben con valores fijos), restaurar widgets (se listan en el resumen; añadirlos exige el permiso del sistema), accesos directos (se omiten), perfil de trabajo, importación móvil↔tablet probada solo con pruebas unitarias, importar en un segundo dispositivo de otra marca (no hay).
- Limpieza: los archivos de prueba se borraron de `/sdcard/Download`.

## Estado de pruebas de compatibilidad (~01:00)
- Android 12 (API 31) y 14 (API 34) en emulador: **NO probado**. Se instalaron el emulador 37.2.12 y las imágenes AOSP `default` (sin GMS) de API 31 y 34 desde sdkmanager y se crearon los AVD `ul31` y `ul34`, pero el emulador termina con SIGSEGV («Violación de segmento») poco después de «full startup» en este host con `-gpu swiftshader_indirect`, `guest` y `off`. `tools/emu-test.sh` queda escrito pero no se ha podido ejecutar. Hay volcados borrados (`core.*`).
- Consecuencia: M1 NO cumple su aceptación en Android 12; tampoco el «dispositivo sin GMS». Conocido por lectura de código/Lint (sin ejecutar): en API 31–33 el selector de widgets (`WidgetPickerActivity`) y `AddItemActivity` implementan `OnBackAnimationCallback` (API 34) → `NoClassDefFoundError` al abrirlos; `getParcelable(Extra)` con clase (API 33) falla en API 31–32 (parche 0011 preparado y sin aplicar); `Stream.toList` (API 34) pide desugaring.
- ~00:45 se perdió la conexión adb inalámbrica con el CPH2841 (la depuración inalámbrica dejó de anunciarse; también desapareció el PGEM10 por USB). La última build verificada en el teléfono es la de M7; la que hay en `dist/` es la misma fuente (parche 0011 sin aplicar), recompilada tras la pérdida de conexión: **no se instaló ni se probó esa copia**.

## M4 — ajuste fino tras la revisión del usuario (2026-10-05) (CPH2841, medido con `tools/measure-home.sh`)
Quejas: «el dock está levantado, los iconos se ven pequeños». Causas y correcciones (todo generado desde `assets/themes/oppo-medido.json`):
- Dock levantado: el hotseat reservaba ≈64 dp para una barra de búsqueda inexistente → parche 0012. Icono del dock: y=2805–3004 px (referencia OPPO 2808–3007).
- Iconos pequeños: con `iconImageSize`=57,1 dp el icono visible medía 183 px (ratio 0,915) → se compensa en el generador (62,4 dp). Resultado: **200 px** como OPPO. Cajón: ratio 0,845 → 67,6 dp → icono de 200 px (OPPO 198) y paso entre filas de 400 px.
- Área de iconos del inicio: filas de 304 px de 2122 a 2426 (idénticas a la referencia); márgenes laterales 11,0 dp; punto del indicador en y=2588 (OPPO 2588) y visible con una sola página.
- Forma del icono: ahora se dibuja con el trazado real de la máscara del sistema; diferencia de contorno ≤ 4 px sobre 200 en la esquina.
- SIGUE distinto: cajón (OPPO: pantalla completa con fondo desenfocado, pestañas Todos/Categorías y barra A–Z; aquí: hoja inferior oscura con buscador arriba), punto del indicador (aquí una píldora), tipografía/peso de etiquetas, fondo/blur del dock y carpetas, tema claro, horizontal y tablet.
