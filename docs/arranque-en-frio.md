# Arranque en frío: R8, perfil de línea base y cómo medirlo

Contexto (`docs/progress.md`, 2026-10-05): si ColorOS mata el proceso del launcher, el gesto de inicio provoca un arranque en frío.
Medido con el tiempo «Displayed»: debug 316–352 ms, release compilada a nativo (`-m speed`) 156–157 ms. `tools/install.sh` ya compila a
nativo tras instalar. Lo que sigue intenta que el APK llegue más ligero y con un perfil de arranque.

## Estado

| Pieza | Estado | Cómo se activa |
|---|---|---|
| R8 (`minifyEnabled`) | Compila y el análisis estático pasa; **sin probar en dispositivo → apagado por defecto** | `./gradlew --offline -Pul.minify=true :app:assembleDefaultRelease` |
| Perfil de línea base (`app/src/main/baseline-prof.txt`) | Escrito a mano (comodines), entra en el APK (`assets/dexopt/baseline.prof`); **no grabado ni medido** | Siempre (inofensivo: solo es una pista para ART) |
| Medida de arranque en dispositivo | Procedimiento abajo; **no ejecutado** (no se usó dispositivo) | Lo ejecuta el humano |

## Tamaño del APK (`assembleDefaultRelease`, firmado con la clave de depuración local)

| Build | Tamaño |
|---|---|
| Sin minificar (actual) | 66 160 850 B (63,1 MiB); `classes.dex` 27,1 MB |
| R8 activado | 10 503 634 B (10,0 MiB); `classes.dex` 6,0 MB |

`baseline.prof` pasa de 10 354 B (solo el de las bibliotecas) a 18 883 B sin R8 y 11 236 B con R8 (R8 elimina clases y reescribe el perfil).
Permisos con R8: idénticos; `tools/check-permissions.sh` OK (sin INTERNET).

## Reglas de R8 (`app/proguard-rules.pro`) y por qué

- `-include ../launcher3-base/proguard.flags`: reglas de AOSP. Mantienen los nombres de `com.android.**` (se puede eliminar lo no usado,
  pero lo que queda conserva nombre), `RecyclerView`, los `Fragment` con constructor público y los protos de registro.
- `-keep class com.qtekfun.** { *; }`: código propio (adaptadores OEM, asistente, dock, animaciones); referenciado desde el manifiesto y por
  reflexión de preferencias. Pila de errores legible.
- `-keep class * extends android.view.View { <init>(...); }` y `Preference { <init>(...); }`: inflado por nombre desde XML (capa de seguridad;
  aapt2 ya genera reglas para los layouts y el manifiesto).
- `GeneratedMessageLite` (protobuf lite): se conservan los campos; el esquema se resuelve por reflexión.
- `set*(float)/get*()`: animadores por nombre de propiedad.
- Dagger no necesita reglas (código generado, sin reflexión).
- `-dontwarn` de `androidx.window.extensions.**`, `androidx.window.sidecar.**` y `com.android.extensions.appfunctions.**`: clases que provee
  la plataforma en tiempo de ejecución y no están en el classpath (lista `missing_rules.txt` de R8, revisada a mano; el servicio
  AppFunctions no está en el manifiesto).

### Comprobaciones estáticas hechas (sin dispositivo)

1. Ninguna clase referenciada por nombre en XML (`res/`, manifiesto, `xml/`) fue eliminada, salvo `androidx.startup.InitializationProvider`,
   que nuestro manifiesto ya quita a propósito (`tools:node="remove"`).
2. Reflexión en el código de Launcher3 propio: solo `Class.forName("android.os.SystemProperties")` (clase del sistema).
3. Dagger (`com.android.launcher3.dagger.*`), `Launcher`, `LauncherAppState` y `LauncherPrefs` siguen en `mapping.txt`.

### Qué NO se ha comprobado (por eso está apagado)

Que la app arranque y funcione con R8 en un teléfono: inicio, cajón, carpetas, widgets (selector Compose), ajustes, exportar/importar
(protobuf/JSON), asistente. Si algo falla, `adb logcat -b crash` y buscar `ClassNotFoundException`, `NoSuchMethodException` o
`NoSuchFieldException`; añadir la regla `-keep` mínima para esa clase en `app/proguard-rules.pro`. Los `mapping.txt` y `usage.txt` están en
`app/build/outputs/mapping/defaultRelease/`.

## Procedimiento de medida de arranque (para el humano)

Requisitos: el dispositivo conectado (una sola vez), `com.qtekfun.ultimatelauncher` instalado. Cuatro variantes a comparar: A = release
actual (`speed`), B = release sin compilar (`verify`), C = release con perfil (`speed-profile`), D = release con R8 + `speed`.

```bash
PKG=com.qtekfun.ultimatelauncher
S=<serie adb>
# 1) Instalar la variante (A/B/C: ./gradlew --offline :app:assembleDefaultRelease; D: añadir -Pul.minify=true)
adb -s $S install -r app/build/outputs/apk/default/release/app-default-release.apk
# 2) Fijar el modo de compilación de ART (uno de: verify | speed-profile | speed)
adb -s $S shell cmd package compile -m speed -f $PKG
# 3) Arranque en frío: matar el proceso y volver a inicio como lo hace el gesto; repetir 10 veces
for i in $(seq 10); do
  adb -s $S shell am force-stop $PKG; sleep 2
  adb -s $S logcat -c
  adb -s $S shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n $PKG/com.android.launcher3.Launcher | grep -E "TotalTime|WaitTime"
done
# Alternativa fiel al gesto: matar y pulsar inicio; leer «Displayed» (la medición usada hasta ahora)
adb -s $S shell am force-stop $PKG; adb -s $S logcat -c; adb -s $S shell input keyevent KEYCODE_HOME; sleep 3
adb -s $S logcat -d -s ActivityTaskManager | grep Displayed
```

Se toma la **mediana de 10** `TotalTime` por variante; descartar la primera tras instalar. Con la pantalla encendida, cargado, sin ahorro de
batería y animaciones a 1.0 (como en `docs/04`). Anotar el estado de ART con `adb shell dumpsys package $PKG | grep -A2 "Dexopt state"`
(`status=speed|speed-profile|verify`). Una mejora menor de 1 fotograma (8 ms a 120 Hz) no es distinguible.

Sobre el perfil de línea base: para que ART lo use en un APK instalado por `adb` sin Play Store hace falta o bien
`cmd package compile -m speed-profile` (usa el perfil que ART tenga registrado, no necesariamente el del APK) o la biblioteca
`ProfileInstaller`, cuyo arranque automático (`androidx.startup`) está desactivado en nuestro manifiesto por privacidad/ruido. Por eso **no se
ha demostrado** que el perfil del APK tenga efecto con `tools/install.sh`; ese script ya usa `-m speed`, que lo supera. Para grabar un perfil
real: módulo Macrobenchmark `BaselineProfileRule` con un dispositivo (no hecho; añadiría dependencias de pruebas).

## Decisión

R8 y la grabación del perfil quedan listos pero **no son el predeterminado** hasta que el humano ejecute el procedimiento anterior con R8.
Si D arranca sin errores y es ≥ A, cambiar el valor por defecto de `ul.minify` en `app/build.gradle`.

## Prueba de R8 en el CPH2841 (2026-10-05)
- APK con `-Pul.minify=true`: 10,5 MB (sin R8: 66,2 MB), compilado a nativo (`speed`) en el móvil.
- Arranque en frío con otra app delante (`am start -W`, mediana de 7): 162 ms con R8 frente a 176 ms sin R8. Diferencia pequeña, dentro del ruido; la ganancia es sobre todo de tamaño.
- Humo funcional con R8 (sin cierres ni `ClassNotFound`/`NoSuchMethod` propios en logcat): abrir cajón, abrir carpeta, ajustes de inicio, pantalla de sincronización, abrir una app desde el dock, menú de pulsación larga. El único `NoSuchMethodException` es de `OplusPredictiveBackController` (código de ColorOS).
- Sin probar con R8: selector de widgets y widgets con configuración, perfil de trabajo, arrastrar iconos/carpetas, importar un archivo de sincronización, asistente de primer arranque.
- Estado: R8 sigue apagado por defecto; en el móvil queda la release sin R8.
