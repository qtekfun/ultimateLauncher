# 10 — Dual apps (v2)

Estado: **implementado** (M9) y probado en un Pixel 8 con Android 17 — 2026-10-12. Código en `app/.../dualapps/`, parche `0220`. Resultados del spike y de la prueba en *Verificado en dispositivo* al final.

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
| Private Space exige `ACCESS_HIDDEN_PROFILES` + rol `ROLE_HOME` | Sin ser launcher por defecto no se ve el espacio. El asistente ya pide fijarlo (RF-40). **Permiso normal** (verificado: `prot=normal` en Android 17); declarado en el manifiesto y en `09`. Sin él, el launcher no ve el perfil privado |
| `requestQuietModeEnabled(false, user)` desde una app de terceros | Funciona siendo launcher por defecto, sin permiso extra. El sistema pide la credencial; si el dispositivo **no tiene bloqueo de pantalla**, pide definir uno (el Private Space lo exige) |
| AOSP marca las apps del Private Space como **no fijables** (`FLAG_NOT_PINNABLE` en `AppInfo`) | Ni «Añadir a inicio» ni arrastrar al escritorio. El parche 0220 lo permite mientras Dual apps esté activo |
| Con el espacio bloqueado, Android **no** pide desbloqueo al abrir una app privada (`UnlaunchableAppActivity` solo atiende perfiles de trabajo) | El launcher pide el desbloqueo y abre la app al recibir `ACTION_PROFILE_AVAILABLE` |
| Un solo Private Space por dispositivo, solo en el usuario principal | Máximo dos copias por app. Si el launcher corre en un usuario secundario, Dual apps se desactiva |
| Flags aconfig de Launcher3 todos a `false` (`tools/gen-flags.py`, DECISIONS 2026-10-04) | `PrivateProfileManager` y la sección de Private Space del cajón dependen de `Flags.enablePrivateSpace()` y afines. Hay que activarlos (parche en `/patches`) y probar que no rompen API 31–34 |
| Modo «oculto» del Private Space (`hide_privatespace_entry_point`) no legible por terceros | Verificado en Android 17: `Settings.Secure` lanza `SecurityException` (y la base de AOSP **cerraba el launcher** al bloquear el espacio; corregido en el parche 0220 con el valor por defecto). Se trata como desconocido: opción «ocultar pares». La pantalla de ajustes sí se puede abrir: `LauncherApps.getPrivateSpaceSettingsIntent()` es API pública (la usa el botón de ajustes del cajón) |
| ColorOS de China no deja fijar el launcher como inicio (`oem-issues.md`, PMX110) | Sin `ROLE_HOME` no hay Private Space: **Dual apps no está disponible** en ese firmware. Aceptado de momento (2026-10-12) |

Fuente: documentación de Private Space de AOSP (source.android.com).

## Componentes

Código propio en `app/src/main/java/com/qtekfun/ultimatelauncher/dualapps/`; sobre AOSP solo los ganchos del parche `0220`. El plan inicial separaba `ProfileRepository`, `DualAppIndex`, `LaunchRouter` y `PrivateSpaceController`; al implementarlo bastó con menos piezas, porque la base ya mantiene los perfiles (`UserCache`), su tipo (principal, trabajo, privado, clonado) y el estado bloqueado.

- **DualAppsLogic** (sin Android, con pruebas unitarias): tipos de perfil, qué perfil tiene la otra copia (principal ↔ privado, o principal ↔ clonado; nunca trabajo), elección de actividad, umbral del gesto y formato de la última app usada.
- **DualApps**: ajustes, búsqueda de la otra copia con `LauncherApps.getActivityList(paquete, perfil)` (bajo demanda, sin tabla persistente), lanzamiento con `startMainActivity`, desbloqueo con `requestQuietModeEnabled(false, user)` + `ACTION_PROFILE_AVAILABLE`, «Añadir al escritorio» y la última app emparejada abierta.
- **OpenOtherCopyShortcut / AddOtherCopyShortcut**: filas del menú contextual.
- **DualSwipeController**: gesto de deslizar hacia arriba sobre un icono del par.
- **DualAppTileService + DualLaunchActivity**: mosaico «Abrir cuenta 2».

## Interacción

- **Iconos** (decidido 2026-10-12): **dos en el escritorio**, uno por copia. La copia principal lleva su icono normal; la del Private Space, el mismo icono con la **insignia del sistema** para el espacio privado (la que Android pone a sus apps, también en el cajón). Es «un pequeño símbolo» como se pidió, no es de ningún fabricante y no hay que mantener un dibujo propio. Una insignia «2» propia queda como mejora opcional (medidas `null`).
- **Añadir al escritorio**: desde el cajón (sección de Private Space: «Añadir a pantalla de inicio» o arrastrar) o desde el menú del icono principal («Añadir cuenta 2 al escritorio», que la coloca en la primera celda libre por el mismo camino que AOSP). La cola de instalación de Launcher3 no sirve: descarta las apps del sistema.
- **Toque**: cada icono abre **su** copia.
- **Mantener pulsado**: el menú contextual (parche 0150) de cualquiera de los dos iconos añade arriba «Abrir en la otra cuenta».
- **Deslizar hacia arriba sobre un icono del par**: abre la otra copia. **Entra en v2** (decidido 2026-10-12); se puede desactivar en Ajustes.
- **Cajón**: se mantiene la sección de Private Space de Launcher3; las apps emparejadas no se duplican fuera de ella.
- **Ajustes** (Ajustes de inicio > «Apps dobles», solo API 35+): interruptor global, gesto de deslizar y «ocultar pares». La lista de pares y el estilo de la insignia quedan pendientes (mejora; hoy los pares se calculan solos).
- **Mosaico** «Abrir cuenta 2»: abre la otra copia de la última app emparejada abierta y, al volver a pulsarlo, alterna. Un mosaico solo puede abrir actividades con `startActivityAndCollapse(PendingIntent)`, así que abre una actividad propia invisible y no exportada (`DualLaunchActivity`) que lanza la copia.

## Bloqueo y privacidad

- Con el espacio bloqueado, el icono con insignia se muestra **en gris** (lo hace AOSP con `FLAG_DISABLED_QUIET_USER`, igual que una app de trabajo en pausa) y el toque pide desbloqueo y abre la app al desbloquear. El atajo, el gesto y el mosaico hacen lo mismo. Decidido 2026-10-12 (descartado: quitarlos del escritorio mientras esté bloqueado).
- El modo oculto del sistema **no se puede leer** (verificado), así que el launcher no sabe si el usuario lo ha activado. La opción manual **«ocultar pares»** quita los accesos a la otra copia (menú, gesto, mosaico), impide fijar nuevas apps privadas y olvida la última app usada. Los iconos privados que ya estén en el escritorio se quedan (en gris si está bloqueado): quitarlos es decisión del usuario, igual que con «Apps ocultas».
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
- `DualAppsLogicTest`: altas, bajas, la misma app en tres perfiles (principal, privado y trabajo: solo empareja principal↔privado), perfil bloqueado, privado antes que clonado, elección de actividad, «Añadir» solo desde la principal, umbral del gesto, API mínima y valor guardado dañado.
- Lo que depende de Android (lanzar, desbloquear, menú, gesto, mosaico) se prueba a mano: ver *Verificado en dispositivo*.

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

## Verificado en dispositivo (Pixel 8, Android 17 `CP3A.260905.009`, 2026-10-12)

Private Space de prueba creado con `pm create-user --profileOf 0 --user-type android.os.usertype.profile.PRIVATE` y borrado al terminar.

| Prueba | Resultado |
|---|---|
| Sin `ACCESS_HIDDEN_PROFILES` | El launcher no ve el perfil privado |
| Con el permiso y `enable_private_space` | Sección «Privado» en el cajón con «Bloquear», apps con la insignia del sistema |
| Menú de la copia principal | «Abrir en la otra cuenta» y «Añadir cuenta 2 al escritorio» arriba |
| «Añadir cuenta 2 al escritorio» | El icono privado aparece en la primera celda libre |
| Bloquear el espacio | El icono privado del escritorio pasa a gris al instante. Antes del arreglo de `SettingsCache`, el launcher se cerraba aquí |
| Tocar el icono gris | Se pide el desbloqueo y se abre la app. Con el perfil de prueba (sin `user_setup_complete`) el desbloqueo es directo; con el dispositivo sin bloqueo de pantalla, el sistema pide definir uno |
| Menú del icono privado | Solo «Abrir en la otra cuenta» (no ofrece añadir) y abre la copia principal |
| Mosaico (`cmd statusbar click-tile`) | Abre la copia privada y, al repetir, la principal |
| Deslizar hacia arriba sobre la copia principal | Abre la copia privada; en una zona vacía sigue abriendo el cajón |
| «Ocultar pares» | Desaparece «Abrir en la otra cuenta»; al desactivarlo vuelve |

**Sin verificar**: desbloqueo con credencial real (el móvil de prueba no tiene bloqueo de pantalla y no se le puso uno), perfiles clonados de AOSP y de fabricantes (`getProfiles()` en OPPO/vivo/Xiaomi/Honor), que el módulo no aparezca en API 31–34 (solo por código: compuerta `SDK_INT >= 35` y Lint), y el criterio de viabilidad con una app real con cuenta en las dos copias.

