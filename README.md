# UltimateLauncher

Launcher Android (fork delgado de Launcher3 de AOSP `android17-release`), sin servicios de Google, sin telemetría y **sin permiso INTERNET** en la variante `default`. Especificación en `docs/` (empieza por `CLAUDE.md` y `docs/00-README.md`); estado en `docs/progress.md` e informe en `docs/OVERNIGHT-REPORT.md`.

## Compilar e instalar
```bash
./gradlew :app:assembleDefaultDebug        # app/build/outputs/apk/default/debug/app-default-debug.apk
cp app/build/outputs/apk/default/debug/app-default-debug.apk dist/ultimatelauncher-default-debug.apk
tools/install.sh [serie_adb]               # instala y fija como launcher
tools/restore.sh [serie_adb]               # vuelve al launcher original de OPPO
tools/check-permissions.sh                 # la variante default no debe declarar INTERNET
./gradlew :app:testDefaultDebugUnitTest    # 14 pruebas unitarias
```
Requisitos: JDK 21, Android SDK con plataforma 37.0 y build-tools 36+, Gradle 9.8 (wrapper).

## Permisos de la variante `default`
| Permiso | Motivo |
|---|---|
| `BIND_APPWIDGET` | alojar widgets de terceros |
| `REQUEST_DELETE_PACKAGES` | desinstalar desde el icono |
| `SET_WALLPAPER`, `SET_WALLPAPER_HINTS` | fondo de pantalla (lo exige `WallpaperManager` al arrancar) |
| `VIBRATE` | respuesta háptica |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | solo si lo pides en el asistente: evita que el sistema mate el launcher en segundo plano |

Sin `INTERNET`, sin almacenamiento, contactos, ubicación, cámara ni notificaciones. La visibilidad de apps usa `<queries>` (solo apps con icono de lanzador), no `QUERY_ALL_PACKAGES`.

## Estructura
`app/` (módulo Android y código propio: `oem/`, `layoutsync/`), `launcher3-base/` (AOSP + parches, ver `patches/`), `systemui-libs/` (librerías de AOSP), `platform-stubs/` (flags y APIs internas), `assets/` (tokens y perfiles de animación), `tools/`.

## Licencia
Las partes de AOSP son Apache-2.0 (cabeceras originales). La licencia del código propio es una decisión abierta (propuesta GPLv3, `docs/08`); no se ha añadido texto de licencia todavía.
