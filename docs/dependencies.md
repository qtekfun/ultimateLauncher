# Dependencias (docs/09)

Solo software libre; sin Google Play Services, Firebase ni librerías con red. Versiones en `gradle/libs.versions.toml`.
Pendiente: revisar licencias una a una y escanear trackers (Exodus) — NO hecho.

| Grupo | Dependencias | Licencia (esperada) |
|---|---|---|
| AOSP (copiadas) | Launcher3, frameworks/libs/systemui (iconloader, animationlib, msdl, usertypelib, dynamiccolors-res), frameworks/base `plugin_core` y `log/core` | Apache-2.0 |
| AndroidX | core-ktx, core-animation, constraintlayout, recyclerview, dynamicanimation, fragment, preference, slice-view/core, cardview, window, activity-compose, lifecycle, navigation-compose, graphics-shapes, appfunctions (alpha, sin servicio exportado) | Apache-2.0 |
| Compose | BOM 2026.09.00; material3 1.5.0-alpha29, material-icons-extended | Apache-2.0 |
| Otros | Material Components, Guava (android), Dagger 2.60.1 (KSP), javax.inject, protobuf-javalite 4.36.2, kotlinx-coroutines, jsr305, errorprone-annotations (solo compilación) | Apache-2.0 / BSD |
| Solo pruebas | junit 4.13.2, org.json 20250517 | EPL-1.0 / licencia JSON (solo pruebas, no se empaqueta) |
| Herramientas de medición (fuera del APK) | perfetto (trace processor), numpy, scipy, opencv-headless en `~/work/venv` | Apache-2.0 / BSD |
