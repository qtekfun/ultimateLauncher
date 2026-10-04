# Incidencias por fabricante

## OPPO CPH2841 — ColorOS 16.1 (Android 16) — 2026-10-04
- **Autoarranque**: las rutas candidatas de `docs/05` (`com.coloros.safecenter/...StartupAppListActivity`, `com.oppo.safe/...`) NO existen en ColorOS 16 (`cmd package resolve-activity` → «No activity found»). En su lugar hay `com.oplus.safecenter`, `com.oplus.athena` y `com.coloros.phonemanager`, sin una actividad de autoarranque localizable por nombre. Estado: abierto. Reserva actual: detalles de la app (`InstalledAppDetails`), donde ColorOS ofrece «Batería» y «Permisos». Pendiente de verificar a mano la ruta en pantalla.
- **Batería**: `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` resuelve y abre `Settings$SpaActivity`. Verificado.
- **Launcher predeterminado**: `cmd role add-role-holder` y el asistente (`RoleManager`) funcionan; el rol HOME vuelve a `com.android.launcher` al desinstalar nuestro launcher. Verificado.
- **Permiso de widgets**: al añadir un widget aparece el diálogo del sistema «¿Crear widget y permitir acceso?» (`com.android.settings/AllowBindAppWidgetActivity`) porque el launcher no es app del sistema. Esperado en launchers de terceros.
- **Gestos/recientes**: no evaluados con el launcher propio como predeterminado (pendiente).
