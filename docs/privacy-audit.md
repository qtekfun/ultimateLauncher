# Auditoría de privacidad (docs/09) — resultado de ejecutarla

Ejecución real: 2026-10-05, sobre `master` + los cambios de M7/M8 (APK `release` de `default` y `sync` compilados en la misma sesión; no minificados, firmados con la clave de depuración). Comando: `tools/privacy-audit.sh` (lo llama `tools/ci.sh`). Resultado: **AUDITORÍA SUPERADA**. Lint (`:app:lintDefaultDebug`, `NewApi` = error) y las pruebas unitarias (`:app:testDefaultDebugUnitTest`, 0 fallos) pasaron en la misma tanda.

## Qué comprueba la parte automática

| Comprobación | Herramienta | Resultado |
|---|---|---|
| `default` sin `INTERNET`, ni `ACCESS_NETWORK_STATE`/`ACCESS_WIFI_STATE`, ni almacenamiento, contactos, ubicación, cámara, micrófono ni notificaciones | `tools/check-permissions.sh` + `tools/permissions-forbidden.txt` | OK |
| Permisos del manifiesto fusionado = lista cerrada `tools/permissions-allowed.txt` (ni uno más ni uno menos) | idem | OK, 7 (ver abajo) |
| Cada permiso permitido está nombrado en la tabla de `docs/09` | idem | OK (la primera ejecución falló: la tabla decía «lo que exija Launcher3» o el nombre en lenguaje natural; se concretaron `BIND_APPWIDGET`, `REQUEST_DELETE_PACKAGES`, `SET_WALLPAPER`, `VIBRATE` y se añadió el permiso propio de androidx) |
| Manifiesto: `allowBackup` ≠ true, sin cleartext, sin `QUERY_ALL_PACKAGES`, sin referencias a GMS/Firebase/Play | idem | OK |
| Código de `default`: ninguna clase de cliente de red (`HttpURLConnection`, `HttpsURLConnection`, `java.net.http`, `android.net.http`, okhttp, retrofit, volley, grpc, cronet, GMS, Firebase, ML Kit…) definida ni referenciada | `apkanalyzer dex packages` + `tools/forbidden-classes.txt` | OK. Informativo: se referencian `java.net.Socket` y `java.net.URL` (vienen de AndroidX/Guava/protobuf); sin `INTERNET` el sistema impide abrir conexiones |
| `sync` declara exactamente la lista + `INTERNET` | idem | OK |
| Dependencias resueltas de `defaultReleaseRuntimeClasspath`: ningún artefacto prohibido y ningún grupo fuera de la lista revisada | `tools/check-dependencies.sh` + `tools/forbidden-deps.txt` + `tools/allowed-dependency-groups.txt` | OK: 160 artefactos, 60 grupos (androidx.*, material, dagger, guava, protobuf-javalite, kotlin/kotlinx, jakarta/javax.inject, jspecify y anotaciones) |
| El serializador de layout (`LayoutModel.kt`, `LayoutStore.kt`) no menciona contraseñas, tokens ni URL | `grep` | OK |

### Permisos reales de `default` (manifiesto fusionado)
`BIND_APPWIDGET`, `REQUEST_DELETE_PACKAGES`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SET_WALLPAPER`, `SET_WALLPAPER_HINTS`, `VIBRATE`, y el propio `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (lo añade androidx). La variante `sync` añade solo `INTERNET`.

### Observaciones
- `androidx.appsearch` y `androidx.slice` están en el árbol de dependencias (los arrastra AOSP/appfunctions). Son almacenamiento local y vistas, sin red; el servicio de AppFunctions no está en el manifiesto (decisión previa en `docs/DECISIONS.md`).
- La comprobación de la cadena de dependencias es por **grupo**; la revisión una a una de licencias sigue pendiente (`docs/dependencies.md`).
- Los cambios en la tabla de `docs/09` no alteran el comportamiento: documentan lo que el manifiesto ya declaraba.

## Casillas de «Auditoría antes de cada versión» (`docs/09`)

| Casilla | Estado |
|---|---|
| Manifiesto fusionado: permisos = tabla | **Hecho** (automático) |
| `default` sin `INTERNET` | **Hecho** (automático) |
| Sin dependencias prohibidas ni trackers detectados | **Parcial**: dependencias y clases, automático; escáner externo de trackers (Exodus) NO ejecutado |
| Funciona sin GMS | **No re-verificado en esta tarea** (sin dispositivo; en hitos anteriores el OPPO CPH2841 no usa GMS en pruebas) |
| Prueba de 24 h con cortafuegos: cero conexiones | **No hecho** (requiere dispositivo; ver nota: sin `INTERNET` la app no puede abrir sockets, el cortafuegos sería una confirmación independiente) |
| Sin registros con datos personales en producción | **No auditado** (`ULDIAG` está solo en debug por el parche 0044; no se ha repasado el resto de `Log.*`/`FileLog`) |
| Exportaciones sin credenciales ni URL | **Hecho** (automático para el serializador; el cifrado opcional AES-256-GCM existe desde M7, `docs/06`) |
| Acceso a notificaciones desactivado en instalación nueva | **No re-verificado** (sin dispositivo) |
| README con tabla de permisos | Hecho (ya estaba; coincide con la lista) |

## Cómo repetirlo
```bash
tools/privacy-audit.sh --build     # compila default y sync release y audita
tools/ci.sh                        # compilación + pruebas + Lint + auditoría (OFFLINE=1 para --offline)
tools/check-permissions.sh dist/ultimatelauncher-default-VERSION.apk   # un APK ya firmado
```
Un permiso nuevo hace fallar la auditoría hasta que se actualicen `tools/permissions-allowed.txt`, la tabla de `docs/09` y el README.
