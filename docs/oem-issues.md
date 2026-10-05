# Incidencias por fabricante

## OPPO CPH2841 — ColorOS 16.1 (Android 16) — 2026-10-04
- **Autoarranque**: las rutas candidatas de `docs/05` (`com.coloros.safecenter/...StartupAppListActivity`, `com.oppo.safe/...`) NO existen en ColorOS 16 (`cmd package resolve-activity` → «No activity found»). En su lugar hay `com.oplus.safecenter`, `com.oplus.athena` y `com.coloros.phonemanager`, sin una actividad de autoarranque localizable por nombre. Estado: abierto. Reserva actual: detalles de la app (`InstalledAppDetails`), donde ColorOS ofrece «Batería» y «Permisos». Pendiente de verificar a mano la ruta en pantalla.
- **Batería**: `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` resuelve y abre `Settings$SpaActivity`. Verificado.
- **Launcher predeterminado**: `cmd role add-role-holder` y el asistente (`RoleManager`) funcionan; el rol HOME vuelve a `com.android.launcher` al desinstalar nuestro launcher. Verificado.
- **Permiso de widgets**: al añadir un widget aparece el diálogo del sistema «¿Crear widget y permitir acceso?» (`com.android.settings/AllowBindAppWidgetActivity`) porque el launcher no es app del sistema. Esperado en launchers de terceros.
- **Gestos/recientes**: no evaluados con el launcher propio como predeterminado (pendiente).

## Huawei MatePad MRO-W09 — EMUI 14.2 (Android 12) — 2026-10-05
- **Autoarranque / gestor de arranque**: `com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity` y `.power.ui.HwPowerManagerActivity` RESUELVEN (`pm resolve-activity`) pero `startActivity` falla con `SecurityException`: exigen `com.huawei.permission.external_app_settings.USE_COMPONENT` (signature|privileged). Inusables para terceros; el asistente (`FirstRunActivity.openFirst`) captura la excepción y cae a los detalles de la app (verificado con captura). `com.hihonor.*` no existe aquí. Las rutas `.optimize.process.ProtectActivity` y `.startupmgr.ui.StartupAppListActivity` tampoco existen.
- **Batería**: `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` abre «Optimización de batería» (`Settings$HighPowerApplicationsActivity`); `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` muestra el diálogo «¿Quieres deshabilitar la optimización de la batería?» (cancelado, sin cambiar ajustes). Verificado.
- **Launcher predeterminado**: `cmd role` NO existe en este EMUI (`Unknown command: get-role-holders`); `add-role-holder` sí fija el launcher. El launcher original se lee con `pm resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN`. Si el launcher de terceros se cierra por una excepción, EMUI devuelve el inicio a `com.huawei.android.launcher` (ocurrió con el fallo de ATRÁS, parche 0090).
- **Compilación AOT**: `pm compile -m speed -f` falla con la build de este worktree (`Dex2oat ... failed with 0x0009`) aunque funcionó con el APK de `dist/` anterior; sin causa identificada (solo rendimiento de arranque).
- **Instalación**: `adb install` puede quedarse colgado si la sesión anterior no terminó; matar la sesión y reintentar.
