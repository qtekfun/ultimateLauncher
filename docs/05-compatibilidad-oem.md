# 05 — Compatibilidad por fabricante

## Familias de ROM

| Marca | ROM (global / China) | Notas |
|---|---|---|
| OPPO (también OnePlus, realme, de la misma familia) | ColorOS | La referencia estética. Launcher del sistema = proveedor de recientes |
| vivo | Funtouch OS / OriginOS | Gestión agresiva de segundo plano |
| Xiaomi | HyperOS | Restricciones de autoarranque y de ejecución en segundo plano |
| Honor | MagicOS | Puede venir con firmware chino sin GMS |

## Qué pasa con cada problema conocido

| Problema | Causa | Respuesta en la app |
|---|---|---|
| El launcher se cierra o los widgets no se actualizan | Gestión agresiva de batería/autoarranque | Asistente que lleva a la pantalla correcta de cada marca |
| No se encuentra dónde elegir launcher predeterminado | Cada ROM lo esconde distinto | Asistente que usa el rol `ROLE_HOME` y, si falla, abre los ajustes de apps predeterminadas y explica la ruta |
| Gestos y recientes sin las animaciones de la marca | El proveedor de recientes es el launcher del sistema | Documentar; no se puede resolver sin root (ver `02`) |
| Widgets propios de la marca ausentes o distintos | Dependen del launcher/servicios del sistema | Lista de widgets probados por marca; aviso en la importación |
| Contadores numéricos en insignias | APIs propietarias | Solo puntos de notificación en v1 |
| Sin búsqueda de Google ni feed | Firmware chino sin GMS | Es un requisito, no un fallo: todo local |

## Asistente de primer arranque (`feature-oemkit`)

Pasos, con detección de marca:
1. Fijar como launcher predeterminado.
2. Autoarranque / inicio automático.
3. Batería sin restricciones para la app.
4. Permitir ventanas emergentes en segundo plano y aviso de "bloquear en recientes" cuando la ROM lo ofrezca (solo informativo).

Reglas de implementación:
- Cada paso intenta una lista de intents candidatos y usa el primero que `resolveActivity` confirme; si ninguno existe, abre los **detalles de la app** en ajustes y muestra instrucciones de texto para esa marca.
- Los componentes concretos **cambian entre versiones de ROM**. Los que siguen son **candidatos de partida que hay que verificar en cada dispositivo** (no están comprobados en esta sesión):
  - OPPO/realme/OnePlus: paquetes de `com.coloros.safecenter` / `com.oppo.safe` (listado de autoarranque).
  - vivo: `com.vivo.permissionmanager` (gestión de inicio en segundo plano).
  - Xiaomi: `com.miui.securitycenter` (gestión de autoarranque).
  - Honor: `com.huawei.systemmanager` (gestión de inicio de aplicaciones).
- Referencia comunitaria para ampliar la lista: dontkillmyapp.com. Las rutas se guardan en un archivo de datos (`oem-intents.json`) para poder corregirlas sin recompilar.

## Interfaz de adaptador

```kotlin
interface OemAdapter {
    val id: String
    fun matches(): Boolean                 // por Build.MANUFACTURER/BRAND y propiedades de la ROM
    fun autostartIntents(): List<Intent>
    fun batteryIntents(): List<Intent>
    fun defaultLauncherHelp(): HelpText
    fun knownIssues(): List<KnownIssue>
}
```

Implementaciones: `ColorOsAdapter`, `VivoAdapter`, `HyperOsAdapter`, `MagicOsAdapter`, `GenericAdapter` (siempre presente).

## Matriz de dispositivos de prueba

Rellenar con los móviles y la tablet reales del usuario antes del hito M1.

| Marca | Modelo | Android | ROM/versión | GMS (sí/no) | Frecuencia Hz | Root (sí/no) |
|---|---|---|---|---|---|---|
| OPPO | | | | | | |
| vivo | | | | | | |
| Xiaomi | | | | | | |
| Honor | | | | | | |
| Tablet (cualquier marca) | | | | | | |

## Lista de comprobación por dispositivo

- [ ] Instalación y fijado como predeterminado
- [ ] Reinicio del teléfono: el launcher vuelve a inicio sin recargar de más
- [ ] Añadir, configurar y redimensionar un widget de terceros
- [ ] Widget del reloj/tiempo de la propia marca (anotar si funciona)
- [ ] Perfil de trabajo: pestañas, insignia, pausa y reanudación
- [ ] Cajón: búsqueda local, desplazamiento rápido, sin elementos extra
- [ ] Carpetas: crear, abrir, cerrar, arrastrar dentro y fuera
- [ ] Apps fijadas en el dock tras reiniciar
- [ ] Después de 12 horas en segundo plano con batería normal: ¿sigue vivo?
- [ ] Gestos del sistema: anotar qué animaciones se pierden
- [ ] Exportar layout aquí e importarlo en otro dispositivo de la matriz
- [ ] Tablet: rejilla propia, rotación, cajón, carpetas y widgets proporcionados
- [ ] Tablet: multiventana y cambio de tamaño de ventana sin perder estado
- [ ] Modo oscuro/claro y cambio de tema
- [ ] Rendimiento: desplazamiento de páginas y cajón sin saltos a la frecuencia nativa

## Informe de incidencias

Cada fallo se anota en `docs/oem-issues.md` con: marca, modelo, ROM, versión, pasos, traza (`adb logcat`) y estado.
