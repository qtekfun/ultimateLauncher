# 01 — Requisitos

## Contexto de uso

Una persona que cambia con frecuencia entre teléfonos OPPO, vivo, Xiaomi y Honor (parte con firmware chino, sin GMS) y que también usa tablet, y quiere la misma interfaz en todos. Móviles y tablets con **Android 12 o superior** (`minSdk` 31), con foco de pruebas en Android 16 y 17.

## Requisitos funcionales (RF)

**Pantalla de inicio**
- RF-01 Rejilla de iconos configurable (columnas/filas) con carpetas y arrastrar y soltar.
- RF-02 **Widgets de terceros** completos: añadir, configurar (incluidos los que piden pantalla de configuración), redimensionar, mover.
- RF-03 **Sin widget de búsqueda** en la pantalla de inicio ni en el dock.
- RF-04 Sin feed/panel a la izquierda de la primera página.
- RF-05 Dock con apps fijas.

**Cajón de aplicaciones**
- RF-10 Lista **plana**, ordenada alfabéticamente, con barra de desplazamiento rápido.
- RF-11 **Búsqueda local por nombre de app** (sin web, sin contactos, sin sugerencias, sin "búsqueda de color" ni similares).
- RF-12 **Sin categorías**.
- RF-13 Fila de apps previstas/sugeridas: **desactivada** (no existe en v1).

**Perfil de trabajo**
- RF-20 Soporte de perfil de trabajo: pestañas personal/trabajo en el cajón, apps de trabajo con insignia, pausa del perfil.
- RF-21 Los widgets y accesos de apps de trabajo funcionan como en el launcher del sistema.

**Aspecto y movimiento**
- RF-30 Estética inspirada en el launcher de OPPO, definida por **tokens** (ver `04`).
- RF-31 Animaciones definidas por un **perfil** intercambiable (muelle o curva), con multiplicador global de velocidad.
- RF-32 Packs de iconos de terceros y forma de icono configurable.

**Adaptación a fabricantes**
- RF-40 Asistente de primer arranque: fijar como launcher predeterminado, quitar restricciones de batería y activar autoarranque, con rutas específicas por marca (ver `05`).
- RF-41 Detección de fallos conocidos por marca y aviso al usuario.

**Tablet y móvil**
- RF-60 Una sola app con **rejillas distintas por clase de pantalla** (móvil, tablet, vertical y horizontal), configurables por separado.
- RF-61 En tablet: rejilla más densa, iconos y dock dimensionados para pantalla grande, cajón adaptado, carpetas y widgets proporcionados.
- RF-62 Rotación en ambos tipos de dispositivo, respetando el ajuste del sistema; cambiar de tamaño de ventana o entrar en multiventana no pierde estado.
- RF-63 Compartir la disposición entre móvil y tablet **con adaptación** (ver `06`): no tiene que quedar idéntica, sí coherente.

**Sincronización**
- RF-50 Exportar/importar disposición completa en JSON.
- RF-51 Mover el archivo entre dispositivos por el **selector de documentos del sistema** (por defecto, sin permiso de red) o, en una variante opcional, por **WebDAV** (Nextcloud del usuario).
- RF-52 Al importar en un teléfono distinto, adaptar lo que no exista (apps no instaladas, widgets de otro fabricante, rejilla distinta) sin fallar.

## Requisitos no funcionales (RNF)

- RNF-01 Funciona **sin servicios de Google (sin GMS)** y sin root; ninguna dependencia de GMS, ni siquiera opcional.
- RNF-02 **Privacidad primero**: sin telemetría, sin analíticas, sin informes de fallos automáticos y **sin permiso `INTERNET` en la compilación por defecto** (ver `09`). La red solo existe en una variante opcional de sincronización.
- RNF-03 Arranque en frío y desplazamiento sin saltos de fotograma en gama media; objetivo de 60 fps sostenidos y a la frecuencia nativa del panel (90/120 Hz) cuando el sistema la conceda.
- RNF-04 Idioma de interfaz: español e inglés.
- RNF-05 Licencia compatible con la base (Launcher3 es Apache 2.0). **Decidido: GPL-3.0-or-later** para todo el proyecto (2026-10-05, ver `docs/DECISIONS.md`; Apache-2.0 es compatible con incluirse en un proyecto GPLv3; el usuario puede cambiarla).
- RNF-06 Distribución: APK propio (Obtainium/GitHub); F-Droid más adelante, no en v1.
- RNF-07 `minSdk` **31** (Android 12). Foco de pruebas: Android 16 y 17. Se revisará subir el mínimo si aporta ventajas sustanciales (ver `03`).

## Fuera de alcance v1

Feed o asistente, gestos de recientes nativos con animaciones del sistema (requieren root o ser app del sistema), barra de tareas de tablet del sistema (ver `02`), Private Space (no debe romperse, pero no se diseña para él), plegables (no deben romperse, no se optimizan), temas dinámicos por fondo de pantalla, copia de seguridad en la nube de terceros.

## Criterios de "hecho" a nivel de producto

1. Se instala como launcher en un OPPO, un vivo, un Xiaomi y un Honor con Android 16/17 y la disposición exportada de uno se importa en otro con las diferencias esperadas.
2. Añadir y configurar widgets funciona en los cuatro.
3. El perfil de trabajo aparece y se puede pausar.
4. Las animaciones usan el perfil `oppo-medido` y se perciben equivalentes a las del launcher de OPPO en una prueba lado a lado.
5. La misma app se instala y funciona en una tablet, con su propia rejilla.
6. La compilación por defecto declara solo los permisos de `09` y ninguno de red, y la auditoría de `09` pasa.
