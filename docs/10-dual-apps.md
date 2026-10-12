# 10 — Dual apps (v2)

Estado: borrador v2 — 2026-10-12. Hito **M9** de `07`. Depende del spike de la fase 0 (ver abajo).

## Qué es

Usar las dos copias de una app (espacio principal + **Private Space**) desde el escritorio como **dos iconos**: el de la copia principal, normal, y el de la copia del Private Space con una **insignia** pequeña («2» o similar). El launcher sabe que forman un **par** para ofrecer accesos rápidos a la otra copia. No usa perfil de trabajo.

**Entra**
- Detección de pares entre el espacio principal y el Private Space.
- Iconos de apps del Private Space en el escritorio, con insignia, junto a los de la copia principal.
- Desbloqueo del Private Space desde el launcher.
- Mosaico de Ajustes rápidos «Abrir cuenta 2».

**No entra**
- Perfil de trabajo como mecanismo de clonado. El perfil de trabajo (p. ej. Intune) se sigue mostrando tal cual (RF-20), **sin emparejarlo**.
- Usuarios secundarios (revisado 2026-10-12, descartado): cada usuario es una sesión aparte con su propio launcher; `LauncherApps.getProfiles()` solo devuelve los perfiles del usuario actual (principal, trabajo, privado) y lanzar o listar apps de otro usuario exige `INTERACT_ACROSS_USERS(_FULL)`, de firma/privilegiado. Una app normal no puede, ni con el usuario en segundo plano.
- Más de dos copias de una app.
- Taskbar y Recientes: siguen siendo del sistema (`02`, «Límites del modelo sin root»).

**Plataformas**
- Objetivo principal: **GrapheneOS en Pixel** (plegables y no plegables) y cualquier AOSP con Private Space. Dispositivo de pruebas: **Pixel 9 Pro Fold** con GrapheneOS (disponible más adelante); hasta entonces, emulador API 35+.
- ROMs de OPPO, vivo, Xiaomi y Honor: modo «lo que el sistema exponga» (ver *Fallback en ROMs de fabricante*).

## Restricciones técnicas

El launcher se instala como app normal, no de sistema.

| Restricción | Consecuencia |
|---|---|
| No es app privilegiada ni proveedor de Recientes | Variante de Launcher3 **sin Quickstep** (la actual). Taskbar, Recientes y la animación del gesto de inicio siguen siendo del launcher del sistema |
| `minSdk` 31 | Todo lo de Private Space va detrás de `Build.VERSION.SDK_INT >= 35`. En API 31–34 el módulo se oculta |
| Private Space exige `ACCESS_HIDDEN_PROFILES` + rol `ROLE_HOME` | Sin ser launcher por defecto no se ve el espacio. El asistente ya pide fijarlo (RF-40). **Permiso nuevo**: se añade a la tabla de `09` y a `tools/permissions-allowed.txt` en M9, no antes. Nivel de protección y requisitos exactos: **sin verificar** (spike) |
| `requestQuietModeEnabled(false, user)` desde una app de terceros | Qué permiso o condición exige a un launcher por defecto: **sin verificar** (spike) |
| Un solo Private Space por dispositivo, solo en el usuario principal | Máximo dos copias por app. Si el launcher corre en un usuario secundario, Dual apps se desactiva |
| Flags aconfig de Launcher3 todos a `false` (`tools/gen-flags.py`, DECISIONS 2026-10-04) | `PrivateProfileManager` y la sección de Private Space del cajón dependen de `Flags.enablePrivateSpace()` y afines. Hay que activarlos (parche en `/patches`) y probar que no rompen API 31–34 |
| Ajustes de Private Space no expuestos a terceros (al menos en Android 15) | No se puede abrir su pantalla de ajustes ni leer si está en modo «oculto». Se trata como **estado desconocido** y se verifica en el spike en 16 y 17 |
| ColorOS de China no deja fijar el launcher como inicio (`oem-issues.md`, PMX110) | Sin `ROLE_HOME` no hay Private Space: **Dual apps no está disponible** en ese firmware. Aceptado de momento (2026-10-12) |

Fuente: documentación de Private Space de AOSP (source.android.com).

## Componentes

Código propio en clases nuevas; sobre AOSP solo puntos de extensión registrados en `/patches`.

- **ProfileRepository**: lista perfiles con `LauncherApps.getProfiles()` y clasifica cada uno con `getLauncherUserInfo()` (principal, privado, trabajo, otro). Escucha `ACTION_PROFILE_AVAILABLE/UNAVAILABLE` y los broadcasts de bloqueo del Private Space.
- **DualAppIndex**: cruza las actividades de ambos perfiles y mantiene la tabla de pares. Un **par** es el mismo `packageName` con actividad lanzable en el espacio principal y en el Private Space. Se recalcula con `LauncherApps.Callback` (instalar, desinstalar, cambiar). Lógica pura, con pruebas unitarias.
- **LaunchRouter**: único punto de lanzamiento. Recibe (componente, perfil); si el perfil está bloqueado, pide desbloqueo y lanza al volver.
- **PrivateSpaceController**: estado (bloqueado / desbloqueado / desconocido) con `UserManager.isQuietModeEnabled()`; desbloqueo con `requestQuietModeEnabled(false, user)`, que dispara la autenticación del sistema.
- **DualAppTileService**: mosaico «Abrir cuenta 2» que lanza la copia contraria de la última app emparejada usada. Vive en el propio launcher, sin `CrossProfileApps`. Guarda solo el último par usado, en el almacenamiento privado del launcher (ver *Privacidad*).

## Interacción

- **Iconos** (decidido 2026-10-12): **dos en el escritorio**, uno por copia. La copia principal lleva su icono normal; la del Private Space, el mismo icono con una insignia pequeña en una esquina («2» o similar). **Diseño propio**: no se copia la insignia del clonador de ningún fabricante. El estilo se configura y sigue los tokens de `04`; las medidas de la insignia son `null` hasta definirlas.
- **Añadir al escritorio**: desde el cajón (sección de Private Space) o desde el menú del icono principal («Añadir cuenta 2 al escritorio», que la coloca en la celda libre más cercana). **Sin verificar** que la base deje poner apps del Private Space en el escritorio (spike T9.0); si no lo deja, es un parche propio.
- **Toque**: cada icono abre **su** copia.
- **Mantener pulsado**: el menú contextual (parche 0150) de cualquiera de los dos iconos añade arriba «Abrir en la otra cuenta».
- **Deslizar hacia arriba sobre un icono del par**: abre la otra copia. **Entra en v2** (decidido 2026-10-12); se puede desactivar en Ajustes.
- **Cajón**: se mantiene la sección de Private Space de Launcher3; las apps emparejadas no se duplican fuera de ella.
- **Ajustes**: interruptor global, lista de pares, estilo de la insignia, gesto de deslizar y opción para tratar los pares como apps sin relación (sin accesos a la otra copia).

## Bloqueo y privacidad

- Con el espacio bloqueado, el icono con insignia se muestra atenuado con candado (como una app de trabajo en pausa) y el toque pide desbloqueo y abre la app al volver. El atajo «Abrir en la otra cuenta» del icono principal hace lo mismo. Decidido 2026-10-12 (descartado: quitarlos del escritorio mientras esté bloqueado).
- Si el usuario oculta el Private Space, los iconos con insignia y los atajos a la otra cuenta desaparecen del escritorio. Como el modo oculto puede no ser legible, hay una opción manual **«ocultar pares»**.
- Ningún dato del par sale del almacenamiento privado del launcher ni aparece en registros (`09`, «Datos locales»).
- La tabla de pares, la última app usada y las posiciones de los iconos del Private Space **no** entran en la copia de seguridad ni en la exportación de disposición (`06`): revelarían que existe el Private Space.

## Fallback en ROMs de fabricante

OPPO, vivo, Xiaomi y Honor tienen su propio clonado de apps, a menudo como usuario o perfil aparte (p. ej. el usuario 999). El launcher no fuerza nada:
- Si `getProfiles()` devuelve ese perfil con actividades lanzables, se empareja igual.
- Si no lo devuelve, Dual apps queda limitado al Private Space de esa ROM (si existe).
- El spike lo comprueba en cada marca y anota el resultado en `oem-issues.md`.

## Fase 0 — spike (puerta del hito)

Antes de escribir el módulo:
1. Confirmar `ACCESS_HIDDEN_PROFILES` (nivel de protección), `requestQuietModeEnabled` y la visibilidad del modo oculto en Android 15, 16 y 17.
2. Activar los flags de Private Space y comprobar que el cajón de Launcher3 muestra la sección en API 35+ y no rompe API 31–34.
3. **Criterio genérico de viabilidad**: una app real que exija cuenta (inicio de sesión, notificaciones y, si las usa, comprobaciones de integridad) funciona **en las dos copias** a la vez en el dispositivo objetivo. Si no funciona en ninguna o el Private Space no es accesible para el launcher, M9 se para y se sigue solo con M10 (`11`).
4. Clonado propio de cada marca: qué devuelve `getProfiles()`.

## Pruebas

**Automáticas**
- Unitarias de `DualAppIndex`: altas, bajas, la misma app en tres perfiles (principal, privado y trabajo: solo empareja principal↔privado), perfil bloqueado.
- Unitarias de `LaunchRouter` y `PrivateSpaceController` con perfiles simulados.

**Manuales clave**
- Bloquear el Private Space con una app de cuenta 2 abierta y volver al launcher: la insignia y el menú se actualizan al instante.
- La app del criterio de viabilidad en ambas copias: abrir, iniciar sesión y recibir notificaciones.
- Emulador API 31–34: el módulo no aparece.

## Riesgos

- **Integridad del dispositivo**: apps que exijan integridad fuerte pueden no funcionar en ROMs como GrapheneOS, en ninguna copia. No es del launcher; se comprueba en el spike con la app que vaya a usarse.
- **Cuentas múltiples**: algunas apps limitan o prohíben varias cuentas por persona, o exigen otro número de teléfono para la segunda. Revisar sus condiciones antes de usar el par en serio.
- **Huella del dispositivo**: cada perfil tiene un `ANDROID_ID` distinto, pero el hardware es el mismo; una app podría relacionar las cuentas.
- **Modo oculto**: si en 16/17 sigue sin poder leerse, el launcher podría delatar que el espacio existe. Mitigación: «ocultar pares».
- **Clonado de fabricante**: cada marca lo implementa distinto y puede no exponerlo a launchers de terceros.
- **ColorOS de China**: sin rol de inicio no hay módulo (ver arriba).
