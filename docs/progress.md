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

## Hitos
(se irá actualizando)

## Fase de referencia (hecha 2026-10-04, CPH2841, launcher OPPO activo, tema oscuro)
- Capturas, dumps UI, trazas Perfetto: `private-measurements/` (ignorado por git, contiene datos personales; no se sube).
- `assets/themes/oppo-medido.json`: rejilla 5x7 (inicio), dock 5, icono ≈57 dp, celda 77,7x86,9 dp, dock 89 dp, cajón 5 col, buscador del cajón abajo, carpeta 3 col. Solo móvil vertical y tema oscuro. Radios, etiqueta, blur/scrim: null.
- `assets/animations/oppo-medido.json`: Perfetto SÍ funciona sin root (shell, perfetto v49): frametimeline, SF layers, transiciones WM shell, atrace. Pero cajón/carpetas se animan dentro de la ventana del Launcher, así que SF no da geometría. Duraciones solo por ráfagas de fotogramas = APROXIMADAS (carpeta abrir ≈877 ms, cerrar ≈823 ms, lanzar app ≈588 ms; frecuencia de pantalla 120 Hz). Muelles y bézier: null (no hay seguimiento por vídeo).
- No se hizo grabación de vídeo (no necesaria para lo anterior; queda pendiente para ajustar curvas).
