# CLAUDE.md — UltimateLauncher

Launcher Android (fork delgado de Launcher3 de AOSP `android17-release`) con estética y animaciones medidas de OPPO, sin extras, con sync de layout. Móvil y tablet, `minSdk` 31, sin GMS. Toda la especificación está en `docs/` (los archivos `00` a `09`).

## Cómo trabajar

1. Lee `docs/00-README.md`, luego `01`, `02`, `03` y `09`.
2. Trabaja **por hitos** de `docs/07-tareas-e-hitos.md`, en orden. No empieces un hito sin cumplir la aceptación del anterior.
3. Antes de cada tarea, vuelve a leer el archivo de la especificación que le corresponde (`04`, `05` o `06`).
4. Antes de tocar un archivo de AOSP, apunta el cambio en `/patches` (archivo, motivo, cómo reaplicar). Cambios mínimos.
5. Si algo de la especificación no coincide con lo que ves en el código, **para y dilo**; corrige el documento en la misma tarea.

## Reglas duras

- **No** copiar ni incluir código, recursos, fuentes, iconos, fondos ni marcas de OPPO, vivo, Xiaomi, Honor ni de ningún otro fabricante. No descompilar launchers de terceros.
- **Privacidad primero** (`docs/09-privacidad.md`): sin telemetría, sin dependencias de Google ni de librerías con red. La variante `default` **no declara `INTERNET`**; la red solo existe en la variante `sync` (WebDAV, solo HTTPS). No añadir permisos fuera de la tabla de `09` sin avisar.
- **Sin root** como requisito.
- `minSdk` 31, móvil y tablet. Toda API posterior a Android 12 va detrás de una compuerta de versión con alternativa.
- Antes de reutilizar cualquier archivo de Lawnchair, revisa su licencia y regístralo en `/patches`.
- No inventes valores: los de estética y animación salen de **mediciones** (`04`) o de la propia rama de AOSP. Si no hay medición, el campo queda en `null`.
- Las rutas de ajustes de cada fabricante son candidatas hasta que se verifiquen en un dispositivo real; van en `oem-intents.json`, no en el código.

## Verificar antes de dar por hecho

- Qué trae realmente la rama respecto a búsqueda, predicciones, categorías y feed (M1 T1.4).
- Si compila y arranca en Android 12 (API 31), 14 y 16/17, y sin GMS (M1 T1.2).
- `compileSdk` y `targetSdk` correctos para Android 17 (supuesto: 37, sin confirmar).

## Estilo de trabajo

- Commits pequeños, uno por tarea.
- Pruebas unitarias para la lógica propia (layout, adaptadores, perfiles). Sin exigir cobertura sobre código de AOSP.
- Comentarios y documentación en español; identificadores y código en inglés.
- Cuando termines un hito, resume en pocas líneas qué cambió y qué quedó sin probar.
