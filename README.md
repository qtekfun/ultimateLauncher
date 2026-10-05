# UltimateLauncher

Launcher Android (fork delgado de Launcher3 de AOSP `android17-release`), sin servicios de Google, sin telemetría y **sin permiso INTERNET** en la variante `default`. Especificación en `docs/` (empieza por `CLAUDE.md` y `docs/00-README.md`); estado en `docs/progress.md` e informe en `docs/OVERNIGHT-REPORT.md`.

## Compilar e instalar
```bash
./gradlew :app:assembleDefaultDebug        # app/build/outputs/apk/default/debug/app-default-debug.apk
cp app/build/outputs/apk/default/debug/app-default-debug.apk dist/ultimatelauncher-default-debug.apk
tools/install.sh [serie_adb]               # instala y fija como launcher
tools/restore.sh [serie_adb]               # vuelve al launcher original de OPPO
tools/check-permissions.sh [apk] [default|sync]   # permisos exactos, sin INTERNET en default, sin clases de red
tools/privacy-audit.sh [--build]           # APK default y sync, dependencias y export sin credenciales (docs/privacy-audit.md)
tools/ci.sh                                # compila, pruebas, Lint y auditoría (workflow comentado en .github/workflows/ci.yml)
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
| `EXPAND_STATUS_BAR` | gesto opcional (apagado por defecto) de deslizar hacia abajo en el escritorio para abrir notificaciones o ajustes rápidos |
| `USE_BIOMETRIC` | «Apps ocultas»: pedir huella, rostro o PIN del dispositivo para verlas |

Sin `INTERNET`, sin almacenamiento, contactos, ubicación, cámara ni notificaciones. La visibilidad de apps usa `<queries>` (solo apps con icono de lanzador), no `QUERY_ALL_PACKAGES`.

## Estructura
`app/` (módulo Android y código propio: `oem/`, `layoutsync/`), `launcher3-base/` (AOSP + parches, ver `patches/`), `systemui-libs/` (librerías de AOSP), `platform-stubs/` (flags y APIs internas), `assets/` (tokens y perfiles de animación), `tools/`.

## Licencia
GNU General Public License v3.0 o posterior (texto en `LICENSE`, atribuciones en `NOTICE`), la misma que UltimateDeck. Las partes de AOSP (Apache-2.0, compatible con la GPLv3) conservan sus cabeceras originales; el código propio lleva una cabecera SPDX mínima (`GPL-3.0-or-later`, «UltimateLauncher contributors»).

## F-Droid
Preparación para publicar en el catálogo de F-Droid: metadatos en `fastlane/metadata/android/`, receta borrador en `docs/fdroid/` y estado en `docs/fdroid.md`.
