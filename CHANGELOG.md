# Cambios

Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Las notas de cada versión se publican en la release de GitHub.

## [0.2.1] - 2026-10-12
### Corregido
- Tablets y plegables: al deslizar hacia el cajón empezando encima del dock saltaba el menú del dock («Ajustes del dock», «Ocultar recientes»).

## [0.2.0] - 2026-10-12
### Añadido
- Apps dobles (Android 15+): las apps del espacio privado pueden ir al escritorio junto a la copia principal, con la insignia del sistema y en gris mientras el espacio esté bloqueado; tocarlas pide el desbloqueo. «Abrir en la otra cuenta» y «Añadir cuenta 2 al escritorio» en el menú, deslizar hacia arriba sobre el icono para abrir la otra copia y mosaico de Ajustes rápidos «Abrir cuenta 2». Ajustes en «Apps dobles».
### Corregido
- Plegables: se usaba la rejilla medida de móvil, que no admite dos pantallas, y el launcher podía acabar con una rejilla de tablet; ahora usa la rejilla de AOSP preparada para plegables.
- El launcher se cerraba al bloquear el espacio privado (lectura de un ajuste del sistema no permitido a terceros).
### Permisos
- `ACCESS_HIDDEN_PROFILES` (normal): necesario para ver el espacio privado.

## [0.1.0] - 2026-10-05
Primera versión pública.
- Launcher basado en Launcher3 de AOSP (`android17-release`), minSdk 31, sin servicios de Google, sin telemetría y sin permiso `INTERNET` en la variante `default`.
- Estética medida en OPPO (rejilla, iconos, dock, cajón, carpetas) y soporte de tablet con dock estilo Huawei.
- Carpetas al estilo OPPO, ampliables a 2x2; menús contextuales unificados.
- Copia de seguridad y restauración a demanda; importar de otros launchers.
- Fondos propios en 4K, paquetes de iconos, gestos de deslizar y apps ocultas con biometría o PIN.
- Reloj digital como widget propio.
