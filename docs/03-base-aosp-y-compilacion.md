# 03 — Base AOSP y compilación

## Hechos verificados (2026-10-04)

- Android 17 se publicó en junio de 2026.
- Google publica el código de AOSP **dos veces al año** (segundo y cuarto trimestre) y recomienda la rama `android-latest-release`.
- En `platform/packages/apps/Launcher3` existen la rama `android17-release` y la etiqueta `android-17.0.0_r1` (también `android16-release`, `android16-qpr1-release`, `android16-qpr2-release`).
- Todavía no hay rama `android17-qpr*`: se espera con la publicación del cuarto trimestre.
- Lawnchair: la versión recomendada a usuarios es 15 Beta 3 (Android 15); la rama 16-dev se publica en nightlies y apunta a API 36; hay issues abiertos con widgets y gestos.

## Decisión

Base = Launcher3 `android17-release`. Referencia de compilación y de solución a dependencias internas = Lawnchair 16-dev (código abierto; **respetar su licencia al reutilizar cualquier archivo**).

### Por qué no Lawnchair completo
Hereda bugs abiertos justo en widgets y gestos (dos requisitos clave), trae funciones que se quieren quitar y su base va por detrás de Android 17.

### Por qué no desde cero
Reimplementar widgets, arrastre y perfil de trabajo es lo más caro y frágil.

### Coste asumido
- Hay que montar el andamiaje de compilación Gradle (Launcher3 está pensado para el árbol completo de AOSP y usa APIs internas).
- Mantenimiento semestral al llegar cada publicación de AOSP.

## Paso a paso (hito M0)

1. Clonar `platform/packages/apps/Launcher3` en la rama `android17-release` (solo ese paquete; no hace falta el árbol completo de AOSP).
2. Inspeccionar `Android.bp` para listar dependencias y módulos (`launcher-aosp-tapl`, bibliotecas de `frameworks/libs/systemui`, etc.).
3. Estudiar cómo Lawnchair 16-dev resuelve: Gradle, `compileOnly` de las APIs internas, la biblioteca de `frameworks/libs/systemui` y los protos. Reproducir el enfoque; no copiar código sin cumplir licencia.
4. Producir un `build.gradle.kts` que compile `app` y genere un APK firmado en modo debug.
5. Registrar todo cambio sobre archivos de AOSP en `/patches`.

## minSdk 31 (decidido) y compatibilidad

Decisión: `minSdk` **31** (Android 12). Se revisa si un mínimo mayor da ventajas sustanciales (tabla siguiente).

Riesgo (mi inferencia, **sin verificar**): al compilar contra Android 17, el código usa APIs que no existen en Android 12-16. Cuanto más bajo el mínimo, más compuertas de compatibilidad hay que mantener.

Estrategia:
- `compileSdk` y `targetSdk` = los de Android 17 (probablemente 37; **verificar**).
- Todo uso de API nueva detrás de `Build.VERSION.SDK_INT` y con alternativa.
- Android Lint con `NewApi` como **error** en CI.
- Pruebas de arranque en Android 12 (API 31), 14 (API 34) y 16/17, en emulador y en dispositivos reales.

### Qué aportaría subir el mínimo (a confirmar contra la documentación al decidir)

| Mínimo | Qué aporta | Valoración |
|---|---|---|
| 31 (Android 12) | Desenfoque de ventana, `RenderEffect`, colores dinámicos, esquinas redondeadas y diseños adaptables en widgets | Ya incluido: es el suelo |
| 33 (Android 13) | Capa monocromática en iconos adaptativos (iconos temáticos), idioma por app, permiso de notificaciones en ejecución | Útil solo si se quieren iconos temáticos nativos sin trabajo propio |
| 34 (Android 14) | Retroceso predictivo más completo, cambios en lanzamiento de actividades en segundo plano | Marginal |
| 35 (Android 15) | Private Space y archivado de apps (que la base ya soporta) | Solo si se quiere Private Space/archivado |
| 36-37 | Coincide con la base de Android 17: menos compuertas | Menos código de compatibilidad, pero deja fuera los Android 12-15 |

Recomendación: mantener 31 durante M0 y M1. Al cierre de M1, contar cuántas compuertas hicieron falta y qué versiones usan los dispositivos reales; si son muchas y los dispositivos ya están en 33 o más, valorar subir (el candidato con más sentido sería 33 por los iconos temáticos). La decisión formal está en el punto de decisión de M1.

Datos de M1 (2026-10-05, `docs/compat-android12-14.md`): 10 cambios de compatibilidad (3 compuertas de versión, 2 equivalentes androidx, 3 alternativas de recursos, desugaring, 1 alternativa sin GMS) y `NewApi` = 0 en Lint. **Recomendación: mantener 31** (la tablet de pruebas es Android 12; subir a 33 apenas elimina nada porque lo costoso son los colores de API 34; subir a 34 solo se justifica si dejan de importar Android 12–13).

## Firma y distribución

- Clave de firma propia, fuera del repo.
- Releases en GitHub con APK y hash SHA-256; compatible con Obtainium.

## Seguimiento de AOSP

- Revisar cada publicación semestral (y las etiquetas `android-17.0.0_r*`) y registrar en `/patches/UPSTREAM.md` la última etiqueta integrada.
- Integrar con `git merge`/`rebase` de la rama upstream en una rama de integración y pasar la batería de pruebas antes de fusionar.

## Qué comprobar en el primer arranque (checklist M1)

- [ ] Compila y se instala.
- [ ] Se puede fijar como launcher predeterminado (ver rol `ROLE_HOME`, abajo).
- [ ] Muestra pantalla de inicio y cajón.
- [ ] Qué trae realmente la rama respecto a búsqueda, predicciones, categorías y feed (confirmar o corregir la tabla de `02`).
- [ ] Añadir un widget con pantalla de configuración funciona.
- [ ] Sin GMS no hay cierres por dependencias ausentes.

## Fijar como launcher predeterminado

Usar la API de roles (`RoleManager` con `ROLE_HOME`, Android 10+) para pedir que sea el launcher predeterminado, con `Settings.ACTION_HOME_SETTINGS` como alternativa. Cada ROM lo oculta en un sitio distinto; ver `05`.
