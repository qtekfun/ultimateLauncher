# Decisiones

| Fecha | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| 2026-10-04 | El repo es ~/repos/ultimateLauncher (ya era un repo git vacío), no ~/ultimatelauncher | Crear ~/ultimatelauncher | Es el directorio de trabajo de la sesión |
| 2026-10-04 | Dispositivo de trabajo = OPPO CPH2841 (wifi), por indicación del usuario a mitad de sesión | PGEM10 (USB) | El PGEM10 se usa para otra cosa |
| 2026-10-04 | Medición de animaciones sin cámara de alta velocidad: Perfetto si es posible sin root; si no, vídeo con sellos de tiempo reales, mediana de 5, marcado "aproximado" | Cámara 240 fps (docs/04) | Indicación del usuario |
| 2026-10-04 | Base = Launcher3 `android17-release` sin quickstep, andamiaje Gradle propio; NO se usó Lawnchair como código (solo se leyeron sus archivos Gradle para fijar versiones del plugin protobuf/protoc y de AGP/Kotlin, ya presentes en otros proyectos del usuario). Licencia Lawnchair: Apache-2.0 según su LICENSE.txt, no se copió nada | Plan B Lawnchair 16-dev | M0 se resolvió en ~1,25 h, muy por debajo de las 3 h |
| 2026-10-04 | Un módulo Gradle por librería systemui (iconloader, animationlib, msdllib, usertypelib) | Un módulo agregado | Cada lib necesita su propio paquete `R` |
| 2026-10-04 | Sin `QUERY_ALL_PACKAGES`; se usa `<queries>` con intent MAIN/LAUNCHER | Declarar QUERY_ALL_PACKAGES | Probado: funciona y cumple docs/09 |
| 2026-10-04 | Flags aconfig de Launcher3: todos `false` (script `tools/gen-flags.py`) | Valores de release de AOSP | No están disponibles fuera del árbol; comportamiento clásico/conservador |
| 2026-10-04 | `androidx.compose.material3` 1.5.0-alpha29 (el 1.4.0 de la BOM no expone la API Expressive que usa el selector de widgets) | BOM estable | Compila el módulo widgetpicker |
| 2026-10-04 | Se mantienen AppFunctions solo como código compilado (androidx alpha); el servicio NO está en el manifiesto, así que no se expone nada | Quitar el módulo | Dagger/`workspacefunctions` lo referencian; quitarlo exigía muchos parches |
| 2026-10-04 | Decisiones abiertas de docs/08 tomadas: (1) licencia GPLv3 (propuesta), (2) variante `sync` se mantiene declarada pero sin implementar esta noche, (4) matriz: OPPO CPH2841, (5) repo local sin remoto, rama `master`, (6) ajustes: preferencias estándar de la base, (7) Private Space solo con minSdk 35: no | — | Parten de la propuesta de docs/08 |
| 2026-10-05 | Borrados los emuladores Android (AVD `ul31`, `ul34`), sus imágenes y el paquete `emulator` del SDK; eran los únicos del PC (libvirt/`virsh` no se tocó) | Dejarlos | Petición del usuario; además el emulador fallaba con SIGSEGV |
| 2026-10-05 | La compensación de tamaño de icono se guarda como «calibración» en los tokens (no cambia el valor medido 57,1 dp) | Cambiar iconSize medido | Es una propiedad de Launcher3, no de OPPO; mantiene los tokens fieles a la medición |
| 2026-10-05 | No se usó el MatePad de Huawei (MRO-W09, Android 12, sin GMS) que apareció por USB | Probar M3b/API 31 ahí | El usuario limitó las pruebas al OPPO; propuesto como siguiente paso |
