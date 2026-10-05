# 09 — Privacidad y funcionamiento sin GMS

## Principios

1. **Nada sale del dispositivo** salvo que el usuario lo ordene de forma explícita.
2. **Permisos mínimos**, cada uno con motivo escrito.
3. **Ninguna dependencia de Google**, ni obligatoria ni opcional.
4. **Auditable**: comprobaciones automáticas en CI y lista de revisión antes de cada versión.

## Permisos

Esta tabla es la **lista objetivo**. Hay que contrastarla con el manifiesto fusionado de la base en M0 y ajustarla (la base puede declarar permisos que aquí se quitan o necesitar alguno que falte).

| Permiso / capacidad | ¿Se declara? | Motivo |
|---|---|---|
| `INTERNET` | **No** en `default`. Sí solo en la variante `sync` | WebDAV directo |
| `ACCESS_NETWORK_STATE` | No | No hay red en `default` |
| `QUERY_ALL_PACKAGES` | No (verificado en M1) | La base lo declaraba; sin él solo se veían 5 apps, por lo que se declara `<queries>` con intent MAIN/LAUNCHER (solo apps con icono de lanzador) |
| `SET_WALLPAPER_HINTS` | Sí (añadido en M1) | `WallpaperManager` lo exige al arrancar; permiso normal |
| Acceso a notificaciones (listener) | Solo si el usuario lo activa; **desactivado por defecto** | Puntos de notificación; solo contar por paquete, sin guardar contenido |
| `BIND_APPWIDGET` | Sí (verificado 2026-10-05 en el manifiesto fusionado) | Alojar widgets de terceros; sin él cada widget pide el diálogo de enlace del sistema (también al restaurar un layout, ver `06`) |
| `REQUEST_DELETE_PACKAGES` | Sí (verificado 2026-10-05) | Acción «Desinstalar» del icono; abre el diálogo del sistema |
| Expandir barra de estado | Opcional | Gesto de deslizar para notificaciones |
| `SET_WALLPAPER` | Sí (verificado 2026-10-05) | Mostrar/cambiar el fondo; también lo usa el selector «Fondos de UltimateLauncher» (imágenes incluidas en el APK, `WallpaperManager.setBitmap` a Inicio/Bloqueo; sin red ni otros permisos) |
| `<queries>` de paquetes de iconos | Sí (2026-10-05); no son permisos | Cuatro intents con las acciones estándar de packs (`org.adw.launcher.THEMES`, `com.novalauncher.THEME`, `org.adw.launcher.icons.ACTION_PICK_ICON`, `com.anddoes.launcher.THEME`) para listar los packs YA instalados. No se descarga nada; solo se leen los recursos del pack elegido |
| `VIBRATE` | Sí | Respuesta háptica |
| `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) | Sí, lo añade androidx | Permiso propio de la app (nivel de firma, no concede nada a terceros) para receptores dinámicos no exportados |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Sí (añadido 2026-10-05) | Solo si el usuario lo pide en el asistente: abre el diálogo del sistema para que no mate el launcher en segundo plano (si no, ColorOS lo cierra y los iconos tardan en volver). Google Play limita este permiso; la distribución es fuera de Play (como UltimateDeck). Sin red, sin datos. |
| ~~`com.android.launcher.permission.READ_SETTINGS` y `com.android.launcher3.permission.READ_SETTINGS`~~ (NO declarados: propuestos por el importador, retirados a la espera de que el usuario los apruebe; la vía de archivo no los necesita) | **Solo en la variante `sync`** (añadidos 2026-10-05); la variante `default` NO los declara (prohibidos en `tools/permissions-forbidden.txt`) | «Importar de otro launcher» por proveedor de contenido: son permisos normales que declaraban los launchers derivados de Launcher3 antiguos para leer su tabla de iconos. Solo se leen a petición del usuario y nada se envía. La rama actual de AOSP usa un permiso de sistema, así que en la práctica solo sirven con launchers antiguos. Ver DECISIONS.md |
| Almacenamiento, contactos, ubicación, cámara, micrófono, notificaciones propias, biometría | **No** | No se necesitan |

Regla: **cualquier permiso nuevo requiere actualizar esta tabla** y el README público.

## Datos locales

- **Sin historial de uso**: no se guardan apps abiertas, recientes ni predicciones (no hay fila de previstas).
- La búsqueda del cajón filtra la lista en memoria y **no guarda consultas**.
- Copia en la nube del sistema desactivada para los datos del launcher; la única exportación es la que pide el usuario (ver `06`).
- Registros: en versiones de producción, sin registros con nombres de apps ni datos personales; eliminar trazas de depuración en la compilación de release.
- El **archivo de exportación lista las apps instaladas**: es información sensible. Cifrado opcional en local y por defecto en la variante `sync`.
- Sin informes de fallos automáticos. Si hace falta, un botón para **exportar un registro local** que el usuario envía a mano.

## Sin GMS

- **Prohibido** en dependencias: servicios de Google Play, Firebase, bibliotecas de Play (revisión, integridad, actualizaciones), ML Kit, fuentes remotas de Google.
- Los widgets de Google (resumen "de un vistazo", barra de búsqueda de Google) **no existen** en este proyecto y nada depende de ellos.
- Se prueba en al menos un dispositivo **sin GMS** en cada hito (firmware chino).

## Dependencias

- Solo software libre compatible con F-Droid; lista permitida en `docs/dependencies.md`.
- Cada PR de Dependabot se revisa contra esa lista.
- Escaneo de trackers con una herramienta específica (por ejemplo Exodus Privacy) **además** de la comprobación propia de paquetes prohibidos.
- Objetivo: **compilación reproducible**, para poder verificar que el APK publicado sale del código.

## Red

- La variante `default` **no tiene permiso `INTERNET`**; sin él, Android impide a la app abrir conexiones.
- Comprobación en CI: `tools/check-permissions.sh` (permisos exactos contra `tools/permissions-allowed.txt`, cada uno nombrado en esta tabla, sin clases de cliente HTTP) y `tools/check-dependencies.sh`; se ejecutan con `tools/privacy-audit.sh` desde `tools/ci.sh`. Resultado real en `docs/privacy-audit.md`. La idea básica (apkanalyzer forma parte de las cmdline-tools del SDK de Android):

```bash
# falla si la variante default declara INTERNET
if apkanalyzer manifest permissions app-default-release.apk | grep -q "android.permission.INTERNET"; then
  echo "ERROR: la variante default declara INTERNET" >&2
  exit 1
fi
```

- Variante `sync`: `applicationId` distinto, solo HTTPS, configuración de seguridad de red que **no permite tráfico sin cifrar** ni certificados de usuario salvo decisión expresa, y contraseña de aplicación en Android Keystore.
- Al ser dos apps, no comparten datos: el paso entre ellas es por archivo (decisión abierta en `08`).

## Auditoría antes de cada versión

- [ ] Manifiesto fusionado: los permisos coinciden con la tabla de esta página.
- [ ] La variante `default` no declara `INTERNET` (comprobación de CI en verde).
- [ ] Sin dependencias prohibidas ni trackers detectados.
- [ ] Sin servicios de Google: funciona en un dispositivo sin GMS.
- [ ] Prueba de 24 horas con la variante `default` bajo un cortafuegos por app o captura en el router: cero conexiones.
- [ ] Sin registros con datos personales en la versión de producción.
- [ ] Las exportaciones no contienen credenciales ni URL de servidores.
- [ ] El acceso a notificaciones está desactivado en una instalación nueva.
- [ ] README público actualizado con la tabla de permisos.

## Transparencia

- README con la tabla de permisos y qué hace cada uno, sin cuentas ni registro.
- Sin servicios de terceros en la web del proyecto.
- Cualquier cambio que afecte a la privacidad queda anotado en el registro de cambios de la versión.
