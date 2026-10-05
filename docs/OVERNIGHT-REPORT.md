# Informe de la noche (2026-10-04)

Estado: **UltimateLauncher compila, está instalado en el OPPO CPH2841 y fijado como launcher predeterminado.** Funciona en lo probado (inicio, cajón, búsqueda, widgets con configuración, asistente de primer arranque, exportar/importar). Hay límites claros abajo. **Aviso:** a ~00:45 se perdió la conexión adb inalámbrica con el CPH2841 (la depuración inalámbrica dejó de anunciarse y el PGEM10 por USB tampoco aparece). Lo que hay instalado en el teléfono es la **última build verificada** (hito M7); `dist/ultimatelauncher-default-debug.apk` es la misma fuente recompilada después, sin instalar ni probar en el teléfono. Si algo no coincide, recompila con `./gradlew :app:assembleDefaultDebug`.

## Después de la noche (2026-10-05): actualización

El resto del documento describe el estado de la noche del 4 de octubre; sigue siendo correcto salvo lo que se corrige aquí. Detalle y evidencias en `docs/progress.md`; lo marcado «sin verificar» no se ha visto funcionar en un dispositivo.

**Se recuperó la conexión** con el CPH2841 (el PGEM10 sigue sin usarse) y el usuario autorizó la tablet Huawei MatePad MRO-W09 (Android 12, EMUI 14.2, sin GMS). Los emuladores se borraron (el emulador fallaba con SIGSEGV).

| Área | Qué se hizo | Estado real |
|---|---|---|
| Tablet (Huawei) | Primera prueba en Android 12 sin GMS: 4 fallos de compatibilidad corregidos (parches 0015, 0016, desugaring). Rejilla 7×5, máscara de icono medida (superelipse, 0018/0019), relleno de iconos sin fondo (0017). | Arranca y funciona en lo probado. **No** se fijó como launcher en la tablet; gestos EMUI, rotación, multiventana, widgets y reinicio sin probar. |
| Dock de tablet | Dos píldoras (fijas, asa, recientes), celdas y centrado, ajustes de fondo y de últimas apps (0040–0042). | Verificado emulando tablet en el OPPO (densidad 280, horizontal; ya restaurado). **Sin verificar en la tablet real**; el asa no hace nada y falta quitar un reciente con pulsación larga. |
| Iconos y cajón (OPPO) | Dock sin hueco de búsqueda, iconos a 200 px como OPPO, márgenes, punto del indicador (0012–0014, 0024); cajón a pantalla completa con desenfoque, buscador abajo, barra A–Z (0020–0025). | Medido contra capturas: diferencias de 1–4 px. Siguen distintos: tipografía/peso de etiquetas, tema claro, horizontal; el buscador no deja el hueco del botón redondo de OPPO. |
| Compatibilidad Android 12–14 | Parches 0011, 0030–0033; `lintDefaultDebug` pasa con `NewApi` = 0; 16 pruebas unitarias. | Por lectura y lint, más lo que ejerció la tablet (API 31). **API 33/34 no probadas.** Se recomienda mantener `minSdk` 31 (`docs/compat-android12-14.md`). |
| Carpetas | Estilo OPPO (sin tarjeta, título arriba, fondo desenfocado, celda medida, 0048–0049) y mezcla con panel de cristal tipo iOS (0050), tras un interruptor. | Probado en el OPPO con una carpeta de 2 apps; la fila de iconos queda ~70 px más arriba que la referencia. Carpetas de 3+ apps, de varias páginas, tablet y tema claro sin probar. |
| Animación de abrir y volver | Apertura con escala desde el icono y «pop» al volver (0043), con ajustes. | Compila y no da errores; **no se pudo ver la animación** (sin vídeo). No es la animación de OPPO: la de ventana es del sistema (`docs/02`). |
| Diagnóstico del gesto de inicio | «Se pierden los iconos al dar a inicio»: parches 0044 (registro debug), 0045 (sin animaciones de ventana) y 0046 (contrato del gesto apagado). Medición automatizada con ráfagas de capturas. | **Causa raíz hallada y fuera de nuestro alcance:** el gesto lo anima el launcher de OPPO; WindowManager aplaza la visibilidad de nuestra ventana hasta ≈0,97 s. Con OPPO como inicio los iconos salen a 0,25 s, con el nuestro a ≈0,9–1,0 s, con el contrato encendido, apagado o respondido al momento. Solo lo arregla root (QuickSwitch). Los iconos no se pierden: la ventana no se muestra. Con «atrás» sale al instante. |
| Arranque en frío y batería | Release firmada en local y compilada a nativo (`tools/install.sh`): ≈2,2× más rápido que debug (156 ms frente a 316–352 ms «Displayed»). Permiso `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` y paso en el asistente (OPPO ya exento), recordatorio cada 3 días. | Medido en el OPPO. Que ColorOS mate el proceso por memoria no se evita; falta uso real. R8 y perfil de línea base: ver abajo. |
| Adaptadores OEM | `VivoAdapter`, `HyperOsAdapter` y adaptadores de Huawei/Honor (`OtherAdapters.kt`), rutas en `oem-intents.json`, pruebas de selección. | **Rutas candidatas sin verificar en vivo** (solo hay ColorOS y EMUI a mano); la ruta de autoarranque de ColorOS 16 sigue sin resolver. |
| M5 animaciones | `oppo-medido` v2 con precisión (±17 ms a 120 Hz) y `drawer.close` = 523 ms aproximado; `hooks.json`; `--aproximadas`; `tools/validate-anim.py`. | Las curvas (muelle/bézier) siguen en `null`. Abrir app y volver a inicio no tienen gancho (los fija el sistema). Perfil por defecto de la build: `aosp-por-defecto`. |
| R8 y Baseline Profile | Reglas de R8 y perfil escrito a mano (`docs/arranque-en-frio.md`). APK release 66,2 MB → 10,5 MB con R8. | R8 **apagado por defecto** (`-Pul.minify=true` para probar): compila y pasa el análisis estático, **no se ejecutó en un teléfono**. El perfil no está grabado ni medido. Procedimiento de medida listo para el humano. |

**Sigue sin hacerse:** WebDAV (variante `sync`), cifrado del archivo exportado, restaurar widgets al importar, curvas de animación medidas, comparación lado a lado, perfil de trabajo, plegables, vivo/Xiaomi/Honor reales, CI y firma de release propia.

**Correcciones a lo que dice la noche más abajo:** el aviso de pérdida de conexión adb ya no aplica; «Android 12/14 no probados» pasa a «Android 12 probado en la tablet Huawei, 14 sin probar»; la APK de `dist/` actual es la **release** (`ultimatelauncher-default-release.apk`), no la debug; la variante `sync` compila pero sigue sin red implementada.

## Cómo probarlo en un minuto

```bash
tools/install.sh        # instala dist/ultimatelauncher-default-debug.apk y fija el launcher (ya está hecho en el CPH2841)
tools/restore.sh        # vuelve al launcher de OPPO (com.android.launcher)
```
Serie adb por defecto: `<ADB_SERIE_OPPO>` (CPH2841 por wifi; la serie wifi cambia si se reconecta: pasa la nueva como primer argumento o con `ANDROID_SERIAL`).
Qué mirar: pantalla de inicio 5×7, arrastrar hacia arriba para el cajón (lista plana, buscador arriba), pulsación larga → Widgets.

## Dispositivo

- **OPPO CPH2841**, Android 16 (API 36), ColorOS V16.1.0 (`CPH2841_16.0.10.500(EX01)`), 1440×3168, densidad 560 (override; física 640), 120 Hz por defecto (hasta 144 Hz), con GMS, launcher original `com.android.launcher` (v16.6.17).
- Decisión: el PGEM10 (USB) **no se usó** porque el usuario avisó de que se usa para otra cosa. Antes de recibir el aviso envié un `KEYCODE_HOME` y una captura al PGEM10; la captura se borró.

## Qué funciona (con evidencia)

| Hito | Estado | Evidencia |
|---|---|---|
| M0 Esqueleto de compilación | **Hecho** | `./gradlew :app:assembleDefaultDebug` genera `dist/ultimatelauncher-default-debug.apk` (88 MB, debug). `tools/check-permissions.sh`: permisos = BIND_APPWIDGET, REQUEST_DELETE_PACKAGES, VIBRATE, SET_WALLPAPER, SET_WALLPAPER_HINTS; **sin INTERNET**. La variante `sync` compila (`.sync`, con INTERNET). |
| M1 Arranca como launcher | **Hecho en Android 16** | Rol HOME = `com.qtekfun.ultimatelauncher` (`cmd role get-role-holders`), `dumpsys window` con foco en `com.android.launcher3.Launcher`, capturas y logcat sin FATAL/ANR tras arrancar, abrir cajón, buscar, volver con HOME. Restauración comprobada: al desinstalar el launcher, el rol vuelve a `com.android.launcher`. |
| M2 Recortes | **Hecho** (verificado en el dispositivo) | Inicio sin buscador (QSB) ni feed; `BuildConfig.QSB_ON_FIRST_SCREEN=false`. Cajón plano alfabético sin categorías ni fila de previstas; búsqueda local por nombre ("cal" → Calendar, Calculadora). Ajustes mínimos de la base (insignias y "añadir icono a inicio"). |
| M3 Widgets | **Hecho en lo comprobable** | Añadido el widget de AntennaPod con su `WidgetConfigActivity` (selector → diálogo del sistema → configuración → widget colocado, host del launcher en `dumpsys appwidget`). Ruta de cancelación probada con DevCheck. |
| M4 Tokens | **Parcial** | Rejilla 5×7, dock de 5, carpeta de 3 columnas e icono 57 dp aplicados desde `assets/themes/oppo-medido.json`. Faltan dock/paddings, radios, etiqueta, blur. No se hizo comparación lado a lado. |
| M5 Animaciones | **Parcial** | Inventario (`docs/anim-inventory.md`), perfiles `aosp-por-defecto`/`rapido`, superposición de 4 recursos de duración generada desde JSON. Verificado con Perfetto: cerrar cajón 311 ms (AOSP, 300) → 222 ms con `rapido` (210). No hay curvas medidas ni carga en ejecución. |
| M6 Primer arranque | **Parcial (mínimo)** | `OemAdapter`, `GenericAdapter`, `ColorOsAdapter`, asistente ES/EN: se muestra en el primer inicio, detecta `coloros`, los botones abren Ajustes (batería, detalles de app), «Hecho» vuelve al inicio. 6 pruebas unitarias. Ruta de autoarranque de ColorOS 16 sin resolver (`docs/oem-issues.md`). |
| M7 Exportar/importar | **Parcial** | Archivo JSON (esquema v1) con el selector de documentos, sin red. Exportó 8 elementos; la importación mostró resumen (app no instalada, perfil de trabajo, widget de otra marca) y al aplicar movió Gmail a la celda indicada, con copia de seguridad previa. 8 pruebas unitarias (reubicación de rejilla, esquema, planificador). Sin WebDAV, sin cifrado, widgets no restaurados. |
| Referencia | **Hecha** | `assets/themes/oppo-medido.json`, `assets/animations/oppo-medido.json` (ver «Limitaciones de la medición»). |

## Qué NO funciona o no está hecho

- M3b (tablet): **no hecho** (no hay tablet; las clases `tablet*` de los tokens están en `null`). M7 T7.4 (WebDAV): no hecho. M5 T5.4/T5.5 (curvas medidas y validación lado a lado): no hechos.
- Perfil de trabajo (RF-20/21): **no probado** (código de AOSP presente, sin verificar).
- Estética: no es «indistinguible» de OPPO todavía (dock, paddings, esquinas, blur, tipografía de etiqueta).
- Android 12 (API 31) y 14 (API 34): **no probados**. Se instalaron el emulador oficial 37.2.12 y las imágenes AOSP sin GMS de API 31 y 34, pero el emulador termina con SIGSEGV en este host con tres modos de GPU; `tools/emu-test.sh` está escrito y sin ejecutar. Por lectura: en API 31–33 el selector de widgets (`OnBackAnimationCallback`, API 34) fallaría al abrirse. Lint `NewApi` da 33 errores (API 33–35: `OnBackInvokedDispatcher`, `getParcelableExtra`, `Stream.toList`, `addLast`, colores `system_*` de API 34) que **no se han corregido**: en Android 12–13 fallarían, en 14 probablemente algunos. Solo se corrigió lo que rompía en Android 16 (parches 0007 y 0008).
- Sin GMS: el APK no depende de GMS (ni de `gms`/Firebase en el árbol de dependencias), pero **no se probó** en un teléfono ni emulador sin GMS (el CPH2841 lo tiene).
- Reinicio del teléfono y 12 h en segundo plano: no probados. Ajustes de batería/autoarranque: sin asistente.
- Tablet, multiventana, plegables, otras marcas (vivo, Xiaomi, Honor): no probados.
- La ventana de recientes y los gestos siguen siendo del launcher del sistema (límite sin root, `docs/02`): ahora que el launcher es el predeterminado, el gesto de volver a inicio usa el nuestro: **no se evaluó cómo se comporta con los gestos de ColorOS**.
- Compilación de release, firma propia, reproducibilidad, CI en GitHub Actions: no hechos.
- Diseño del lanzador por defecto: la pantalla de inicio sale casi vacía (diseño por defecto de AOSP 5×5 sobre una rejilla 5×7).

## Limitaciones de la medición (docs/04)

- Sin cámara de alta velocidad, según indicación. **Perfetto SÍ funciona sin root** (perfetto v49, usuario `shell`): se capturaron frametimeline, capas de SurfaceFlinger y transiciones de ventana. Pero cajón y carpetas se animan *dentro* de la ventana del Launcher, por lo que SF no da geometría por fotograma. Solo las transiciones de app tienen eventos de ventana.
- Las duraciones salen de ráfagas de fotogramas del Launcher (5 repeticiones, mediana, descartando repeticiones con >25 % de fotogramas con jank) y son **aproximadas**: carpeta abrir ≈ 877 ms, cerrar ≈ 823 ms, lanzar app ≈ 588 ms (tapa + cola de fotogramas). Frecuencia de pantalla real 120 Hz (`dumpsys display`, `renderFrameRate`).
- `drawer.open/close` y `app.returnHome` quedan en `null` (arrastre inyectado o variabilidad: `returnHome` bimodal ≈ 640/1040 ms). Muelles y bézier: **todo `null`** (haría falta seguimiento por vídeo; no se grabó vídeo). `pageScroll`, `pageSnap`, `icon.*`, `widget.resize`: no medidos.
- Estética: solo móvil en vertical y tema oscuro (no se cambiaron ajustes del teléfono); radios, etiqueta, blur/scrim: `null`. Un solo modelo (no se midió un segundo OPPO).
- Capturas, volcados de UI y trazas con datos personales: en `private-measurements/` (ignorado por git, no subido).

## Decisiones tomadas

Todas en `docs/DECISIONS.md`. Las que más afectan:
1. No hizo falta el plan B: la compilación de Launcher3 fuera de AOSP salió en ~1 h 15 min con Gradle propio (AGP 9.4.1, Kotlin 2.4.20, KSP, protobuf-lite). Lawnchair se estudió solo para versiones de plugins; no se copió código suyo.
2. Sin `QUERY_ALL_PACKAGES`: `<queries>` MAIN/LAUNCHER (probado necesario: sin él solo se veían 5 apps).
3. Privacidad: desactivado el broadcast de la primera pantalla (parche 0006); sin servicio AppFunctions en el manifiesto; `allowBackup=false`; proveedor de datos no exportado.
4. Flags de aconfig: todos `false`, y las clases `Flags` de plataforma redirigidas a stubs propios (evita choques con el framework).
5. Los tokens se aplican en compilación (script), no en ejecución.
6. Decisiones abiertas de `docs/08` resueltas con la propuesta del documento (GPLv3, variante `sync` declarada sin implementar, etc.).

## Problemas conocidos

- APK debug de 88 MB (sin minificar; incluye Compose y Material 3 alpha).
- `androidx.compose.material3:1.5.0-alpha29` y `androidx.appfunctions` alpha: dependencias inestables.
- 972 errores `StringFormatMatches` de Lint heredados de las traducciones de AOSP (no bloquean el APK, sí un `lint` limpio).
- Forma de icono personalizada (`GenericPathShape`) sustituida por un rectángulo redondeado (parche 0004); App Lock de Android 17 desactivado.
- Cambio de `applicationId`: al desinstalar/reinstalar se pierde la disposición del inicio.
- Pérdida de conexión con el CPH2841 a ~00:45 (ver arriba). Parche 0011 (compatibilidad API 33) preparado y **sin aplicar** por no poder verificarlo.
- El historial git contiene una copia comprimida de un APK de una versión anterior (el `.git` pesa 34 MB); el APK actual está fuera de git.
- Incidentes en el teléfono (sin consecuencias, lo digo por transparencia): mis toques abrieron un diálogo de privacidad de una app del sistema y la pantalla de inicio de sesión de UltimateNotes (en el que se escribieron letras en el campo del servidor, sin enviar nada); se hizo `force-stop` de UltimateNotes. Se aceptó 2 veces el diálogo del sistema «Crear widget» (sin «permitir siempre»). Los widgets de prueba desaparecieron al reinstalar el launcher.

## Los 3 siguientes pasos recomendados

1. **Compuertas de compatibilidad de API** (R1): resolver el SIGSEGV del emulador (otro host, o `-no-accel`/imagen ARM) o usar un teléfono con Android 12–14; aplicar el parche 0011, quitar `OnBackAnimationCallback` de `WidgetPickerActivity`/`AddItemActivity`, activar desugaring y corregir los 33 `NewApi`; decidir con datos si subir `minSdk` a 33.
2. **Cerrar M4 y M5**: ajustar dock/paddings/esquinas con comparación lado a lado y medir curvas (muelles/bézier) con grabación de pantalla + sellos de tiempo; después implementar `AnimationProfileProvider`.
3. **WebDAV (variante `sync`), cifrado del archivo exportado, restaurar widgets al importar, resto de adaptadores OEM verificados en vivo (vivo, Xiaomi, Honor)**, y una pasada de privacidad (24 h sin red, auditoría de `docs/09`), más CI en GitHub Actions con `tools/check-permissions.sh`.
