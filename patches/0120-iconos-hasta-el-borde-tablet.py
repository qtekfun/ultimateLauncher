#!/usr/bin/env python3
"""Parche 0120 (reaplicable, idempotente): ajuste «Iconos hasta el borde» (pref_ul_edge_grid) en tablet.

En la MatePad horizontal (2800x1840) la rejilla deja 270 px por lado: relleno del workspace 204 px + relleno de CellLayout 66 px,
que suman el `horizontalMarginLandscape` (120 dp x 2,25) del bloque UL-TABLET de device_profiles.xml (retrato: la mitad).
Con el interruptor encendido y lado corto >= 600 dp (EdgeGrid.eligible) la rejilla escalable:
  - usa margen lateral 0 (workspacePadding y cellLayoutPadding laterales quedan en 0: el segundo es un «inset» del primero), y
  - da a cada celda availableWidth / (paneles * columnas) (EdgeGrid.cellWidthPx) en lugar de minCellSize, de modo que las
    columnas reparten TODO el ancho (los iconos quedan centrados en celdas mas anchas).
La escala (scaleX/scaleY) se sigue calculando con el perfil NORMAL (primera pasada sin borde): el tamano de los iconos y la
altura de las celdas no cambian al activar el ajuste; solo cambia el ancho. Apagado = comportamiento medido de siempre.
No toca telefonos, dock, carpetas ni cajon (el cajon usa allAppsStyle y el parche 0091).
Al cambiar el interruptor, SettingsActivity llama a InvariantDeviceProfile.ulReloadGrid(), que ejecuta onConfigChanged():
reconstruye los DeviceProfile y avisa a Launcher (onIdpChanged), igual que un cambio de rejilla.
Archivos: WorkspaceProfileNonResponsiveFactory.kt, InvariantDeviceProfile.java, SettingsActivity.java, launcher_preferences.xml.
"""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
MARK = "UltimateLauncher 0120"


def sub(path, old, new, count=1):
    p = R / path
    t = p.read_text()
    if MARK in t and new in t:
        return
    assert t.count(old) >= 1, (path, old[:60])
    p.write_text(t.replace(old, new, count))


F = "src/com/android/launcher3/deviceprofile/WorkspaceProfileNonResponsiveFactory.kt"
p = R / F
t = p.read_text()
if "UL 0120" not in t:
    # 1) createWorkspaceProfileScalable: parametro y margen
    t = t.replace("""        isSeascape: Boolean,
        hotseatProfile: HotseatProfileInitialValues,
    ): WorkspaceProfile {
        val cellLayoutBorderSpacePx =
            Point(""", """        isSeascape: Boolean,
        hotseatProfile: HotseatProfileInitialValues,
        edgeToEdge: Boolean = false, // UL 0120
    ): WorkspaceProfile {
        val cellLayoutBorderSpacePx =
            Point(""", 1)
    t = t.replace("""        val desiredWorkspaceHorizontalMarginOriginalPx =
            when {
                isVerticalLayout -> 0
                else -> pxFromDp(inv.horizontalMargin[typeIndex], metrics)
            }""", """        val desiredWorkspaceHorizontalMarginOriginalPx =
            when {
                isVerticalLayout -> 0
                edgeToEdge -> 0 // UL 0120: iconos hasta el borde
                else -> pxFromDp(inv.horizontalMargin[typeIndex], metrics)
            }""", 1)
    t = t.replace("""        var cellWidthPx = pxFromDp(inv.minCellSize.get(typeIndex).x, metrics, scale)
        var cellHeightPx = pxFromDp(inv.minCellSize.get(typeIndex).y, metrics, scale)
""", """        var cellWidthPx = pxFromDp(inv.minCellSize.get(typeIndex).x, metrics, scale)
        if (edgeToEdge) { // UL 0120: las columnas reparten todo el ancho disponible
            cellWidthPx =
                com.qtekfun.ultimatelauncher.grid.EdgeGrid.cellWidthPx(
                    deviceProperties.availableWidthPx,
                    panelCount,
                    inv.numColumns,
                    cellLayoutBorderSpacePx.x,
                )
        }
        var cellHeightPx = pxFromDp(inv.minCellSize.get(typeIndex).y, metrics, scale)
""", 1)
    # 2) internalCreate: parametro y paso
    t = t.replace("""        isSeascape: Boolean,
        hotseatProfile: HotseatProfileInitialValues,
    ): WorkspaceProfile {
        // Icon scale should never exceed 1, otherwise pixellation may occur.""", """        isSeascape: Boolean,
        hotseatProfile: HotseatProfileInitialValues,
        edgeToEdge: Boolean = false, // UL 0120
    ): WorkspaceProfile {
        // Icon scale should never exceed 1, otherwise pixellation may occur.""", 1)
    t = t.replace("""                        hotseatProfile = hotseatProfile,
                        deviceProperties = deviceProperties,
                    )
                    .let { hideWorkspaceLabelsIfNotEnoughSpace(isVerticalLayout, it, inv) }
""", """                        hotseatProfile = hotseatProfile,
                        deviceProperties = deviceProperties,
                        edgeToEdge = edgeToEdge && !isVerticalLayout, // UL 0120
                    )
                    .let { hideWorkspaceLabelsIfNotEnoughSpace(isVerticalLayout, it, inv) }
""", 1)
    # 3) createWorkspaceProfileNonResponsive: primera pasada normal, segunda con borde
    t = t.replace("""        hotseatProfile: HotseatProfileInitialValues,
    ): WorkspaceProfile {
        var workspaceProfile =
            internalCreateWorkspaceProfileNonResponsive(""", """        hotseatProfile: HotseatProfileInitialValues,
    ): WorkspaceProfile {
        // UL 0120: «Iconos hasta el borde» (solo tablet, rejilla escalable); la escala sale del perfil normal.
        val edgeToEdge =
            isScalableGrid &&
                com.qtekfun.ultimatelauncher.grid.EdgeGrid.enabled(context) &&
                com.qtekfun.ultimatelauncher.grid.EdgeGrid.eligible(
                    min(deviceProperties.widthPx, deviceProperties.heightPx) / metrics.density,
                    deviceProperties.isTwoPanels,
                    isVerticalLayout,
                )
        var workspaceProfile =
            internalCreateWorkspaceProfileNonResponsive(""", 1)
    t = t.replace("""                    isSeascape = isSeascape,
                    hotseatProfile = hotseatProfile,
                )
            extraHeight =""", """                    isSeascape = isSeascape,
                    hotseatProfile = hotseatProfile,
                    edgeToEdge = edgeToEdge, // UL 0120
                )
            extraHeight =""", 1)
    assert t.count("UL 0120") >= 6, t.count("UL 0120")
    p.write_text(t)

sub("src/com/android/launcher3/InvariantDeviceProfile.java",
    "    private Object[] toModelState() {",
    """    /** UltimateLauncher 0120: reconstruye los DeviceProfile (p. ej. al cambiar «Iconos hasta el borde»). */
    public void ulReloadGrid() {
        mMainExecutor.execute(this::onConfigChanged);
    }

    private Object[] toModelState() {""")

sub("src/com/android/launcher3/settings/SettingsActivity.java",
    "                case \"pref_ul_dock_recents_clear\": // UltimateLauncher 0092",
    """                case com.qtekfun.ultimatelauncher.grid.EdgeGrid.KEY: // UltimateLauncher 0120
                    // Solo tablet (lado corto >= 600 dp); al cambiar se reconstruye la rejilla tras guardar el valor.
                    preference.setOnPreferenceChangeListener((pref, newValue) -> {
                        InvariantDeviceProfile.INSTANCE.get(getContext()).ulReloadGrid();
                        return true;
                    });
                    return info.isLargeScreen(info.realBounds);
                case "pref_ul_dock_recents_clear": // UltimateLauncher 0092""")

px = R / "res/xml/launcher_preferences.xml"
t = px.read_text()
if "pref_ul_edge_grid" not in t:
    add = '''    <!-- UltimateLauncher 0120 -->
    <SwitchPreference
        android:key="pref_ul_edge_grid"
        android:title="@string/ul_pref_edge_grid"
        android:summary="@string/ul_pref_edge_grid_summary"
        android:defaultValue="false"
        android:persistent="true" />

'''
    px.write_text(t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1))
