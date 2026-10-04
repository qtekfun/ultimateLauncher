# 04 — Estética y animaciones estilo OPPO

## Punto de partida (verificado)

OPPO **no ha publicado** el código del launcher ni de sus animaciones. Su código abierto es el kernel y componentes con licencia GPL/LGPL. Lo que hay en GitHub sobre "launcher de ColorOS" son imitaciones de terceros (por ejemplo un clon en Flutter de una versión antigua) y módulos que reempaquetan el launcher original, que **no se usan** en este proyecto.

Consecuencia: la estética y los tiempos se **miden desde fuera** en teléfonos reales y se guardan como datos (JSON). No se descompila ni se copia nada de OPPO.

## Reglas de limpieza (ver también `08`)

- No incluir **código, recursos, iconos, fondos, fuentes, sonidos ni marcas** de OPPO/ColorOS.
- El nombre de la app, el icono y los textos no deben sugerir que es de OPPO.
- Solo se replican **parámetros observables** (tiempos, curvas, medidas, proporciones).

## A. Tokens de diseño (`ThemeTokens`)

Formato: JSON versionado en `assets/themes/<id>.json`. Valores a **medir** en un OPPO real; hasta entonces `null`. Rejilla, tamaños y dock se definen **por clase de pantalla** (`phone`, `phone-landscape`, `tablet`, `tablet-landscape`): el bloque siguiente es el de una clase y el archivo real lo repite por clase.

```json
{
  "id": "oppo-medido",
  "version": 1,
  "source": { "device": "<modelo>", "os": "<ColorOS x.y>", "androidApi": 0, "date": "YYYY-MM-DD" },
  "grid": { "columns": null, "rows": null, "dockColumns": null },
  "icon": { "sizeDp": null, "shape": "squircle|circle|rounded", "cornerRadiusRatio": null, "labelSizeSp": null, "labelLines": 1 },
  "dock": { "heightDp": null, "cornerRadiusDp": null, "background": "blur|scrim|solid", "blurRadiusDp": null, "scrimAlpha": null },
  "drawer": { "background": "blur|scrim|solid", "blurRadiusDp": null, "scrimAlpha": null, "searchBar": "top|bottom", "scrollbar": "alphabet|thin" },
  "folder": { "previewGrid": "3x3", "cornerRadiusDp": null, "openStyle": "expand|popup", "background": "blur|scrim" },
  "pageIndicator": { "style": "dots|line", "sizeDp": null },
  "wallpaper": { "scrollEffect": "none|parallax", "depth": false }
}
```

### Protocolo de medición de estética

1. Teléfono OPPO de referencia, ajustes por defecto, tema claro y oscuro, sin tema de iconos de terceros.
2. Capturas de pantalla (inicio, cajón, carpeta abierta, carpeta cerrada, dock, búsqueda del cajón) en 1:1.
3. Anotar la densidad (`adb shell wm density`) y el tamaño (`adb shell wm size`) para convertir píxeles a dp.
4. Medir con una regla sobre las capturas: tamaño de icono, separación, altura del dock, radio de esquinas, tamaño de etiqueta.
5. Rellenar el JSON y revisar **lado a lado** con la app propia hasta que no se distingan a simple vista.
6. Repetir en un segundo modelo OPPO para saber qué cambia con el tamaño de pantalla, y en una tablet si se dispone de ella.

### Blur

Usar el desenfoque de ventana del sistema (la API existe desde Android 12, el `minSdk`, pero cada dispositivo o ROM puede desactivarlo: comprobar con `WindowManager.isCrossWindowBlurEnabled`) y caer a un velo semitransparente cuando no. Algunas ROMs lo desactivan en modo ahorro de batería o en gama baja.

### Tipografía

Usar la fuente del sistema del teléfono (no incluir fuentes de OPPO). Permitir al usuario elegir otra. La uniformidad entre marcas será por tamaños y espaciados, no por tipo de letra.

## B. Perfil de animaciones (`AnimationProfileProvider`)

### Modelo

Cada evento de movimiento se define como **muelle** o como **curva con duración**. Hay un multiplicador global de velocidad.

```json
{
  "id": "oppo-medido",
  "version": 1,
  "speedMultiplier": 1.0,
  "events": {
    "home.pageScroll":   { "type": "spring", "dampingRatio": null, "stiffness": null },
    "home.pageSnap":     { "type": "spring", "dampingRatio": null, "stiffness": null },
    "drawer.open":       { "type": "spring", "dampingRatio": null, "stiffness": null },
    "drawer.close":      { "type": "spring", "dampingRatio": null, "stiffness": null },
    "folder.open":       { "type": "easing", "durationMs": null, "bezier": [null, null, null, null] },
    "folder.close":      { "type": "easing", "durationMs": null, "bezier": [null, null, null, null] },
    "icon.pressScale":   { "type": "spring", "dampingRatio": null, "stiffness": null, "scale": null },
    "icon.drag.pickup":  { "type": "spring", "dampingRatio": null, "stiffness": null, "scale": null },
    "widget.resize":     { "type": "spring", "dampingRatio": null, "stiffness": null },
    "app.launch":        { "type": "easing", "durationMs": null, "bezier": [null, null, null, null] },
    "app.returnHome":    { "type": "easing", "durationMs": null, "bezier": [null, null, null, null] }
  }
}
```

Perfiles incluidos desde el principio:
- `aosp-por-defecto`: los valores originales de Launcher3 (extraerlos de la rama, **no inventarlos**).
- `oppo-medido`: rellenar tras la medición.
- `rapido`: `oppo-medido` con `speedMultiplier` menor que 1.

### Dónde enganchar

En Launcher3 las duraciones, los interpoladores y los muelles están repartidos por varias clases (gestor de transiciones, utilidades de animación, estados del launcher, carpetas, cajón). El hito M5 empieza con un inventario (`/docs/anim-inventory.md`) de **cada** constante y su clase, antes de tocar nada, y después las redirige al proveedor.

### Alcance realista sin root

- Se puede igualar: página de inicio, cajón, carpetas, pulsación y arrastre de iconos, redimensionado de widgets.
- Se iguala solo en parte: abrir una app desde su icono (limitado por `ActivityOptions`).
- No se puede igualar: volver a inicio con gesto y menú de recientes (los gestiona el launcher del sistema).

## C. Protocolo de medición de animaciones (herramienta `tools/anim-measure`)

Objetivo: obtener, para cada evento, la **curva de posición/escala/opacidad en el tiempo** y ajustarle un muelle o una curva.

### Preparación
1. Misma ROM y ajustes que en la medición de estética. Comprobar que las escalas de animación están a 1.0: `adb shell settings get global window_animation_scale`, `transition_animation_scale`, `animator_duration_scale`.
2. Fijar la frecuencia de pantalla (60 o 120 Hz) y anotarla; medir en ambas si es posible.
3. Teléfono cargado y sin modo ahorro de batería.

### Captura (en orden de preferencia)
1. **Grabación de pantalla** con `scrcpy --record` o `adb shell screenrecord`; sirve para eventos grandes (abrir cajón, carpetas). La resolución temporal queda limitada a la tasa de la grabación.
2. **Cámara de alta velocidad** (240 fps de otro teléfono, apuntando a la pantalla) cuando la grabación del sistema no tenga precisión suficiente.
3. **Perfetto** (`adb shell perfetto` con categorías de gráficos, ventanas y vistas) para obtener inicio y fin de las transiciones de ventana y los fotogramas entregados. Sirve para duraciones, no para la forma de la curva.

### Análisis
1. Extraer fotogramas y seguir el elemento (caja contenedora del icono, panel del cajón, carpeta) con visión por computador (Python + OpenCV).
2. Convertir a magnitud normalizada 0→1 respecto al tiempo desde el primer fotograma con movimiento.
3. Ajustar:
   - **Muelle**: modelo de oscilador amortiguado, obteniendo razón de amortiguamiento y rigidez (`scipy.optimize.curve_fit`).
   - **Curva**: duración (primer a último fotograma con movimiento) y bézier cúbica de mejor ajuste.
4. Elegir muelle o curva según el error de ajuste. Guardar el error y el número de repeticiones.
5. Repetir **5 veces por evento** y usar la mediana.
6. Volcar al JSON del perfil, con `source` (modelo, ROM, frecuencia, fecha).

### Validación
Prueba lado a lado: dos teléfonos (OPPO original y el que corre UltimateLauncher) grabados a la vez; comparar las curvas superpuestas. Aceptación: diferencia de duración menor de 1 fotograma y forma indistinguible a simple vista.

### Entregables del hito
- `tools/anim-measure/` con scripts reproducibles y un README.
- `assets/animations/oppo-medido.json` relleno.
- Informe corto con las curvas y los errores de ajuste.

## D. Qué NO hacer

- No descompilar el launcher de OPPO ni extraer sus recursos.
- No instalar módulos que lo reempaqueten.
- No afirmar en la app ni en el repositorio que es "el launcher de OPPO".
