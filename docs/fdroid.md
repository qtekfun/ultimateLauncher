# Publicar en F-Droid (preparación)

Objetivo: que `com.qtekfun.ultimatelauncher` (variante `default`) esté en el catálogo oficial de F-Droid. Este documento recoge lo que ya está preparado en el repositorio, lo que falta, los riesgos y la lista de comprobación del merge request a [fdroiddata](https://gitlab.com/fdroid/fdroiddata). **El MR lo envía el mantenedor, no lo envía ningún agente.** Fuentes consultadas el 2026-10-05: [Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/), [Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/), `buildserver/` de [fdroidserver](https://gitlab.com/fdroid/fdroidserver) y [gradlew-fdroid](https://gitlab.com/fdroid/gradlew-fdroid).

## Qué hay ya en el repositorio
| Pieza | Dónde | Estado |
|---|---|---|
| Licencia GPL-3.0-or-later | `LICENSE`, `NOTICE`, cabeceras SPDX en `app/src` y `tools/` | Hecho (decisión en `docs/DECISIONS.md`, el usuario puede cambiarla) |
| Metadatos del catálogo | `fastlane/metadata/android/{es-ES,en-US}/` (título, descripción corta y larga, `changelogs/1.txt`) | Hecho; **sin icono ni capturas** (ver «Pendiente») |
| Receta borrador | `docs/fdroid/com.qtekfun.ultimatelauncher.yml` | Borrador, **no probada con `fdroid build`** (no hay fdroidserver instalado aquí) |
| Release sin firma | `./gradlew -Pul.unsigned=true :app:assembleDefaultRelease` → `app-default-release-unsigned.apk` | Hecho (el `release` normal no cambia: sigue firmando con la clave de publicación por variables de entorno o, sin ellas, con la de depuración) |
| Sin bloque `dependenciesInfo` | `app/build.gradle` | Hecho: el APK ya no lleva el blob cifrado con la clave de Google Play, que F-Droid marca |
| Sin binarios en el árbol | `git ls-files` | Solo `gradle/wrapper/gradle-wrapper.jar` (ver «Gradle»), PNG/WebP de recursos y los de pruebas de captura de AOSP. Ningún `.apk`, `.aar`, `.so`, `.jks`, `.keystore`. `.gitignore` cubre `*.jks`, `*.keystore`, `dist/*.apk`, `build/`, `.gradle/` y ahora `.kotlin/` (había un log versionado, retirado) |

## Dependencias (classpath real de `defaultReleaseRuntimeClasspath`)
`./gradlew :app:dependencies --configuration defaultReleaseRuntimeClasspath` resuelve 160 artefactos en 60 grupos (la comprobación automática es `tools/check-dependencies.sh`, que falla con cualquier artefacto de `tools/forbidden-deps.txt`). Resultado de esta revisión:

- **AndroidX / Compose / Material**: Apache-2.0, de Google Maven (repositorio aceptado por F-Droid). Incluye `androidx.appsearch:appsearch-platform-storage` (usa el AppSearch del sistema, no GMS), `androidx.profileinstaller` y `androidx.startup`.
- **`com.google.*` presentes**: `com.google.android.material` (Material Components, Apache-2.0), `guava`/`failureaccess`/`listenablefuture`, `dagger`, `protobuf-javalite` (BSD-3), `j2objc-annotations`, `error_prone_annotations`, `jsr305`, `auto-service-annotations`. **Ninguna** es propietaria: son bibliotecas FOSS publicadas en Maven.
- **Terceros**: Kotlin y kotlinx (Apache-2.0), JetBrains annotations, JSpecify, `javax.inject` y `jakarta.inject`.
- **`desugar_jdk_libs`** (`coreLibraryDesugaring`, GPLv2 con Classpath Exception): FOSS, aceptado habitualmente.
- **No aparecen**: `com.google.android.gms`, `com.google.firebase`, `play-services-*`, `com.google.android.play`, ML Kit, Crashlytics, okhttp/retrofit/volley/grpc/cronet. F-Droid las rechazaría; aquí no están.
- **Solo en tiempo de compilación** (se descargan de Maven, no van al APK): `protoc` (binario nativo `com.google.protobuf:protoc` de Maven Central, usado por el plugin protobuf), `aapt2` y KSP/AGP de Google Maven. Es el caso habitual y lo permite la política («binarios FLOSS de Maven Central / Google Maven»), pero es lo primero que mirará un revisor.
- **`androidx.appfunctions` (alpha)**: está en el classpath pero el servicio no figura en el manifiesto (ver `docs/DECISIONS.md`).
- Falta, ya apuntado en `docs/dependencies.md`: revisión de licencias artefacto a artefacto y escáner de trackers (Exodus). El revisor de F-Droid hace su propio escaneo.

## Compatibilidad del build con el servidor de F-Droid
Qué exige el proyecto: JDK 21, plataforma Android `37.0` (`compileSdk` 37 minor 0), build-tools 36, AGP 9.4.1, Gradle 9.8.0 (wrapper), Kotlin 2.4.20, KSP, R8 activo.

Lo que se sabe del buildserver (`fdroidserver/buildserver`, Debian trixie, consultado en la documentación pública; **no verificado ejecutando un build real**):

| Requisito | Qué hace F-Droid | Riesgo |
|---|---|---|
| JDK 21 | La imagen es Debian trixie, cuyo OpenJDK por defecto es 21; Gradle usa `org.gradle.java.installations.auto-download=false`. Se puede forzar otro con `sudo:` en la receta. | Bajo |
| Gradle 9.8.0 | `gradlew-fdroid` sustituye al `./gradlew` del repo: lee `distributionUrl` de `gradle-wrapper.properties`, descarga esa versión de `downloads.gradle.org` y **verifica su SHA-256 contra el [gradle-transparency-log](https://gitlab.com/fdroid/gradle-transparency-log)**. No ejecuta `gradle-wrapper.jar`. Gradle 9.8.0 es posterior a la lista interna (llega a 9.2.0), así que depende de que el registro de transparencia ya la incluya. | Medio: si falta la 9.8.0 en el registro el build falla («No hash for gradle version»). Se resuelve esperando a que se registre o bajando a una versión incluida que acepte AGP 9.4.1 (AGP 9.4 pide Gradle ≥ 9.4.1 según su tabla de compatibilidad) |
| Plataforma Android 37.0, build-tools 36 | El script `provision-android-sdk` preinstala plataformas hasta `android-33` y build-tools hasta `33.0.0`; deja las carpetas `platforms/` y `build-tools/` escribibles para que **Gradle (AGP) descargue lo que falte**, con las licencias de `android-sdk-license` ya aceptadas. En fdroiddata hay apps con `compileSdk` reciente que se construyen así. | Medio-alto: `platforms;android-37.0` (con el `minorApiLevel`) es muy nueva; si la descarga automática no sirviera, hay que añadir en la receta `prebuild: sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'` (ver la receta). Una plataforma «en vista previa» o una licencia no aceptada bloquearía el build |
| AGP 9.4.1 / Kotlin 2.4.20 / KSP / Compose BOM | Se resuelven de Google Maven y Maven Central durante el build (con red). F-Droid permite Maven Central, Google Maven, JitPack, etc., pero **cada binario debe tener licencia libre**. | Bajo-medio: todo el classpath es FOSS (arriba) |
| `gradle/verification-metadata.xml` | No existe. F-Droid no lo exige (hace su propio análisis y se apoya en que los repositorios sean los permitidos). | Bajo. Sería una mejora de seguridad y de reproducibilidad: `./gradlew --write-verification-metadata sha256 :app:assembleDefaultRelease` (con las verificaciones fijadas, no se puede descargar un artefacto distinto) |
| Wrapper verificable | El JAR del wrapper (`gradle/wrapper/gradle-wrapper.jar`, SHA-256 `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`) lo ignora gradlew-fdroid, pero el escáner puede avisar de un JAR binario. **Verificado el 2026-10-05**: coincide con el «Wrapper JAR Checksum» de la 9.8.0 en https://gradle.org/release-checksums/, y `distributionSha256Sum` coincide con el ZIP `-bin`. | Bajo |
| Repositorios del build | `settings.gradle.kts` solo usa `google()` (limitado a `com.android.*`, `com.google.*`, `androidx.*`), `mavenCentral()` y `gradlePluginPortal()` para plugins. Sin JitPack ni repositorios propios. | Bajo |
| Tiempo/memoria | El build completo con R8 usa varios GB de memoria (`org.gradle.jvmargs=-Xmx6g`). El buildserver arranca máquinas virtuales con memoria limitada. | Medio: pedir/ajustar si se queda sin memoria |

## Anti-Features
Ninguna prevista. Revisado: sin red en `default` (no `NonFreeNet`), sin servicios de Google ni dependencias propietarias (no `NonFreeDep`), sin analítica (no `Tracking`), sin anuncios (no `Ads`), sin binarios sin fuentes (no `UpstreamNonFree`), sin recursos con licencia no libre (no `NonFreeAssets`: los fondos se generan por código, ver `NOTICE`). El revisor puede discrepar; si lo pide, se documenta.

Dos cosas que un revisor puede cuestionar:
1. **Permiso `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`**: F-Droid lo admite, la política de Google Play no. Está explicado en `docs/09`.
2. **API no SDK por reflexión** (gesto de abrir notificaciones, parche 0180): opcional y apagado por defecto.

## Pendiente antes de enviar el MR
1. **Icono y nombre propios.** El icono actual (`@drawable/ic_launcher_home`) es el de Launcher3 de AOSP (casa en píxeles con el robot de Android) y el título del APK es «UltimateLauncher». La política de inclusión dice que los forks **deben recibir nombre e icono distintos** del original; el nombre ya lo es, el icono no. Recomendado: crear un icono propio (adaptable) antes de publicar, y entonces añadir `fastlane/metadata/android/<locale>/images/icon.png` (512x512). No se generó un icono de catálogo a partir del de AOSP a propósito.
2. **Capturas de pantalla:** no hay capturas públicas aprobadas (pueden mostrar apps y datos personales). Faltan `phoneScreenshots/` (y `tenInchScreenshots/` para tablet). Son opcionales, pero el catálogo luce mucho mejor con ellas; deben hacerse con un perfil limpio.
3. **Etiqueta `v0.1.0` sobre un commit que contenga estos cambios** (el que ya exista sin ellos no sirve: no tiene `-Pul.unsigned`, ni la licencia, ni `fastlane/`). Crear la etiqueta tras fusionar esta rama, o pasar a `0.1.1` / `versionCode 2` y actualizar `CHANGELOG.md` y `fastlane/.../changelogs/2.txt`. Cada versión nueva necesita `versionCode` mayor y su `changelogs/<versionCode>.txt` (≤ 500 caracteres).
4. **Probar la receta** con `fdroid build -v -l com.qtekfun.ultimatelauncher` (fdroidserver y su imagen `buildserver`), y pasar `fdroid lint` y `fdroid rewritemeta`. No se ha podido en esta sesión.
5. Confirmar que Gradle 9.8.0 está en el registro de transparencia de Gradle de F-Droid (el hash del `gradle-wrapper.jar` ya está verificado).
6. **Aviso de la rama principal:** el repositorio debe ser público y la URL del `Repo` accesible sin credenciales.
7. Decidir si se quiere builds reproducibles con la firma del upstream (siguiente apartado).

## Build reproducible
**Resultado (2026-10-05): reproducible en esta máquina.** Se compiló dos veces `./gradlew --no-build-cache --no-daemon -Pul.unsigned=true :app:assembleDefaultRelease` (R8 activo) desde copias limpias del árbol en **directorios distintos** (`.../ra/ul-one` y `.../rb/otro-directorio-mas-largo/ul-two`, de longitud distinta), sin caché de Gradle ni de compilación:

| APK | SHA-256 | Tamaño |
|---|---|---|
| A | `1b160f10e2d12813ba8a55fe771c963d15e7f0501b43e7600dd4b0b79a1dae38` | 12 643 421 B |
| B | `1b160f10e2d12813ba8a55fe771c963d15e7f0501b43e7600dd4b0b79a1dae38` | 12 643 421 B |

`cmp` no encuentra diferencias: **idénticos byte a byte**, sin tocar nada de marcas de tiempo (AGP ya normaliza las fechas del ZIP a 1981-01-01 y `dependenciesInfo` está desactivado, que era el único blob no determinista habitual). Además, el mismo APK compilado **con firma de depuración** tiene las mismas 1412 entradas con idéntico contenido y compresión que el sin firmar (solo cambia el bloque de firma v2), y ambos pasan `zipalign -c -P 16 4`. Eso es lo que necesita `apksigcopier` para trasplantar la firma del upstream al APK que compile F-Droid.

Lo que **no** se ha probado, y puede romper la identidad bit a bit:
- Otra máquina o entorno: JDK (Temurin 21.0.12 aquí frente al OpenJDK 21 de Debian en el buildserver), build-tools (36.0.0 aquí; el buildserver puede traer otra), Gradle (el mismo 9.8.0), zona horaria y locale. El mismo JDK major con distinto proveedor suele dar las mismas clases, pero conviene confirmarlo.
- `META-INF/version-control-info.textproto`: AGP escribe ahí el tipo de VCS y el **hash del commit**. En esta prueba no había `.git` (`NO_SUPPORTED_VCS_FOUND`); en GitHub y en F-Droid el árbol sí lo tiene y es el mismo commit, por lo que debería coincidir. Si diera problemas, se desactiva con `buildTypes.release.vcsInfo.include = false` (no se ha cambiado para no alterar el APK ya comparado).
- Compilar con la clave real de GitHub frente al sin firmar de F-Droid (solo se probó la de depuración).
- No se usó `diffoscope`/`apkdiff`: no hacían falta al ser idéntico el hash (y no están instalados).

Causas habituales de no determinismo que se revisaron: marcas de tiempo (normalizadas por AGP; no hay `BuildConfig` con fechas: los `buildConfigField` de `app/build.gradle` son constantes), orden de recursos (estable), rutas absolutas (R8 no las incrusta; el cambio de directorio no afectó), bloque de dependencias de Play (desactivado) y R8 (determinista con las mismas entradas).

**Camino recomendado:** pedir a F-Droid la verificación reproducible: en la receta, descomentar `Binaries:` y `AllowedAPKSigningKeys:` (ya preparadas). F-Droid compila `v0.1.0`, compara con `UltimateLauncher-0.1.0.apk` de la release de GitHub ignorando la firma y, si coincide, **publica el APK firmado por el upstream**: una sola firma para GitHub y F-Droid, y las actualizaciones cruzadas funcionan. Requisitos: la release de GitHub de esa versión debe existir **antes** de que F-Droid compile, y ser la compilada con `release.yml` desde el mismo commit.
**Plan B, si no coincide en el buildserver:** quitar `Binaries:`/`AllowedAPKSigningKeys:`; F-Droid firma con su clave (ver «Dos firmas»). Se investigará la diferencia con `diffoscope` sobre los dos APK.

## Dos firmas: GitHub y F-Droid
- **Release de GitHub** (`release.yml`): firmada con la clave de publicación del propietario (todavía por crear; ver «Cuando el propietario cree su clave»).
- **F-Droid**, por defecto, compila desde las fuentes y **firma con la clave de F-Droid**, distinta. Android no deja actualizar una app con otra firma, así que **quien instale la de GitHub no podrá actualizarla desde F-Droid ni al revés**: hay que desinstalar (y se pierde la disposición salvo que se use «Guardar copia» antes) o instalar solo desde una fuente. Hay que decirlo en el README y en la descripción de la release.
- **Salida alternativa: build reproducible con la firma del upstream.** Si el APK sin firmar que compila F-Droid es idéntico al de GitHub (salvo la firma), F-Droid publica **el APK firmado por el upstream** (con `Binaries:` y `AllowedAPKSigningKeys:` en la receta, ya preparadas y comentadas) y entonces **una sola firma** vale para ambas fuentes. Ver «Build reproducible».
- La clave del upstream sigue sin entrar nunca en el repo ni en F-Droid; solo se publica la huella del certificado.
- Si algún día F-Droid y el upstream firman distinto y se quiere unificar, la única vía es que los usuarios se muevan con copia de seguridad/restauración.

## Lista de comprobación del merge request a fdroiddata
- [ ] Repositorio público con `LICENSE` (GPL-3.0-or-later) y etiqueta `vX.Y.Z` que contiene `fastlane/` y `-Pul.unsigned`.
- [ ] Hacer fork de `gitlab.com/fdroid/fdroiddata`, rama nueva (`com.qtekfun.ultimatelauncher`), copiar `docs/fdroid/com.qtekfun.ultimatelauncher.yml` a `metadata/com.qtekfun.ultimatelauncher.yml`.
- [ ] Quitar los comentarios del borrador que no apliquen y fijar `commit:` a la etiqueta real (o al hash completo).
- [ ] `fdroid readmeta`, `fdroid rewritemeta com.qtekfun.ultimatelauncher`, `fdroid lint com.qtekfun.ultimatelauncher`, `fdroid build -v -l com.qtekfun.ultimatelauncher` y `fdroid scanner com.qtekfun.ultimatelauncher` sin errores.
- [ ] El pipeline de CI del MR en verde (compila, escanea, comprueba el APK).
- [ ] Rellenar la plantilla del MR: «Fixes #» no aplica (app nueva), marcar las casillas: licencia FOSS, sin binarios, sin Anti-Features, build desde fuentes, `AutoName`, categorías, `versionCode` correcto. Enlazar a `docs/09-privacidad.md`.
- [ ] Si se pide un ticket RFP, abrirlo en gitlab.com/fdroid/rfp (opcional; el MR directo es lo habitual).
- [ ] Decidir y anotar en el MR si se publican con la firma del upstream (reproducible) o con la de F-Droid, y avisar del cambio de firma respecto a GitHub.
- [ ] Mensaje en el MR sobre `compileSdk 37.0` y Gradle 9.8.0, por si hace falta esperar al soporte del buildserver.

## Otras consideraciones
- La variante `sync` (`com.qtekfun.ultimatelauncher.sync`, con `INTERNET`) **no** se envía a F-Droid: no tiene cliente de red y llevaría `NonFreeNet` o no, según el revisor. La receta compila solo `default`.
- Las actualizaciones: con `UpdateCheckMode: Tags` y `AutoUpdateMode: Version`, F-Droid detecta una etiqueta nueva `vX.Y.Z`, lee `versionName`/`versionCode` de `app/build.gradle` y genera la entrada de build. Mantener el formato `versionCode N` y `versionName 'X.Y.Z'`.
- Datos de contacto: F-Droid no permite claves de API ni donaciones sin verificar; el proyecto no usa ninguna.

## Cuando el propietario cree su clave
La clave de publicación la crea el propietario; no existe ninguna en el repositorio ni la genera ningún agente.
1. Generar el JKS en su equipo con `keytool` (pasos en `docs/release-signing.md`), guardarlo **fuera del repositorio** con copia de seguridad cifrada y apuntar la huella SHA-256 del certificado.
2. Subir los cuatro secretos del repositorio con `gh secret set`: `UL_KEYSTORE_BASE64`, `UL_KEYSTORE_PASSWORD`, `UL_KEY_ALIAS` y `UL_KEY_PASSWORD`.
3. Publicar la etiqueta `vX.Y.Z`: `release.yml` compila y firma la release de GitHub.
4. Para la vía reproducible de F-Droid, poner esa huella (minúsculas, sin dos puntos) en `AllowedAPKSigningKeys` de la receta y descomentar `Binaries`.
