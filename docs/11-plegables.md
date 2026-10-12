# 11 — Plegables (v2)

Estado: **parcial** — 2026-10-12. Hecho: elección de rejilla en plegables (ver *Estado de M10*). Sin plegable ni emulador disponibles: lo demás está pendiente. Hito **M10** de `07`. No depende de Dual apps (`10`): puede avanzar en paralelo a M9.

## Qué es

Que el launcher se adapte a la **pantalla exterior**, a la **interior** y a la **transición** entre ambas, y a la **postura** semiabierta. Se aprovecha lo que Launcher3 de Android 17 ya trae para multipantalla (`InvariantDeviceProfile`, `DisplayController`, escritorio a dos paneles) y se añade solo lo que falta: rejillas propias por pantalla, continuidad al plegar y postura.

**Plataformas**
- Pixel plegables con GrapheneOS o AOSP, y el emulador «foldable». Dispositivo de pruebas: **Pixel 9 Pro Fold** (disponible más adelante); hasta entonces, emulador.
- Plegables de OPPO, vivo, Xiaomi y Honor **con firmware global**. En **ColorOS de China** el launcher no se puede fijar como inicio (`oem-issues.md`, PMX110), así que M10 no se puede probar ni usar allí (aceptado de momento, 2026-10-12); vale la misma reserva para otras ROMs chinas hasta comprobarlas.

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
- **Modo de diseño**: por defecto **sincronizado** (decidido 2026-10-12): la interior muestra en pares las páginas de la exterior. Opción para que cada pantalla tenga su propia disposición.
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

**Corregido (2026-10-12)**: en AOSP los dos paneles solo existen en dispositivos con dos pantallas (`isTwoPanels = isLargeScreen && isMultiDisplay`). Una tablet de una sola pantalla es `TYPE_TABLET` y sigue con su rejilla medida (`ultimate_tablet`) y el dock de M3b (parches 0040–0042, 0091, 0120). Dar dos paneles a una tablet apaisada sería un cambio de diseño aparte, sin pedir.

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

## Estado de M10 (2026-10-12)

**Lo que ya trae AOSP** (leído en el código de `android17-release`; sin verificar en un plegable real):
- `LauncherDisplayInfo.deviceType` devuelve `TYPE_MULTI_DISPLAY` cuando las pantallas del dispositivo incluyen una de móvil y otra grande; `DeviceProperties.isTwoPanels` activa el escritorio a dos paneles en la interior.
- Las dos pantallas comparten **una misma base de datos de escritorio**; la interior muestra las páginas de dos en dos. Es el diseño **sincronizado** decidido por defecto.
- `DisplayController` detecta el cambio de pantalla activa (`CHANGE_ACTIVE_SCREEN`) y recalcula el `DeviceProfile`; la actividad no se recrea (`configChanges`).

**Hecho**
- T10.1 (rejillas): el parche 0009 forzaba `ultimate_phone` (solo `phone`) en todo lo que no fuera tablet. En un plegable esa rejilla no vale y la base elegía entre **todas** las rejillas, incluidas las de tablet. Ahora `DefaultGrid` (con prueba) deja en plegables y escritorio la rejilla de AOSP `phone|multi_display`, hasta que haya medidas; en móvil y tablet nada cambia (comprobado en el Pixel 8: `ultimate_phone`, 5×7).

**Pendiente** (hace falta el Pixel 9 Pro Fold o un emulador plegable):
- Verificar en un plegable los dos paneles, la continuidad al plegar/desplegar y los 20 pliegues seguidos.
- Opción «cada pantalla con su disposición» (exige dos bases de datos; AOSP no lo trae).
- T10.4 postura (libro y mesa) con `WindowInfoTracker`: no se implementa sin un dispositivo donde verla.
- T10.5 pruebas instrumentadas con `WindowLayoutInfoPublisherRule` y capturas.
- Medir la rejilla exterior e interior de un plegable real para sustituir las de AOSP.

