# 06 — Sincronización de la disposición

## Objetivo

Al estrenar un teléfono: instalar UltimateLauncher, restaurar y obtener la misma disposición que en el anterior, con las adaptaciones necesarias.

## Qué se sincroniza

- Páginas de inicio: posición de apps, accesos, carpetas (con su contenido y orden) y widgets (proveedor, tamaño, posición).
- Dock.
- Ajustes del launcher: rejilla, tamaño de iconos, forma de icono, pack de iconos elegido (por paquete), perfil de animación, tokens de tema, fuente elegida.
- Perfil activo de animaciones y multiplicador de velocidad.

Fuera: datos de los widgets (los guarda cada app), fondos de pantalla, perfil de trabajo (su contenido lo gestiona el sistema).

## Formato (v1)

```json
{
  "schema": 1,
  "createdAt": "ISO-8601",
  "device": { "class": "phone|tablet", "brand": "", "model": "", "androidApi": 0, "density": 0, "widthDp": 0, "heightDp": 0 },
  "settings": { "grids": { "phone": { "columns": 0, "rows": 0 }, "phoneLandscape": { "columns": 0, "rows": 0 }, "tablet": { "columns": 0, "rows": 0 }, "tabletLandscape": { "columns": 0, "rows": 0 } }, "iconPack": null, "iconShape": "", "animationProfile": "oppo-medido", "speedMultiplier": 1.0, "theme": "oppo-medido" },
  "hotseat": [ { "slot": 0, "item": { } } ],
  "pages": [
    { "index": 0, "items": [
      { "type": "app",    "package": "", "activity": "", "profile": "personal|work", "cell": { "x": 0, "y": 0 } },
      { "type": "folder", "title": "", "cell": { "x": 0, "y": 0 }, "items": [ ] },
      { "type": "widget", "provider": "pkg/class", "cell": { "x": 0, "y": 0 }, "span": { "w": 0, "h": 0 } },
      { "type": "shortcut", "package": "", "id": "", "cell": { "x": 0, "y": 0 } }
    ] }
  ]
}
```

Reglas del formato:
- Versionado con `schema`; el importador rechaza una versión mayor que la que conoce y migra las menores.
- No incluye datos personales aparte de lo que hay en el layout. No incluye contraseñas ni la URL de WebDAV.
- **El archivo lista las apps instaladas, que es información sensible**: se trata como tal (cifrado opcional en local y por defecto en la variante `sync`).

### Cifrado (implementado en M7, `LayoutCrypto.kt`)
- Opcional al exportar: si se deja la frase de paso vacía, el archivo queda en claro; si no, se escribe un **sobre JSON versionado**: `{"ulEncrypted":1,"cipher":"AES-256-GCM","kdf":"PBKDF2WithHmacSHA256","iterations":600000,"salt":"b64","iv":"b64","data":"b64"}`.
- Clave de 256 bits derivada con PBKDF2-HMAC-SHA256 (600 000 iteraciones por defecto, sal de 16 bytes aleatoria por archivo); IV de GCM de 12 bytes aleatorio; etiqueta de 128 bits. Los campos de cabecera (versión, algoritmo, iteraciones, sal, IV) entran como datos autenticados (AAD): manipularlos hace fallar el descifrado.
- Solo APIs del JDK/Android (`javax.crypto`), sin librerías. Argon2 no existe en la plataforma; PBKDF2 es lo disponible sin dependencias (es la razón de las 600 000 iteraciones).
- El importador detecta el sobre por el campo `ulEncrypted`, pide la frase y distingue «frase incorrecta o archivo alterado» (indistinguibles por diseño en GCM) de «sobre de versión mayor / algoritmo desconocido / iteraciones fuera de [100 000, 5 000 000]» (un archivo manipulado no puede rebajar el coste de la clave).
- No se guarda la frase en ningún sitio. La copia automática previa a importar (`filesDir/backups/`) queda en claro dentro del almacenamiento privado de la app (sin copia en la nube del sistema).
- Pendiente: «cifrado por defecto» en la variante `sync` (no hay variante `sync` implementada, ver abajo); contraseñas de otra longitud mínima / indicador de fortaleza.

## Importación en otro teléfono (RF-52)

1. **Apps no instaladas**: omitir el icono y anotarlo en un informe final ("faltan estas apps"); opcionalmente dejar un hueco.
2. **Widgets cuyo proveedor no existe**: omitir y anotar. Los widgets propios de otra marca se sustituyen por un hueco vacío.
   - **Widgets cuyo proveedor (paquete/clase exactos) sí existe**: se restauran como en una restauración de copia de Android: se escribe la fila con `appWidgetId=-1` y `FLAG_ID_NOT_VALID|FLAG_PROVIDER_NOT_READY`, en la celda reubicada y con su tamaño (recortado a la rejilla). Al recargar, `WidgetInflater` intenta reservar el id y enlazarlo. Como el launcher declara `BIND_APPWIDGET` pero este permiso solo se concede a apps privilegiadas, lo normal es que el enlace automático no se conceda: el widget queda **pendiente** y al tocarlo el sistema muestra su diálogo de enlace/configuración (sin permisos nuevos).
   - **No se restaura**: el contenido ni la configuración del widget (los guarda cada app; si el widget necesita configurarse, el sistema lanza su actividad de configuración al enlazar), ni un widget del mismo paquete pero con otra clase de proveedor (se omite y se anota). Los widgets en el dock no se importan. No verificado en dispositivo.
3. **Rejilla distinta**: reubicar manteniendo orden de lectura; los widgets se reajustan a las celdas posibles.
4. **Perfil de trabajo ausente**: las apps de trabajo se omiten con aviso.
5. **Ajustes que dependen de capacidades** (blur, frecuencia): se aplican con el valor de reserva y se avisa.
6. Mostrar siempre un **resumen** antes de aplicar (qué se importará y qué no) y permitir cancelar.
7. Hacer una copia automática del layout actual antes de importar.
8. **Cambio de clase de pantalla (móvil ↔ tablet)**: reubicar manteniendo el orden de lectura sobre la rejilla de destino, repartir páginas si hace falta y reajustar widgets; el resumen del paso 6 indica qué cambió.

## Transporte

Por privacidad, la compilación por defecto **no declara permiso de red** (ver `09`). Por eso hay dos caminos:

### 1. Archivo local / selector de documentos (por defecto, sin red)
- Exportar e importar con el selector de documentos del sistema (Storage Access Framework).
- Sirve cualquier proveedor de documentos que el usuario tenga: almacenamiento local, Syncthing, o la app de Nextcloud si expone sus archivos en el selector. Que la app de Nextcloud aparezca en el selector del sistema se deduce de su documentación y de incidencias públicas que mencionan problemas puntuales; **hay que verificarlo en los dispositivos**. La sincronización la hace esa otra app; UltimateLauncher no toca la red.
- Es el camino por defecto para pasar la disposición entre dispositivos.

### 2. WebDAV directo (variante opcional `sync`)
- Solo en una variante de compilación separada que declara `INTERNET` (con `applicationId` distinto, por ejemplo con sufijo `.sync`). **Nunca** en la variante por defecto.
- Configuración: URL base, usuario y **contraseña de aplicación** de Nextcloud, guardada en Android Keystore.
- Solo HTTPS, sin aceptar certificados inválidos y sin seguir redirecciones a otro host sin confirmar.
- Operaciones: `PUT`, `GET` y `PROPFIND`; cliente ligero sin dependencias de Google.
- Cifrado del JSON con frase de paso **activado por defecto** (AES-GCM con clave derivada).
- Al ser otra app instalada, no comparte datos con la variante por defecto: el paso entre ambas es por archivo.

### Política de conflictos
- Sin fusión automática: todo el layout se trata como una unidad.
- Mostrar fecha y dispositivo de origen de `latest.json`; el usuario elige **subir**, **bajar** o **cancelar**.
- Sincronización siempre manual en v1 (botón); la automática, más adelante.

## Pruebas

- Unitarias: ida y vuelta (exportar → importar) devuelve el mismo estado en el mismo dispositivo.
- Unitarias (`LayoutCryptoTest`): ida y vuelta cifrada, frase errónea, texto cifrado o cabecera alterados, iteraciones rebajadas, versión de sobre mayor, frase vacía.
- Unitarias: adaptación de rejilla con casos 4x6 → 5x6, 5x6 → 4x5, y con widgets grandes.
- Integración (variante `sync`): subida y descarga contra un Nextcloud de prueba o un servidor WebDAV local.
- Comprobar que la variante por defecto no declara `INTERNET` (ver `09`).
- Manual: exportar en un dispositivo de la matriz e importar en otro de otra marca.

## Importar de otro launcher (`importer/`, «Traer mi pantalla de inicio»)

Asistente `ForeignImportActivity`, enlazado desde «Exportar/importar disposición» y desde el paso opcional 4 del asistente de primer arranque. Sin red. Reutiliza `ImportPlanner` (instalado/no instalado, widgets solo con proveedor exacto, dock) y `GridReflow` (rejilla distinta), y `LayoutStore.apply` (copia automática previa en `files/backups/`). Antes de aplicar se muestra «Se importarán N apps, M carpetas (K apps dentro), W widgets y D del dock en P página(s). X omitidos» más las razones.

| Vía | Estado | Detalle |
|---|---|---|
| Archivo de copia por el selector de documentos (SAF, sin permisos) | **Implementada** | Se detecta por los primeros bytes (no por la extensión): SQLite suelto, ZIP (`.novabackup`, `.lawnchairbackup`) o JSON. En el ZIP se buscan bases SQLite (hasta 4 candidatas, `launcher.db` primero) y se prueba cada una hasta encontrar una con tabla `favorites`. Límites: 64 MB por archivo, 128 MB descomprimidos, 500 entradas. Estructura interna de Nova y Lawnchair **no verificada** (no está documentada oficialmente; el rastreo por contenido evita depender de nombres) |
| Proveedor de contenido `content://<autoridad>/favorites` de launchers instalados | **Implementada, casi siempre bloqueada** | La rama `android17-release` protege su proveedor con `android.permission.ACCESS_LAUNCHER_DATA` (de sistema): una app normal no puede obtenerlo. El asistente lee `ProviderInfo.readPermission` y, si no lo tenemos, lo dice y remite al archivo de copia. Solo launchers antiguos o forks con permiso normal (`com.android.launcher[3].permission.READ_SETTINGS`) responden, y esos permisos solo se declaran en la variante `sync` (ver `09` y DECISIONS) |
| Export propio de UltimateLauncher | Ya existía | El selector reconoce el JSON y remite a «Exportar/importar» |
| Launchers cerrados de fabricante (ColorOS, MIUI/HyperOS, EMUI, MagicOS, vivo, One UI) | **Descartada** | No exponen nada sin privilegios; descompilarlos está prohibido. El asistente lo explica y propone alternativas (copia propia del launcher si existe, capturas, conservar el móvil antiguo) |
| Paquete de iconos elegido | **Descartada** | Los formatos de copia no lo documentan y este launcher no gestiona paquetes de iconos; el asistente avisa de que se vuelva a elegir |
| Captura guiada de apps por orden | No implementada | Solo descrita como alternativa manual |

Datos en `assets/launcher-import-sources.json` (como `oem-intents.json`): paquetes públicos, autoridades por convención de AOSP (`<paquete>.settings`) y vías con `verified=false` hasta probarlas con el launcher real.

Mapeo (`ForeignLayoutParser`): `itemType` 0 → app; 1 → app si el intent es MAIN/LAUNCHER con componente (así guardaban las apps launchers viejos) y si no acceso directo (se omite y se cuenta); 2 → carpeta (hijos por `rank`, máx. 100); 4 → widget con proveedor exacto; 6 → acceso profundo (se omite); resto → «no soportado». `container` -100 escritorio, -101 dock (hueco = `screen`), >0 hijos de carpeta, resto (predicciones, cajón) se ignora. Páginas ordenadas por `workspaceScreens.screenRank` si existe o por `screen`. La rejilla de origen no se guarda en la base: se infiere del máximo de celdas usadas. `profileId` ≠ 0 se marca como perfil de trabajo (se omite). Límites: 20 000 filas, 40 páginas, celdas 0..63, spans 1..16, títulos 64 caracteres, intents 4096; todo lo que los supere o esté corrupto se cuenta como «omitido» y nunca se confía.

Pruebas: `ForeignImportTest` (16) con bases SQLite sintéticas generadas en la propia prueba (JDBC solo en `testImplementation`).
