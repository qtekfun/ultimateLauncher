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

## Tablet Huawei MatePad MRO-W09 (serie USB <SERIE_TABLET>) — autorizada por el usuario el 2026-10-05
- Dispositivo: Android 12 (API 31), EMUI 14.2.0 (`MRO-W09 4.2.0.192(C432E2R1P1)`), 2800×1840 físico (modo vertical 1840×2800), 360 dpi, hasta 144 Hz (120 por defecto), **sin GMS** (0 paquetes `gms`), un solo usuario.
- Launcher original (VÍA DE VUELTA): `com.huawei.android.launcher/.unihome.UniHomeLauncher`. Restaurar: `tools/restore.sh <SERIE_TABLET> com.huawei.android.launcher` (o `adb -s <SERIE_TABLET> shell cmd role add-role-holder --user 0 android.app.role.HOME com.huawei.android.launcher`; si el rol no existe en EMUI: Ajustes > Apps > Apps predeterminadas > App de inicio).
- Reglas: solo instalar la app, fijarla como launcher y leer logs/capturas.

### Resultado en la tablet Huawei MRO-W09 (Android 12, sin GMS) — 2026-10-05 07:20
- 4 fallos de compatibilidad encontrados y corregidos, uno tras otro: (1) `Can't convert to ComplexColor`: los colores `@android:color/system_*` de API 34 no existen en Android 12 → parche 0015 (alternativa con la paleta de API 31); (2) `NoSuchMethodError Stream.toList()` (API 34) → *core library desugaring*; (3) y (4) `NullPointerException` en `AppWidgetHostView` desde `OseWidgetView` (widget de búsqueda sin GMS, en pantalla grande) → parche 0016.
- Con la build final: arranca sin FATAL, inicio en landscape (2800×1840) con dock de 5 iconos (Calendario, Galería, AppGallery, Brave, Cámara), indicador de páginas, cajón con 30+ apps y buscador. Primer arranque: el asistente aparece.
- **NO fijado como launcher predeterminado en la tablet** (el usuario tuvo que desconectarla): se probó lanzando la actividad. Launcher activo al terminar: el original (`com.huawei.android.launcher`). Gestos de EMUI/recientes, rotación, multiventana, widgets y reinicio: NO probados. Rejilla de tablet: se usa `ultimate_phone` solo en móvil; en tablet cae a las rejillas de AOSP (no hay tokens de tablet).
- Aviso de Huawei al instalar (`AppGallery InstallDistActivity`, comprobación de riesgo): no se tocó.

## Referencia Huawei MatePad (tablet) — 2026-10-05
- Capturas y volcados: `private-measurements/huawei/` (home, página 2, recorte del dock, gestos). Medidas en `assets/themes/huawei-tablet-medido.json`.
- Dock de Huawei: dos píldoras (6 apps fijas | asa arrastrable | últimas usadas, ahora 2) de 194 px de alto con iconos de 138 px; rejilla 7×5 con celdas de 322×271 px; iconos sin fondo propio (engranajes de Ajustes, terminal, etc.) se rellenan con una baldosa clara.
- Petición del usuario: dock de tablet «como Huawei» con zona fija + zona de últimos usados (3–4) e iconos rellenos como los originales.

## Tablet Huawei: iconos y dock (2026-10-05, en curso)
- Hecho y verificado en la tablet: rejilla 7×5 de tokens (parche por tipo de dispositivo), máscara de icono = superelipse n≈5 medida en Huawei (parche 0018), bandera `enable_launcher_icon_shapes` activada (el launcher recorta con su propia forma), iconos heredados con fondo propio llenan la baldosa con el color de su borde (0017b/c, sin marco negro), iconos adaptativos sin fondo (engranajes) se rellenan con baldosa clara (0017), la forma se aplica también en el cajón (0019).
- Dock estilo Huawei (parche 0040, `app/.../dock/UlDockView.kt` y `RecentApps.kt`): primera versión instalada: dibuja la píldora de las apps fijas. PENDIENTE: celdas del hotseat de 210 px en vez de 175 (el relleno del hotseat no se aplica como se esperaba), icono de 5 apps en una píldora de 6 huecos, comprobar la zona de recientes (no se llegaron a abrir apps) y el centrado del conjunto. Captura: `private-measurements/t-final.png` (ignorada por git).
- Ajustes del dock (parche 0041, compilado, SIN probar en dispositivo): Ajustes del launcher → «Fondo del dock de tablet» (Píldoras / Sutil / Ninguno) y «Últimas apps en el dock» (al desactivar se borran y no se registran). Pedido del usuario: la estética del dock debe ser configurable (no está seguro de querer el sombreado).
- Capturas actuales de la tablet: `private-measurements/t-final.png`.
## Compatibilidad Android 12–14 (agente) — 2026-10-05
Sin dispositivo (adb y emulador no usados). Detalle y tabla completa: `docs/compat-android12-14.md`.
- Parche 0011 aplicado; nuevos 0030 (retroceso del selector de widgets y `AddItemActivity` sin tipos de API 34), 0031 (`LinkedHashSet` API 35), 0032 (colores `@color/system_*` de Launcher3 que solo existían en `values-v34`: habrían dado `Resources.NotFoundException` en 12–13), 0033 (receptores no exportados en API ≥ 33).
- Lint `:app:lintDefaultDebug`: `NewApi` 33 → 0 y la tarea pasa limpia (configuración en `app/build.gradle`, sin línea base; `NewApi` error). Lint sin los `@SuppressWarnings("NewApi")` de AOSP: solo archivado (tras flag, inalcanzable) y retroceso (D8 incluye stubs de las clases de API 34).
- Pruebas: 16 en verde (nueva `ResourceFallbackTest`: ningún recurso existe solo en `-vNN` > 31). `assembleDefaultDebug` y `assembleSyncDebug` compilan; `tools/check-permissions.sh` OK (sin INTERNET).
- Recomendación: mantener `minSdk` 31 (la tablet es Android 12; subir a 33 apenas ahorra; 34 sí ahorraría pero excluye 12–13).
- SIN verificar en dispositivo: todo lo anterior salvo lo que ya probó la tablet (0015, 0016, desugaring). Lista de pruebas pendientes en la tablet: al final de `docs/compat-android12-14.md`.
## Estética cajón (agente) — 2026-10-05 (CPH2841, medido contra private-measurements/drawer.png y home.png)
Parches 0020–0025 (ver `patches/README.md`); todos los valores salen de `assets/themes/oppo-medido.json` vía `tools/apply-theme-tokens.py`.
- **Cajón a pantalla completa**: hoja desde y=0 sin esquinas ni asa; fondo = desenfoque de ventana (`blur behind`, 26 dp, apagado/encendido con el progreso del cajón; la base no enlazaba el estado de desenfoque, se consulta `isCrossWindowBlurEnabled()`) y velo negro de alfa 0,33 (medido: la media del borde del cajón es 0,65–0,68 de la del escritorio). Sin desenfoque: velo 0,6 (elección, no medida). Lista plana, sin categorías ni previstas; búsqueda local intacta («cal» devuelve Calendar y las dos Calculadoras).
- **Columnas**: centros a x=185, 451, 717, 983, 1249 px = referencia (paso 266 px); primera fila con el icono a y=495 (OPPO 493); paso entre filas 400 px.
- **Buscador abajo**: píldora en y=2914–3070, x desde 64 (OPPO 2914–3070, x desde 63), relleno blanco 12 %. Diferencia: OPPO deja un hueco a la derecha (botón redondo de colores); aquí la píldora es simétrica (1376 px de borde derecho vs 1152). Con el teclado, la píldora sube (parche 0025). La lista se recorta sobre la píldora (OPPO deja ver apenas el borde de la fila siguiente).
- **Barra A–Z**: 24 letras (las presentes), paso 48 px (OPPO 48), de y=998 a 2130 (OPPO 998–2129), x=1387–1424 (OPPO 1386–1424), altura de mayúscula 29 px (OPPO 28), color gris 210 (OPPO 205). Siempre visible; el pulgar solo aparece al arrastrar. Se activó el flag `letter_fast_scroller` y el contenedor de letras pasó de ConstraintLayout a FrameLayout (con ConstraintLayout las letras salían de 0×0).
- **Indicador con una página**: punto redondo de 22 px en y=2578–2599 (OPPO 23 px, 2577–2600), desplazado +23 px como en OPPO (centro x=743).
- **Etiquetas**: «Calendar» mide 180×34 px aquí y 181×34 px en OPPO a 14,4 sp: sin diferencia medible, no se cambia `iconTextSize`. Peso y sombra no se miden con fiabilidad.
- **Dock**: OPPO no pinta panel ni velo (iconos sobre el fondo); aquí tampoco: sin cambios. Guardado en el JSON.
- **Carpeta abierta**: referencia medida en `folder.png` (sin tarjeta; fondo desenfocado a pantalla completa; título a y=782, fila de iconos a y=1084, paso 398 px). **NO aplicado**: sigue la tarjeta de AOSP; hacerlo exige una carpeta real para probar y no se crea una en el teléfono del usuario.
- Verificación en el teléfono antes de commit: sin `FATAL EXCEPTION`; inicio, cajón, búsqueda, menú de pulsación larga y selector de widgets abren. No probados: carpeta, tema claro, horizontal, tablet, modo ahorro de batería (reserva sin desenfoque).


## Integración de los subagentes y modo tablet en el OPPO (2026-10-05)
- Ramas integradas en `master`: compatibilidad Android 12–14 (parches 0011, 0030–0033; Lint NewApi = 0, 16 pruebas) y estética del cajón OPPO (parches 0020–0025: cajón a pantalla completa con desenfoque, buscador abajo, barra A–Z, punto de una página). `assembleDefaultDebug`, `testDefaultDebugUnitTest` y `lintDefaultDebug` pasan tras la mezcla; generadores y parches son idempotentes.
- **El OPPO está en MODO TABLET de prueba** (`tools/emulate-tablet.sh on`: densidad 280 y horizontal; valores originales guardados: densidad 560, rotación automática). Restaurar con `tools/emulate-tablet.sh off`.
- Dock de tablet verificado en el OPPO emulando tablet: dos píldoras centradas (apps fijas | asa | últimas usadas), la píldora izquierda se ajusta al nº de apps, recientes (DevCheck, Brújula) con la forma de icono buena; celdas de 136 px a 280 dpi (=77,8 dp). Ajustes del dock (interruptores «Fondo del dock de tablet», «Fondo sutil», «Últimas apps en el dock») probados: sin fondo / sutil / píldoras. Un `ListPreference` fallaba (tema sin diálogo AndroidX) y se sustituyó por interruptores.
- Parches nuevos: 0040 (dock), 0041 (ajustes), 0042 (espacio del hotseat). Sin verificar en la tablet real todavía (prueba final pendiente). Pendiente conocido: tamaño de icono visible de la tablet (140 px), asa arrastrable sin función, quitar un reciente con pulsación larga.

## Animación de abrir y volver al icono (2026-10-05)
- Petición del usuario: la app debe abrirse desde el icono y volver al icono. LÍMITE de plataforma: la animación de la ventana de la app hacia el icono la ejecuta una transición remota del sistema (`CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS`, permiso de firma), que un launcher normal no puede registrar (ver docs/02). Decisión acordada con el usuario: aproximación propia dentro del launcher.
- Parche 0043 (`app/.../anim/OpenReturnAnim.kt`): apertura con `makeScaleUpAnimation` desde los límites del icono (alternativa: revelado de AOSP) y, al volver (`Launcher.onResume`), el icono lanzado hace un «pop» con rebote (escala 1,32→1, 300 ms). Dos interruptores en Ajustes de inicio. Compilado y sin errores en el OPPO; NO se ha podido comprobar la animación en vídeo (`screenrecord` no puede escribir en este teléfono y las capturas tardan ~200 ms): la valoración es visual por el usuario.
- «Iconos que tardan unos segundos al volver»: NO reproducido en el OPPO en modo móvil (marcador, cámara: iconos presentes desde el primer fotograma, mismo proceso). Posible causa específica de la tablet; falta registro (logcat) de la tablet al volver de una app.
- Higiene: una ráfaga de capturas mostró brevemente datos personales (contactos/llamadas) al abrir Teléfono; se borró del PC y del teléfono.

## Iconos que desaparecen al volver con el gesto de inicio (2026-10-05) — EN INVESTIGACIÓN
- Informe del usuario: «los iconos se pierden si le das a inicio, o sea gesto hacia arriba». No reproducido en el OPPO (móvil): tecla Inicio, gesto desde app, gesto con el cajón abierto, gesto en el inicio y arrastre lento muestran los iconos desde el primer fotograma.
- Parche 0044 (solo debug, etiqueta logcat `ULDIAG`): registra onStart/onStop/onPause/onResume/onNewIntent/onTrimMemory y el estado de los iconos a +0/+0,5/+1,5/+4 s. Dato del OPPO con el gesto: se reciben DOS pares onNewIntent+onResume seguidos, `focus=false` hasta ≈1,5 s, `onTrimMemory=20` al ir a segundo plano; iconos=6, sinDrawable=0, noVisibles=0, alfa=1 (están bien).
- Pendiente: repetir en el dispositivo del fallo (¿la tablet Huawei?) y leer `adb logcat -s ULDIAG`. Hipótesis: ventana sin foco/transparente durante la animación de inicio de EMUI, o repintado de iconos tras `onTrimMemory`.

### Causa hallada: arranque en frío lento de la build debug (2026-10-05)
- Reproducido en el OPPO: si ColorOS mata el proceso del launcher mientras se usa otra app, el gesto de inicio provoca un arranque en frío y el registro muestra `iconos=0` al volver, con los iconos a ≈1,5 s en la build debug.
- La build debug es `debuggable` y queda en estado `status=verify` (sin compilar a nativo): `cmd package compile` no tiene efecto. Medido con el tiempo «Displayed» de Android (proceso muerto, gesto de inicio): **debug 316–352 ms, release compilada (`status=speed`) 156–157 ms** (≈2,2× más rápido).
- Medidas: nueva variante release firmada con la clave de depuración local (`app/build.gradle`, sin minificar), `dist/ultimatelauncher-default-release.apk`, y `tools/install.sh` instala la release y ejecuta `cmd package compile -m speed -f` (la debug sigue para diagnóstico `ULDIAG`). El OPPO tiene ahora la release compilada.
- Sigue en pie: que ColorOS mate el proceso (ajustes de batería/autoarranque del asistente) y que el sistema tarde ≈1,5 s en dar foco a la ventana tras el gesto. Pendiente: perfil de arranque (baseline profile) para que el AOT venga ya en el APK, y minificación R8.
- Herramientas: `tools/measure-cold-return.sh` quedó descartada (capturas por wifi/`date` del teléfono no fiables); la medición válida es el tiempo «Displayed» de logcat.

### Exclusión de la optimización de batería (2026-10-05)
- Petición del usuario (como en UltimateDeck): evitar que ColorOS mate el launcher. Se añade el permiso `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (documentado en docs/09 y README) y el asistente (`FirstRunActivity`, icono «Configuración de UltimateLauncher» del cajón) pide el diálogo directo del sistema, muestra si está concedido y, si la ROM no lo tuviera, abre la lista/detalles de la app.
- OPPO CPH2841: antes «ACTIVA»; se aceptó el diálogo estándar («¿Permitir que la aplicación se ejecute siempre en segundo plano?») → ahora `deviceidle whitelist` contiene `user,com.qtekfun.ultimatelauncher`, `RUN_ANY_IN_BACKGROUND: allow`, bucket de espera 5 (exento), el asistente muestra «DESACTIVADA».
- Límite: la exención evita Doze/optimización de batería pero ColorOS aún puede cerrar procesos por falta de memoria; por eso se mantiene también la build release compilada (arranque en frío 2× más rápido). Pendiente de comprobar en uso real.

### Diagnóstico con el gesto REAL del usuario (2026-10-05 10:03) — causa del «se pierden los iconos»
- Con la build debug y `ULDIAG`, dos gestos reales dieron lo mismo: proceso vivo (exento de batería), `onNewIntent`+`onResume` dos veces seguidas, **`focus=false` ≈0,9 s después de `onResume`**, y en todas las muestras (0–6 s) 6 iconos, todos visibles, opacidad 1, con imagen y modelo cargado. El launcher NO pierde los iconos: es la ventana la que no se muestra hasta que el sistema termina la transición del gesto.
- Mecanismo: en ColorOS la animación del gesto de inicio y la multitarea las ejecuta el launcher de OPPO (`com.android.launcher`, proveedor de recientes). Con un launcher de terceros por defecto, el sistema no lo pone visible debajo del cierre de la app: durante el cierre se ve solo el fondo de pantalla (`windowShowWallpaper` + `windowBackground` transparente) y los iconos aparecen cuando la ventana consigue foco. Es el mismo origen que la multitarea lenta. No se puede arreglar sin ser el proveedor de recientes (root/QuickSwitch o app de sistema); ver docs/02.
- Mitigaciones hechas: exclusión de batería (evita además el arranque en frío), build release compilada (arranque en frío ≈2× más rápido). Experimentos posibles (sin garantía): animación de entrada propia del launcher, `windowDisablePreview`.
- Cuando se acabe el diagnóstico se vuelve a dejar la release (hecho).

### Investigación externa y parche 0046 (2026-10-05)
- Es un problema conocido: OnePlus/OPPO/realme desde Android 14 (XDA, comunidad OnePlus, docs de Lawnchair «Gesture navigation issues»): con launcher de terceros y gestos, el cierre de la app ocurre sobre un home en blanco 1–2 s; con botones no pasa. Solo se arregla con root (módulo QuickSwitch, que hace al launcher proveedor de recientes).
- Pista del usuario: con «atrás» los iconos salen al instante y con «inicio» no. Diferencia: «inicio» llega como `onNewIntent` HOME con el contrato `GestureNavContract`; `Launcher.handleGestureContract` muestra `FloatingSurfaceView`, que oculta el icono real hasta que el sistema cierra. Lawnchair recomienda desactivar ese API en OEM que lo rompen.
- Parche 0046: interruptor «Contrato del gesto de inicio», apagado por defecto (se ignora el contrato). Sin verificar en el dispositivo.

### Medición automatizada del gesto y causa raíz (2026-10-05, CPH2841, ColorOS)
- `input swipe` desde el borde inferior dispara el gesto real; ráfagas de `screencap` crudo (~110 ms/captura) en el propio móvil. Resultado (montajes en `private-measurements/gesto/`, no se versionan):
  - Launcher de OPPO como inicio: iconos desde ≈0,25 s.
  - UltimateLauncher: solo fondo de pantalla hasta ≈0,9–1,0 s, igual con el contrato del gesto encendido, apagado o con respuesta inmediata del contrato (probado y descartado, no se versiona).
- Registro del sistema: el gesto lo arranca `com.android.launcher/com.android.quickstep.RecentsActivity` (proveedor de recientes de OPPO, uid 10177), que a los ≈80 ms lanza nuestro HOME dos veces (el primero con el contrato). Nuestra actividad queda `RESUMED` a los ≈76 ms, pero WindowManager aplaza su visibilidad («defer commitVisibility for transition») hasta `finishTransition` de la animación de recientes a los ≈0,97 s, y esa transición la termina el proceso de OPPO. Es decir, el retraso no está en nuestro código: la ventana existe y está lista, el sistema no la muestra hasta que OPPO cierra su animación.
- Con el botón atrás no hay animación de recientes, por eso allí los iconos salen al instante.
- Es el problema conocido de OxygenOS/ColorOS desde Android 14 con cualquier launcher de terceros (XDA, comunidad OnePlus, docs de Lawnchair); solo lo resuelve QuickSwitch con root.

### Carpetas al estilo OPPO (parches 0048 y 0049, 2026-10-05)
- Referencia medida: `private-measurements/folder.png` (assets/themes/oppo-medido.json, «folder»): sin tarjeta, título centrado al 24,7 % de la altura, iconos en fila (celda 114,3 × 113,1 dp) sobre el fondo de pantalla desenfocado, resto del inicio oculto.
- 0048: tarjeta transparente (Folder + FolderAnimationManager), título arriba y blanco, posición centrada, desenfoque de la ventana (`setBackgroundBlurRadius` + `blurBehindRadius`) y fundido del escritorio/dock (`app/.../folder/FolderStyle.kt`). Interruptor «Carpetas abiertas como OPPO» (activo por defecto).
- 0049: celda de carpeta 114,3 × 113,1 dp en teléfono (FolderProfile.kt, variantes escalable y no escalable).
- Comprobado en el CPH2841 con la carpeta existente (2 apps): título a 784 px (ref. 782), paso entre iconos 400 px (ref. 398), apertura y cierre restauran el inicio. Pendiente: fila de iconos ~70 px más arriba que en la referencia (1014 vs 1084), carpetas de 3+ apps y de varias páginas, tablet, tema claro.

### Carpeta: mezcla iOS + ColorOS (parche 0050, 2026-10-05)
- Carpeta cerrada: forma del icono (superelipse/cuadrado redondeado) en vez de círculo y fondo de cristal claro (blanco 40 %) en vez del azul oscuro del tema (`ThemeManager.folderShape = iconShape`, `FolderStyle.closedIconColor`).
- Carpeta abierta: fondo desenfocado de OPPO + oscurecimiento 32 % + panel de cristal traslúcido tipo iOS (blanco 22 %) tras título e iconos, para leer sobre fondos claros. Todo tras el interruptor «Carpetas abiertas como OPPO».

### Icono de carpeta cerrada como OPPO (parche 0051, 2026-10-05)
- Referencia medida en el launcher de OPPO (captura propia, carpeta «Redes sociales», 198 px de lado): rejilla 3x3 en orden de lectura, mini-iconos ≈40 px (20,2 %), paso 54 px (27,3 %), margen 25 px (12,6 %), fondo gris traslúcido (≈ negro/gris 30 % sobre el fondo de pantalla).
- 0051: `ClippedFolderIconLayoutRule` pasa de círculo de 4 a rejilla 3x3 de hasta 9; `FolderStyle.closedIconColor` = gris 30 %.
## M5 — mediciones al perfil y validador (2026-10-05, sin dispositivo)
- `assets/animations/oppo-medido.json` v2: añadidos `source.captureHz` (120), `frameMs` (8,33) y `precisionMs` (±17 ms = 2 fotogramas) y, por evento, `approximate`, `precisionMs` y `basis` (de dónde sale). Rellenado `drawer.close` = 523 ms (aproximado: 5 repeticiones a 0,2 ms entre sí; con AOSP la misma técnica da +11 ms de sesgo). Siguen en null: `drawer.open` (la ráfaga incluye el arrastre inyectado), `app.returnHome` (bimodal, lo anima el sistema), todas las curvas (muelle/bézier) y `home.*`, `icon.*`, `widget.resize` (sin medición).
- Ganchos en `assets/animations/hooks.json` (lo leen `tools/apply-anim-profile.py` y `tools/validate-anim.py`): se aplican en **compilación** como recursos `home.pageSnap`, `drawer.open/close` y `folder.open` (un único recurso rige abrir y cerrar carpeta; `folder.close` comparte). `app.launch` y `app.returnHome` **no tienen gancho** (la duración de abrir una app la fija el sistema; volver a inicio lo anima el launcher de recientes de OPPO).
- `tools/apply-anim-profile.py oppo-medido --aproximadas` aplica también las duraciones aproximadas (cajón cerrar 523, carpeta 877); **no es el valor por defecto de la build** (sigue `aosp-por-defecto`): 877 ms incluye la cola de fotogramas, es ~4× AOSP y no se ha probado a mano en el teléfono.
- `tools/validate-anim.py [perfil] [--medidas ...] [--estricto]`: compara perfil vs mediciones (tolerancia max(17 ms, 5 %)), comprueba que el XML generado corresponde al perfil y lista lo que falta del alcance. Resultado actual: coherente en folder.open/close, drawer.close, app.launch; faltan curvas en los cinco, la duración de `drawer.open` y el gancho de `app.launch`.
## M7 (cifrado y widgets) y M8 (auditoría de privacidad) (2026-10-05)
- M7: `layoutsync/LayoutCrypto.kt` (AES-256-GCM, PBKDF2-HMAC-SHA256 600 000 it., sobre JSON versionado, cabecera autenticada) con 9 pruebas (ida y vuelta, frase errónea, alteraciones, iteraciones rebajadas, versión mayor); la pantalla de exportar/importar pide la frase (vacía = sin cifrar). El importador ahora restaura widgets cuyo proveedor exacto existe (fila con `appWidgetId=-1` y flags de restauración; el enlace lo confirma el usuario en el diálogo del sistema) y omite los demás; ver docs/06. WebDAV NO implementado (DECISIONS.md). Sin probar en dispositivo: pantalla de la frase, restauración real de widgets, importación cifrada entre dos teléfonos.
- M8: `tools/check-permissions.sh` (lista cerrada de permisos, tabla de docs/09, manifiesto, clases de red), `tools/check-dependencies.sh`, `tools/privacy-audit.sh`, `tools/ci.sh`, workflow comentado `.github/workflows/ci.yml`, `docs/privacy-audit.md` (resultado real: superada) y `docs/release-signing.md`. Pendiente: Exodus, prueba de 24 h, build reproducible, R8, clave de publicación.
### M4 estética restante (2026-10-05, CPH2841)
- Carpeta abierta con 4 apps (la existente, 2x2): fila de iconos a y=1082 px (ref. 1084; antes 1014) con un hueco extra de 20 dp entre título y fila (`FolderStyle.styleFolder`). Título a 784 px (ref. 782).
- Tema claro (probado con `cmd uimode night no`, restaurado a `yes`): carpeta cerrada y dock legibles sin cambios; cajón y carpeta abierta tenían etiquetas oscuras sobre el velo oscuro -> ahora blancas (parche 0060 y `FolderStyle.onOpen`). Colores de OEM: ninguno tocado.
- Horizontal: el launcher es vertical fijo en teléfono (AOSP); nuevo interruptor «Girar la pantalla de inicio» (0061, apagado por defecto). Probado girando el OPPO: cuadrícula y dock (vertical a la derecha, de AOSP) correctos, cajón con 5 columnas. Fallos hallados y corregidos: la celda de OPPO no se aplicaba en horizontal (el ancho >600 dp; título oculto por falta de ancho) y la carpeta se salía por abajo (0048/0049: lado corto, filas que caben, posición acotada).
- SIN probar: carpeta con más de 6 apps / varias páginas (no se crearon ni movieron iconos del usuario; solo cálculo: en vertical 4x3 = 12 por página, en horizontal 2x3 = 6 por página); tablet; indicador de páginas del pie en estilo OPPO con varias páginas; pruebas unitarias no ejecutadas; en horizontal el área de la barra A-Z se corta por abajo (sin corregir); cambiar el interruptor de giro requiere reiniciar el launcher.

### R8 por defecto y fondo de la carpeta cerrada (2026-10-05)
- R8 encendido por defecto en la release (`-Pul.minify=false` lo apaga): APK de 10,5 MB; humo funcional en el CPH2841 (docs/arranque-en-frio.md).
- Fondo del icono de carpeta cerrada: medido en el launcher de OPPO, ≈(201,190,172) sobre un fondo (224,209,186), es decir ≈10 % más oscuro, uniforme y con borde nítido. Aquí `0xB3C0B6A6` (70 % opaco, gris cálido) da (201,190,172) sobre ese mismo fondo y oculta casi todo el detalle del papel pintado. Diferencia que queda: OPPO probablemente desenfoca lo que hay detrás; una vista normal no puede desenfocar el fondo de otra ventana.

### Título de la carpeta abierta al estilo iOS (2026-10-05)
- Título 1,35× más grande y en negrita; separación título-iconos reducida (−16 dp sobre el relleno por defecto, ≈ 77 px del texto a la fila de iconos) en lugar de los +20 dp de la referencia de OPPO, que el usuario encontró «fea». `FolderStyle.TITLE_SCALE` y `TITLE_GAP_DP`.
- Ajuste posterior: título 1,35× era desproporcionado frente a los iconos (≈22 sp con etiquetas de 14,4 sp). Ahora 1,1× la etiqueta (≈18 sp) en negrita real; el peso 600 de fuente variable no lo aplica el tipo de letra del sistema de ColorOS, por eso negrita. Regla: jerarquía discreta, título ≈ 1,1–1,25× la etiqueta, mismo cuerpo de letra que los iconos.

### Carpeta abierta idéntica a OPPO (parches 0049b y 0052, 2026-10-05)
- El usuario eligió «idéntica a OPPO» frente a la mezcla con iOS: sin panel, sin velo (dim 0,1), desenfoque fuerte (220 px), título 1,7× la etiqueta (regular), separación título-iconos de la referencia (+20 dp), 3 columnas fijas (4 apps = 3 + 1; parche 0052) y etiquetas de las apps ×0,87 (0049b). Comparación lado a lado con `folder.png`: posición del título, paso y tamaño de iconos coinciden; la etiqueta larga sale más ancha porque «Collabora Office…» tiene nombre largo.
