#!/usr/bin/env python3
"""Parche 0181 (reaplicable, idempotente): las apps ocultas no salen en el cajón ni en su búsqueda.

La lista y el filtro son código propio (app/.../hidden/: HiddenAppsLogic, HiddenApps). Este script solo toca AOSP:
 - AllAppsStore.notifyUpdate pasa a ser público (para recargar el cajón al cambiar la lista).
 - AlphabeticalAppsList: filtra las apps ocultas al construir la lista del cajón (antes de ordenar/seccionar) y observa
   los cambios de la lista (SharedPreferences) para recargar sin reiniciar. El oyente se guarda en un campo porque
   SharedPreferences solo guarda referencias débiles.
 - DefaultAppSearchAlgorithm.doSearch: la búsqueda trabaja con la lista del modelo, no con la del almacén, así que
   también se filtra ahí.
No hay fila de sugerencias/predicciones en esta base (docs/pendientes y 09: sin historial de uso)."""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base" / "src/com/android/launcher3/allapps"
HA = "com.qtekfun.ultimatelauncher.hidden.HiddenApps"


def sub(f, old, new, marker):
    p = R / f
    t = p.read_text()
    if marker in t:
        return
    assert t.count(old) == 1, (f, old, t.count(old))
    p.write_text(t.replace(old, new, 1))


sub("AllAppsStore.java", "    private void notifyUpdate() {",
    "    public void notifyUpdate() { // UltimateLauncher 0181: público para recargar al cambiar las apps ocultas",
    "UltimateLauncher 0181")

A = "AlphabeticalAppsList.java"
sub(A, "    private Predicate<ItemInfo> mItemFilter;\n",
    "    private Predicate<ItemInfo> mItemFilter;\n"
    "    // UltimateLauncher 0181: oyente de la lista de apps ocultas (referencia fuerte: SharedPreferences usa débiles).\n"
    "    private Object mUlHiddenListener;\n",
    "mUlHiddenListener")
sub(A, "        if (mAllAppsStore != null) {\n            mAllAppsStore.addUpdateListener(this);\n        }\n",
    "        if (mAllAppsStore != null) {\n            mAllAppsStore.addUpdateListener(this);\n"
    "            mUlHiddenListener = " + HA + ".observe(context, mAllAppsStore::notifyUpdate); // UltimateLauncher 0181\n"
    "        }\n",
    "HiddenApps.observe")
sub(A, "        Stream<AppInfo> appSteam = Stream.of(mAllAppsStore.getApps()).filter(\n                info -> !isPrivateSpaceApp(info));\n",
    "        final java.util.Set<String> ulHidden = " + HA + ".hiddenSet(mActivityContext.asContext()); // UltimateLauncher 0181\n"
    "        Stream<AppInfo> appSteam = Stream.of(mAllAppsStore.getApps()).filter(\n"
    "                info -> !isPrivateSpaceApp(info) && !" + HA + ".isHidden(ulHidden, info));\n",
    "ulHidden")

sub("search/DefaultAppSearchAlgorithm.java",
    "ArrayList<AdapterItem> result = getTitleMatchResult(apps.data, query);",
    "ArrayList<AdapterItem> result = getTitleMatchResult(\n"
    "                    " + HA + ".visibleApps(mAppState.getContext(), apps.data), query); // UltimateLauncher 0181",
    "UltimateLauncher 0181")
