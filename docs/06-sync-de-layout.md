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

## Importación en otro teléfono (RF-52)

1. **Apps no instaladas**: omitir el icono y anotarlo en un informe final ("faltan estas apps"); opcionalmente dejar un hueco.
2. **Widgets cuyo proveedor no existe**: omitir y anotar. Los widgets propios de otra marca se sustituyen por un hueco vacío.
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
- Unitarias: adaptación de rejilla con casos 4x6 → 5x6, 5x6 → 4x5, y con widgets grandes.
- Integración (variante `sync`): subida y descarga contra un Nextcloud de prueba o un servidor WebDAV local.
- Comprobar que la variante por defecto no declara `INTERNET` (ver `09`).
- Manual: exportar en un dispositivo de la matriz e importar en otro de otra marca.
