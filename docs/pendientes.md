# Pendientes (backlog vivo)

Última actualización: 2026-10-05 (copia de seguridad en Ajustes de inicio hecha). Lo hecho está en `docs/progress.md`; esto es lo que falta o se ha pedido y no está terminado.

## Entrevista del 2026-10-05 (decisiones del usuario)
- **Copia a demanda del setup:** disposición + ajustes del launcher + widgets (con su configuración hasta donde sea posible; la config interna de cada widget la guarda cada app), en un archivo con el selector del sistema, **sin cifrar**. Acceso desde **Ajustes de inicio** con tres acciones: Guardar copia, Restaurar copia, Traer mi pantalla de inicio.
- **Traer la pantalla de inicio de launchers cerrados** (OPPO, Huawei, vivo, Honor, Xiaomi, Samsung): solo «apps por orden» (sin capturas). Nova/Lawnchair/Pixel: por archivo de copia.
- **Funciones nuevas pedidas (prioridad ahora):** fondos de pantalla propios, paquetes de iconos, gestos (deslizar vertical = notificaciones / ajustes rápidos), ocultar apps con biometría o PIN. Doble toque para bloquear: pendiente (exige accesibilidad o administrador de dispositivo).

## Pedido por el usuario, aún sin hacer
- **Fondos de pantalla propios en el launcher** («unos fondos de pantalla chulos»): selector de fondos incluido en el launcher o galería de fondos propios (sin red en la variante `default`: imágenes empaquetadas con licencia libre o generadas por nosotros; nada de OEM). Ideas: pack pequeño (6–12) en WebP, selector desde «Fondo de pantalla y estilo», aplicar a inicio/bloqueo con `WallpaperManager` (permiso `SET_WALLPAPER`, ya en la tabla de `docs/09`), versión clara/oscura, desenfoque/oscurecimiento opcional. Decidir licencia y tamaño del APK (ahora 10,5 MB con R8).
- **Doble toque para bloquear la pantalla** (pedido en la entrevista, NO implementado a propósito): Android no ofrece API pública de bloqueo a un launcher normal. Las dos vías son `AccessibilityService` (`GLOBAL_ACTION_LOCK_SCREEN`, API 28+, exige activar un servicio de accesibilidad: permiso peligroso, aviso largo, restringido fuera de tiendas desde Android 13) y administrador de dispositivo (`DevicePolicyManager.lockNow()`, impide desinstalar sin pasos extra). Ambas rompen la regla de «permisos mínimos» de `docs/09`. Si el usuario lo quiere de todos modos, hacerlo como variante o módulo aparte, apagado por defecto y con aviso explícito; ver `docs/DECISIONS.md`.
- Gestos del escritorio y apps ocultas: pendiente de **probar en móvil y tablet** (checklist en `docs/progress.md`, «Gestos del escritorio y apps ocultas»). Ideas siguientes: gesto de dos dedos, deslizar abajo también sobre el dock, ocultar también los accesos del escritorio (opción), ocultar apps del perfil de trabajo.
- Arrastre desde la esquina para ampliar carpetas (fase 2 de las carpetas expandibles; hoy solo menú «Ampliar/Reducir»).
- Más columnas/migración de rejilla para «Iconos hasta el borde» (hoy solo ensancha celdas).

## Sin verificar en dispositivo
- Copia de seguridad (parche 0160): verificada guardar/restaurar en la tablet; falta restaurar en otro dispositivo, archivo cifrado, «Colocar mis apps por orden» por fecha de instalación y en móvil.
- Tablet: animación de cierre de carpeta (parche del subagente, solo por código), «Iconos hasta el borde» con mini hueco de 12 dp, dock editable tras reiniciar el launcher, retrato y multiventana.
- OPPO: menú y ampliación de carpetas 2x2, soltar iconos en carpeta ampliada (0144), reloj digital en el inicio (sizes reales, tarjeta cuadrada), importador «Traer mi pantalla de inicio» (checklist de 9 pasos en `progress.md`), R8 con widgets con configuración, perfil de trabajo, arrastres.
- Intents de batería/autoarranque de vivo, Xiaomi, Honor/Huawei (candidatos).

## Conocido y no arreglable sin root
- Iconos ≈1 s tarde tras el gesto de inicio y multitarea lenta en ColorOS (lo gobierna el launcher de OPPO; ver `progress.md`, 2026-10-05).

## Plan original (docs/07) sin cerrar
- M3: redimensionar/mover widgets y perfil de trabajo en dispositivo.
- M5: curvas medidas (muelles/bézier), validación lado a lado.
- M7: WebDAV (decidido NO), widgets con configuración al importar.
- M8: paquetes de iconos, clave de firma de release, CI real, build reproducible, revisión de licencias.
