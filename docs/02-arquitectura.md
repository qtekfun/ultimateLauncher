# 02 — Arquitectura

## Principio

Fork **delgado** de Launcher3. Todo lo propio vive en módulos y paquetes separados para poder absorber las publicaciones semestrales de AOSP (segundo y cuarto trimestre) con el menor conflicto posible.

## Estructura propuesta del repositorio

```
/app                     módulo Android (launcher)
/launcher3-base          código de AOSP Launcher3 (android17-release) con parches mínimos
/platform-stubs          stubs/compileOnly de APIs internas del sistema necesarias
/feature-animprofile     motor de perfiles de animación
/feature-theme           tokens de diseño (rejilla, iconos, dock, cajón, carpetas)
/feature-oemkit          onboarding y adaptadores por fabricante
/feature-layoutsync      exportar/importar disposición (archivo/SAF, sin red)
/feature-sync-webdav     cliente WebDAV; solo se compila en la variante `sync`
/tools/anim-measure      scripts de medición de animaciones (Python, fuera de la app)
/tools/ui-measure        guía y scripts para medir estética
/patches                 lista numerada de parches sobre AOSP con motivo de cada uno
/docs                    esta especificación
```

Regla: **cada cambio sobre código de AOSP se registra en `/patches`** con archivo, motivo y cómo reaplicarlo. Es lo que hará viable el mantenimiento.

## Qué se conserva de Launcher3

- Modelo de datos de la pantalla de inicio (workspace, hotseat, carpetas) y su base de datos.
- Gestión de widgets (`AppWidgetHost`, `AppWidgetManager`), incluidos los de configuración.
- Gestión de perfiles de usuario (`LauncherApps`, `UserManager`): pestañas personal/trabajo, insignias, pausa del perfil.
- Arrastrar y soltar, carpetas, indicadores de página.
- Cajón de aplicaciones con búsqueda por nombre (se simplifica, ver abajo).

## Qué se elimina o desactiva

| Elemento | Acción |
|---|---|
| Barra/widget de búsqueda (QSB) en inicio y dock | Eliminar de la carga de layout por defecto y de las opciones del usuario |
| Fila de apps previstas en el cajón | Desactivar y quitar la UI |
| Búsqueda más allá de nombre de app (web, contactos, sugerencias) | Eliminar proveedores; dejar solo el filtro local |
| Cualquier gancho a feed/panel izquierdo | Eliminar |
| Categorías en el cajón | No implementar (verificar que la base no las añade) |
| Dependencias de servicios de Google | Eliminar o hacer opcionales con comprobación de disponibilidad |

*(Verificar en la primera compilación qué trae realmente la rama; esta tabla parte de lo que se espera de AOSP.)*

**Verificado en M1 (2026-10-04, android17-release, sin `quickstep/`):**
- QSB: existe y lo controla `BuildConfig.QSB_ON_FIRST_SCREEN` (por defecto `true` en `tools/buildconfig.sh`); UltimateLauncher lo pone a `false` y no aparece en inicio ni en dock.
- Fila de previstas, feed/panel izquierdo y categorías: no aparecen sin `quickstep/` (los proveedores viven allí); el cajón es una lista plana con búsqueda local.
- Búsqueda: `search_container_all_apps` filtra por nombre en local (probado: "cal" → Calendar, Calculadora).
- Dependencias de GMS: ninguna en el código importado; sí hay `AppFunctions`, `slice` y otros androidx que se mantienen sin red.
- La base declara permisos de más (QUERY_ALL_PACKAGES, CALL_PHONE, READ_EXTERNAL_STORAGE, POST_NOTIFICATIONS…): el manifiesto propio (`app/src/main/AndroidManifest.xml`) los quita.

## Puntos de extensión propios

1. **AnimationProfileProvider**: sustituye las constantes de duración/interpoladores/muelles que usa Launcher3 por valores de un perfil (ver `04`).
2. **ThemeTokens**: rejilla, tamaños de icono y etiqueta, radio y forma, altura y fondo del dock, fondo del cajón, estilo de carpetas, indicador de página.
3. **OemAdapter** (interfaz): `id`, `matches(Build)`, `autostartIntents()`, `batteryIntents()`, `defaultLauncherHelp()`, `knownIssues()`. Una implementación por familia de ROM (ver `05`).
4. **LayoutSnapshotter**: serializa y reconstruye el estado de la pantalla de inicio (ver `06`).

## Límites del modelo sin root (importante)

Mi entendimiento, a verificar en las primeras pruebas:

- Un launcher normal **no puede** ser el proveedor de recientes del sistema. El gesto de volver a inicio y el menú de recientes siguen siendo del launcher de la ROM, así que esas animaciones **no se pueden igualar** sin root.
- Las animaciones **dentro** del launcher (cambio de página, apertura/cierre del cajón, carpetas, pulsación de iconos, arrastre) **sí** se controlan por completo.
- La animación de abrir una app desde el icono se limita a lo que permiten las opciones de actividad (`ActivityOptions`); las transiciones remotas del sistema requieren permisos que una app normal no tiene.
- Opcional futuro (fuera de v1): integración tipo QuickSwitch con root.

## Tablet y móvil

- Launcher3 ya modela **perfiles de dispositivo** (rejillas por tamaño y orientación). Se parte de eso, no se reescribe.
- Clases de pantalla mínimas: `phone`, `phone-landscape`, `tablet`, `tablet-landscape`, cada una con rejilla, tamaños y dock propios dentro de `ThemeTokens`.
- La barra de tareas de tablet pertenece al launcher del sistema (mi entendimiento: depende de ser el proveedor de recientes). **No se espera** en UltimateLauncher; se confirma en el hito M3b.
- Plegables: no se diseña para ellos; cambiar de estado no debe cerrar la app ni perder datos.
- Rotación, cambio de tamaño de ventana y multiventana no deben recargar la base de datos ni perder estado.

## Privacidad

La arquitectura se rige por `09`: variante por defecto sin `INTERNET`, sin dependencias de Google, permisos mínimos y datos solo locales.

## Datos y persistencia

- Base de datos de Launcher3 tal cual, con migraciones controladas.
- Perfiles de animación y tokens de tema: archivos JSON versionados en `assets/` con posibilidad de importar uno externo.
- Ajustes del usuario: `DataStore` o el mecanismo de preferencias de la base, a decidir en el hito M2.
- Credenciales WebDAV (solo variante `sync`): contraseña de aplicación de Nextcloud guardada en el almacén seguro del sistema (Android Keystore).

## Pruebas

- Unitarias para: serialización/deserialización del layout, adaptación de rejilla, selección de `OemAdapter`, carga de perfiles de animación.
- Instrumentadas: arranque del launcher, añadir widget con configuración, perfil de trabajo (en emulador con perfil gestionado).
- Manuales: lista de `05` por dispositivo real.
