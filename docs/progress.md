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
| 897201dc (USB) | OPPO PGEM10 | 16 (API 36), PGEM10_16.0.5.1201(CN01), ColorOS V16.0.0 | **Dispositivo de trabajo**. 1080x2376, 480 dpi, 60/90/120 Hz (120 por defecto), GMS presente |
| <SERIE_OPPO> (wifi) | OPPO CPH2841 | 16, CPH2841_16.0.10.500(EX01) | NO se toca (tiene usuario MultiApp) |
| <SERIE_PIXEL> (wifi) | Pixel 8 | 17 | NO se toca |

## Launcher de sistema original (VÍA DE VUELTA)
- Paquete: `com.android.launcher` (OplusLauncher, /system_ext/priv-app/OplusLauncher, v16.4.28)
- Actividad HOME: `com.android.launcher/.Launcher`
- Titular del rol HOME antes de cambiar nada: `com.android.launcher`
- Restaurar: `tools/restore.sh` o a mano:
  `adb -s 897201dc shell cmd role add-role-holder --user 0 android.app.role.HOME com.android.launcher`
- Escalas de animación: 1.0/1.0/1.0.

## Hitos
(se irá actualizando)
