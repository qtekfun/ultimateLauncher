# 08 — Riesgos y decisiones

## Riesgos

| # | Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|---|
| R1 | Launcher3 de Android 17 usa APIs que no existen en Android 12-16 | Alta | Alto | Compuertas por versión y Lint `NewApi` como error; prueba en M1; revisar `minSdk` (ver `03`). Estado 2026-10-05: Lint `NewApi` = 0 y como error; 10 cambios de compatibilidad; ver `docs/compat-android12-14.md` (sin probar en 13/14) |
| R2 | Montar la compilación Gradle fuera de AOSP lleva mucho más de lo previsto | Media | Alto | M0 aislado; modelo de Lawnchair; si bloquea, replantear |
| R3 | No se pueden igualar las animaciones del sistema (gestos y recientes) | Alta | Medio | Alcance declarado en `04`; documentar; opción root futura |
| R4 | Las ROMs matan el proceso o rompen widgets | Alta | Alto | Asistente por marca; lista de pruebas; avisos |
| R5 | Rutas de autoarranque cambian entre versiones de ROM | Alta | Medio | Datos en `oem-intents.json`; reserva a detalles de app |
| R6 | Mantenimiento semestral de AOSP | Segura | Medio | `/patches` + `UPSTREAM.md`; parches mínimos |
| R7 | Medir animaciones con grabación limitada da curvas imprecisas | Media | Medio | Cámara de alta velocidad; 5 repeticiones; mediana; Perfetto para duraciones |
| R8 | La estética depende del modelo y de la versión de ColorOS | Media | Bajo | Perfil con `source`; medir en dos modelos |
| R9 | Se descubre que la rama de AOSP trae más funciones de las esperadas (o menos) | Media | Bajo | M1 T1.4 corrige `02` |
| R10 | Firmware chino sin GMS rompe alguna dependencia | Media | Medio | Prueba en M1 sin GMS; sin dependencias de Google |
| R11 | Móvil y tablet multiplican las rejillas, tokens y pruebas | Alta | Medio | Clases de pantalla sobre los perfiles de Launcher3; M3b; matriz con tablet |
| R12 | Sin permiso de red, el sync depende de otra app (selector de documentos) o de la variante `sync` | Media | Medio | Documentar los dos caminos; verificar Nextcloud en el selector; variante `sync` opcional |
| R13 | Dos variantes (`default` y `sync`) con `applicationId` distinto no comparten datos | Media | Bajo | Paso entre ambas por archivo; decisión abierta sobre si se mantiene la variante `sync` |

## Decisiones tomadas

1. **Nombre**: UltimateLauncher. `applicationId` propuesto `com.qtekfun.ultimatelauncher` (misma convención que otros proyectos del autor; confirmar).
2. **Dispositivos**: móvil y tablet con la misma app.
3. **`minSdk` 31** (Android 12). Se revisa al cierre de M1 si subirlo aporta ventajas sustanciales (tabla en `03`).
4. **Sin GMS y privacidad primero**: la variante por defecto no declara `INTERNET`; el sync por defecto es por archivo/selector de documentos; WebDAV solo en la variante opcional `sync` (propuesta, confirmar).

## Decisiones abiertas (pendientes de confirmar)

1. **Licencia**: propuesta GPLv3 (la base es Apache 2.0; revisar la compatibilidad con cualquier código de Lawnchair que se reutilice).
2. **¿Se mantiene la variante `sync` con WebDAV directo**, o solo el selector de documentos?
3. **Root opcional** en algún dispositivo para un modo con gestos nativos (fuera de v1).
4. **Matriz de dispositivos** real del usuario (móviles y tablet, Android, GMS).
5. **Repositorio**: dónde alojarlo y política de ramas.
6. **Sistema de ajustes**: preferencias estándar o la capa de la base.
7. **Private Space y archivado de apps**: solo si el `minSdk` sube a 35.

## Notas legales y de uso

No soy abogado; estas son reglas de prudencia, no asesoramiento legal.

- No copiar ni incluir código, recursos, fuentes, iconos, fondos ni marcas de OPPO ni de otros fabricantes.
- La medición se hace **observando** el comportamiento en teléfonos del propio usuario; no se descompila ni se redistribuye nada de terceros.
- Los **tiempos, curvas y medidas** son parámetros observables; aun así, si el proyecto va a publicarse, conviene una revisión por alguien con conocimiento legal sobre la reproducción del "aspecto y sensación".
- Respetar las licencias de todo lo que se reutilice: AOSP (Apache 2.0) y Lawnchair (revisar la licencia concreta de cada archivo antes de reutilizarlo).
- No usar nombres de producto de terceros en el nombre, el icono ni la descripción de la app.

## Qué falta por conocer

- Qué modelos y versiones exactas usa el usuario.
- Qué trae realmente la rama `android17-release` de Launcher3 (se confirma en M1).
- Si hay forma razonable de conseguir fluidez a la frecuencia nativa en todas las ROMs.
- Qué permisos declara realmente el manifiesto de la base (se audita en M0, ver `09`).
