# 07 — Tareas e hitos

Cada hito termina con algo que se pueda **instalar y probar**. No pasar al siguiente hito si el criterio de aceptación no se cumple.

## M0 — Esqueleto de compilación
- T0.1 Clonar Launcher3 `android17-release`.
- T0.2 Inventario de dependencias (`Android.bp`).
- T0.3 Andamiaje Gradle que compile, siguiendo el enfoque de Lawnchair 16-dev (respetando su licencia).
- T0.4 `/patches` y `UPSTREAM.md` creados.
- T0.5 Variantes de compilación `default` (sin red) y `sync` (con WebDAV), y comprobación en CI de que `default` no declara `INTERNET` (ver `09`).
- **Aceptación**: APK debug de la variante `default` generado desde línea de comandos, sin permiso de red.

## M1 — Arranca en tus teléfonos
- T1.1 Fijar como launcher (rol `ROLE_HOME`).
- T1.2 Prueba de arranque en Android 12 (API 31), 14 (API 34) y 16/17, en emulador y en dispositivos reales, incluido un dispositivo sin GMS.
- T1.3 Rellenar la matriz de dispositivos y ejecutar la checklist de `03`.
- T1.4 Confirmar o corregir la tabla de qué se conserva/elimina de `02`.
- **Aceptación**: muestra inicio y cajón en Android 12 y en 16/17; informe de qué falla (si algo).
- **Punto de decisión**: contar las compuertas de compatibilidad que hicieron falta, revisar qué versiones usan los dispositivos reales y decidir si se mantiene `minSdk` 31 o se sube (tabla de `03`).

## M2 — Recortes
- T2.1 Quitar QSB (inicio y dock), fila de previstas, búsqueda extendida y ganchos de feed.
- T2.2 Cajón plano con búsqueda local por nombre.
- T2.3 Decidir y montar el sistema de ajustes del usuario.
- **Aceptación**: cumple RF-03, RF-04, RF-10 a RF-13.

## M3 — Widgets y perfil de trabajo
- T3.1 Pruebas de widgets (incluyendo los que piden configuración) en los cuatro fabricantes.
- T3.2 Pruebas de perfil de trabajo (en un teléfono con perfil de trabajo real o en emulador).
- T3.3 Corregir lo que falle.
- **Aceptación**: RF-02, RF-20, RF-21.

## M3b — Tablet
- T3b.1 Definir las clases `phone`, `phone-landscape`, `tablet`, `tablet-landscape` sobre los perfiles de dispositivo de Launcher3.
- T3b.2 Tokens de rejilla, iconos y dock por clase de pantalla (`ThemeTokens`).
- T3b.3 Probar rotación, multiventana y cambio de tamaño de ventana sin pérdida de estado.
- T3b.4 Comprobar qué ocurre con la barra de tareas de tablet (se espera que no esté).
- **Aceptación**: RF-60 a RF-62 en una tablet real.

## M4 — Estética (tokens)
- T4.1 Medir estética en un OPPO (protocolo de `04`) y, si se dispone de ella, en una tablet.
- T4.2 Implementar `ThemeTokens` y cargar `oppo-medido.json`.
- T4.3 Blur con reserva a velo.
- T4.4 Comparación lado a lado y ajuste.
- **Aceptación**: indistinguible a simple vista en inicio, cajón y carpetas.

## M5 — Animaciones
- T5.1 Inventario de constantes de animación de Launcher3 (`docs/anim-inventory.md`).
- T5.2 Implementar `AnimationProfileProvider` y redirigir las constantes.
- T5.3 Perfiles `aosp-por-defecto` y `rapido`.
- T5.4 `tools/anim-measure`: capturas y ajuste.
- T5.5 `oppo-medido.json` relleno y validación lado a lado.
- **Aceptación**: duración con diferencia menor de 1 fotograma y forma indistinguible en los eventos del alcance.

## M6 — Adaptación por fabricante
- T6.1 Interfaz `OemAdapter` y `GenericAdapter`.
- T6.2 `ColorOsAdapter`, `VivoAdapter`, `HyperOsAdapter`, `MagicOsAdapter` con `oem-intents.json`.
- T6.3 Asistente de primer arranque.
- T6.4 Ejecutar la checklist de `05` en cada dispositivo.
- **Aceptación**: un teléfono nuevo de cada marca queda utilizable siguiendo solo el asistente.

## M7 — Sincronización
- T7.1 Serialización del layout y esquema v1.
- T7.2 Importador con adaptaciones y resumen previo.
- T7.3 Archivo local y selector de documentos (por defecto, sin red).
- T7.4 Variante `sync` con WebDAV, Keystore y cifrado por defecto.
- T7.5 Adaptación móvil ↔ tablet en la importación.
- **Aceptación**: exportar en un dispositivo e importar en otro de otra marca con el informe de diferencias.

## M8 — Pulido y publicación
- T8.1 Packs de iconos y forma de icono.
- T8.2 Español e inglés.
- T8.3 Documentación de usuario y de mantenimiento.
- T8.4 Release firmada en GitHub, compatible con Obtainium.
- T8.5 Auditoría de privacidad completa (`09`) y build reproducible como objetivo.
- **Aceptación**: todos los criterios de producto de `01`.

## Integración continua (propuesta)

- Compilación y pruebas en cada PR (GitHub Actions).
- Dependabot para dependencias propias.
- Comprobación del manifiesto fusionado (permisos), de dependencias prohibidas (Google/no libres) y de trackers (ver `09`); si falla, falla el PR.
- Android Lint con `NewApi` como error.
- Análisis estático (Kotlin lint/ktlint y Android lint) con una **línea base** del código de AOSP para no ahogar el ruido heredado.
- Cobertura: umbral alto en `feature-layoutsync` y `feature-oemkit` (lógica pura); no exigir un porcentaje sobre el código de AOSP.
- Prueba de arranque instrumentada en emulador Android 12, 14 y 16/17, y en tamaño de móvil y de tablet.

## Convenciones

- Commits pequeños, uno por tarea cuando sea posible.
- Cada parche sobre AOSP referenciado en `/patches`.
- Nada de datos propios del usuario en el repo.
