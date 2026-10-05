# Pendientes (backlog vivo)

Última actualización: 2026-10-05. Lo hecho está en `docs/progress.md`; esto es lo que falta o se ha pedido y no está terminado.

## Pedido por el usuario, aún sin hacer
- ~~Fondos de pantalla propios~~ y ~~paquetes de iconos~~: hechos por código (12 fondos generados + selector, y soporte de packs ADW/Nova/Apex; ver `progress.md`, «Fondos propios y paquetes de iconos»). Falta verificarlos en dispositivo y, si se quiere, más fondos (versión clara/oscura por fondo) y fondos dinámicos.
- Arrastre desde la esquina para ampliar carpetas (fase 2 de las carpetas expandibles; hoy solo menú «Ampliar/Reducir»).
- Más columnas/migración de rejilla para «Iconos hasta el borde» (hoy solo ensancha celdas).

## Sin verificar en dispositivo
- Tablet: animación de cierre de carpeta (parche del subagente, solo por código), «Iconos hasta el borde» con mini hueco de 12 dp, dock editable tras reiniciar el launcher, retrato y multiventana.
- OPPO: menú y ampliación de carpetas 2x2, soltar iconos en carpeta ampliada (0144), reloj digital en el inicio (sizes reales, tarjeta cuadrada), importador «Traer mi pantalla de inicio» (checklist de 9 pasos en `progress.md`), R8 con widgets con configuración, perfil de trabajo, arrastres.
- Intents de batería/autoarranque de vivo, Xiaomi, Honor/Huawei (candidatos).
- Fondos propios (selector, vista previa, aplicar a Inicio/Bloqueo, desenfoque/oscurecer) y paquetes de iconos (pack de prueba debug, interruptor del fondo, recarga y caché): solo compilados y con pruebas unitarias; el OPPO estaba bloqueado.
- Paquetes de iconos: el calendario dinámico (`<calendar>`) y los relojes del pack no se soportan; con `res/xml/appfilter` y `assets/appfilter.xml` a la vez se usa el de `assets`.

## Conocido y no arreglable sin root
- Iconos ≈1 s tarde tras el gesto de inicio y multitarea lenta en ColorOS (lo gobierna el launcher de OPPO; ver `progress.md`, 2026-10-05).

## Plan original (docs/07) sin cerrar
- M3: redimensionar/mover widgets y perfil de trabajo en dispositivo.
- M5: curvas medidas (muelles/bézier), validación lado a lado.
- M7: WebDAV (decidido NO), widgets con configuración al importar.
- M8: clave de firma de release, CI real, build reproducible, revisión de licencias.
