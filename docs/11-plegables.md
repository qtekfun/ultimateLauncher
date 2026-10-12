# 11 — Plegables (v2)

Estado: borrador v2 — 2026-10-12. Hito **M10** de `07`. No depende de Dual apps (`10`): puede avanzar en paralelo a M9.

## Qué es

Que el launcher se adapte a la **pantalla exterior**, a la **interior** y a la **transición** entre ambas, y a la **postura** semiabierta. Se aprovecha lo que Launcher3 de Android 17 ya trae para multipantalla (`InvariantDeviceProfile`, `DisplayController`, escritorio a dos paneles) y se añade solo lo que falta: rejillas propias por pantalla, continuidad al plegar y postura.

**Plataformas**
- Pixel plegables con GrapheneOS o AOSP, y el emulador «foldable».
- Plegables de OPPO, vivo, Xiaomi y Honor **con firmware global**. En **ColorOS de China** el launcher no se puede fijar como inicio (`oem-issues.md`, PMX110), así que M10 no se puede probar ni usar allí; vale la misma reserva para otras ROMs chinas hasta comprobarlas.

## Lo que ya hay en la base

- `launcher3-base/res/xml/device_profiles.xml` ya define opciones con `deviceCategory="phone|multi_display"` (líneas 141 y 206). Se parte de ellas.
- `androidx.window:window` 1.5.0 ya es dependencia (`gradle/libs.versions.toml`). Para las pruebas hace falta además `androidx.window:window-testing` (**nueva, solo `testImplementation`/`androidTestImplementation`**; se apunta en `docs/dependencies.md` cuando se añada).
- La actividad `Launcher` ya declara `configChanges` con `orientation|screenSize|screenLayout|smallestScreenSize` (`app/src/main/AndroidManifest.xml`), así que plegar no la recrea.

## Perfiles por pantalla

- Opciones de rejilla `multi_display`: una para la pantalla exterior (tipo móvil) y otra para la interior (por panel). **Valores: `null` hasta medirlos** en cada modelo (regla de `CLAUDE.md`); mientras tanto se usan los de AOSP.
- `DisplayController` detecta el cambio de pantalla activa y elige el `DeviceProfile` correspondiente.
- Iconos, texto y espaciado escalan por densidad y tamaño, **no por modelo**, para no mantener una tabla por marca.

## Disposición

- **Interior**: escritorio a dos paneles (dos páginas visibles), dock centrado a todo el ancho, cajón en cuadrícula ancha.
- **Exterior**: disposición de móvil normal.
- **Modo de diseño**: cada pantalla con su disposición, u opción de sincronizar (la interior muestra en pares las páginas de la exterior). Valor por defecto: ver *Preguntas abiertas*.
- **Widgets**: al cambiar de pantalla se reajustan con `minResizeWidth/Height`; si no caben, pasan a la siguiente celda libre **sin borrarse**.

## Continuidad al plegar y desplegar

- Se guarda la página visible y el estado del cajón; al desplegar, la página de la exterior queda en el panel izquierdo.
- Transición con el perfil de animación activo (`04`). Objetivo: sin parpadeo blanco ni salto de rejilla.

## Postura (Jetpack WindowManager)

- `WindowInfoTracker` da el `FoldingFeature` con el estado (plano / semiabierto) y los límites de la bisagra.
- **Libro** (bisagra vertical, semiabierto): el hueco entre paneles se alinea con la bisagra; ningún icono ni widget cae encima.
- **Mesa** (bisagra horizontal, semiabierto): el contenido queda en la mitad superior y el dock baja a la inferior. Launcher3 no trae esta disposición: es código propio.
- Si `androidx.window` no da información (ROMs que no lo implementan), se asume pantalla plana y se decide los dos paneles solo por el ancho.

## Tablets

Comparten el camino de la pantalla interior: rejilla ancha, dos paneles en horizontal y uno en vertical. No debe romper lo hecho en M3b (dock de tablet, parches 0040–0042, 0091, 0120).

## Mantenimiento

Cada rebase de AOSP puede romper el código de dos paneles. Los cambios van en clases propias; sobre AOSP solo puntos de extensión, cada uno en `/patches`.

## Pruebas

**Automáticas**
- Unitarias de la elección de `DeviceProfile` según el tamaño de pantalla.
- Instrumentadas con `WindowLayoutInfoPublisherRule` (de `window-testing`) para simular `FoldingFeature` sin hardware.
- Captura de pantalla de cada disposición (exterior, interior, libro, mesa) para detectar regresiones visuales.

**Manuales clave**
- Plegar y desplegar 20 veces seguidas con el cajón abierto y con un widget grande: sin cierre ni pérdida de posición.
- Rotación y multiventana en la pantalla interior sin pérdida de estado (RF-62).
