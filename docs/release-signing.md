# Firma de la release (notas; sin claves reales en el repo)

Estado actual: `app/build.gradle` firma `release` con la clave de **depuración** local (solo pruebas, ver `docs/03`). Nada de lo de abajo se ha ejecutado ni hay clave de publicación creada; son los pasos para cuando se decida publicar (M8, T8.4).

## Reglas
- La clave de publicación (`.jks`/`.keystore`) y sus contraseñas **no entran nunca en el repo** (`.gitignore` ya excluye `*.jks` y `*.keystore`) ni en CI. Copia de seguridad cifrada fuera del PC de trabajo: perder la clave impide actualizar las instalaciones existentes.
- Una clave distinta para cada `applicationId` no es necesaria; la variante `sync` (`.sync`) puede compartir clave, pero son apps distintas para Android.
- Distribución fuera de Google Play (GitHub Releases, compatible con Obtainium): Obtainium verifica que el certificado no cambie entre versiones, así que la clave debe ser siempre la misma.

## Crear la clave (una vez, en el equipo del mantenedor)
```bash
keytool -genkeypair -v -keystore ~/secure/ultimatelauncher-release.jks -alias ultimatelauncher \
  -keyalg RSA -keysize 4096 -validity 10000   # pide contraseñas por teclado; no las pases en la línea de comandos
```
Anota la huella SHA-256 del certificado (`keytool -list -v -keystore ...`) en el README para que cualquiera pueda verificar los APK.

## Firmar fuera de CI
Compilar sin firmar o con la clave de depuración y firmar el resultado con `apksigner` (build-tools), leyendo las contraseñas del terminal:
```bash
./gradlew :app:assembleDefaultRelease
zipalign -p -f 4 app/build/outputs/apk/default/release/app-default-release.apk /tmp/ul-aligned.apk
apksigner sign --ks ~/secure/ultimatelauncher-release.jks --ks-key-alias ultimatelauncher \
  --out dist/ultimatelauncher-default-VERSION.apk /tmp/ul-aligned.apk
apksigner verify --print-certs dist/ultimatelauncher-default-VERSION.apk
sha256sum dist/ultimatelauncher-default-VERSION.apk > dist/ultimatelauncher-default-VERSION.apk.sha256
```
Esquemas v2/v3 (minSdk 31 no necesita v1). Antes de firmar: `tools/ci.sh` en verde y la lista de `docs/09 «Auditoría antes de cada versión»` marcada. Auditar el APK **ya firmado** con `tools/check-permissions.sh dist/ultimatelauncher-default-VERSION.apk`.

## Si se quiere firmar desde Gradle más adelante
Definir un `signingConfig` que lea ruta y contraseñas de variables de entorno o de `~/.gradle/gradle.properties` (fuera del repo) y aplicarlo solo si existen; si no, seguir con la clave de depuración. No poner valores por defecto ni rutas reales en `build.gradle`.

## Pendiente (objetivo de M8)
- Minificación R8 (hoy `minifyEnabled false`): puede cambiar el resultado de la auditoría de clases; volver a pasar `tools/privacy-audit.sh`.
- Build reproducible: no verificado (dos compilaciones consecutivas no se han comparado byte a byte). Sin `dist/` versionado.
- Escáner de trackers externo (Exodus Privacy / `exodus-standalone`): no instalado ni ejecutado; la comprobación propia de clases prohibidas lo sustituye solo en parte.
