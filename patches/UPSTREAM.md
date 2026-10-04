# Seguimiento de AOSP

| Componente | Rama | Commit integrado | Fecha |
|---|---|---|---|
| platform/packages/apps/Launcher3 | android17-release | c612e6ece389f21c40f8cb9cd9a4b44239f00009 | 2026-10-04 |
| platform/frameworks/libs/systemui | android17-release | 11e04f60f563aed48e4ec080bd7bde06bae1b2f3 | 2026-10-04 |

Importación inicial: copia sin historial de `src`, `src_no_quickstep`, `src_plugins`, `shared`, `modules`, `dagger`, `protos`,
`protos_overrides`, `res`, `aconfig`, manifiestos, `proguard.flags` y `tests/shared`. `quickstep/` NO se importa (recientes/gestos del sistema; fuera de alcance, ver docs/02).
El commit "Importación AOSP sin modificar" contiene el estado puro; los cambios posteriores sobre `launcher3-base/` y `systemui-libs/` son los parches (ver patches/NNNN-*.md).
