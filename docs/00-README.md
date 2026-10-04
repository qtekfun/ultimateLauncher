# Launcher uniforme para OPPO / vivo / Xiaomi / Honor — Paquete de especificación (SDD)

Nombre: **UltimateLauncher** (`applicationId` propuesto: `com.qtekfun.ultimatelauncher`, confirmar).
Estado: borrador v0.2 — 2026-10-04. Móvil y tablet, `minSdk` 31, sin GMS, privacidad primero.

## Qué es

Un launcher Android para uso propio (con vocación de publicarse como software libre) que da la **misma interfaz en cualquier teléfono**, con la estética y la velocidad de animaciones del launcher de OPPO, sin sus extras, y con un sistema para **clonar la disposición de un dispositivo a otro**. Funciona en **móvil y tablet**, sin servicios de Google y con **privacidad como requisito de diseño** (ver `09`).

## Base técnica elegida

Fork del **Launcher3 de AOSP, rama `android17-release`** (etiqueta `android-17.0.0_r1`), con un andamiaje de compilación Gradle tomando como modelo el de Lawnchair 16-dev. Motivos y alternativas descartadas en `03-base-aosp-y-compilacion.md`.

## Contenido

| Archivo | Para qué |
|---|---|
| `01-requisitos.md` | Qué debe y qué no debe hacer (funcional y no funcional) |
| `02-arquitectura.md` | Módulos, qué se conserva, qué se elimina, puntos de extensión |
| `03-base-aosp-y-compilacion.md` | Cómo obtener y compilar la base, `minSdk` 31 y compatibilidad |
| `04-estetica-y-animaciones-oppo.md` | Tokens de diseño, perfil de animaciones y protocolo de medición |
| `05-compatibilidad-oem.md` | Matriz por fabricante, onboarding, lista de pruebas |
| `06-sync-de-layout.md` | Exportar/importar disposición (archivo/SAF; WebDAV opcional) |
| `07-tareas-e-hitos.md` | Hitos, tareas, criterios de aceptación, CI |
| `08-riesgos-y-decisiones.md` | Riesgos, decisiones tomadas y abiertas, notas legales |
| `09-privacidad.md` | Privacidad, permisos, funcionamiento sin GMS, auditoría |
| `CLAUDE.md` | Instrucciones para Claude Code al trabajar en el repo |

## Orden de lectura para Claude Code

1. `CLAUDE.md`
2. `01` → `02` → `03` → `09`
3. Trabajar por hitos de `07`, leyendo `04`, `05` o `06` cuando toque cada hito.

## Dato clave sobre OPPO

OPPO **no publica** el código de su launcher ni de sus animaciones (solo kernel y componentes GPL). Por eso la estética y las curvas se **miden desde fuera** en teléfonos reales y se guardan como un perfil configurable. Ver `04`.
