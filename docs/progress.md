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
