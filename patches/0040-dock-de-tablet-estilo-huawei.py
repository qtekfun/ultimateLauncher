#!/usr/bin/env python3
"""Parche 0040 (reaplicable): dock de tablet estilo Huawei (dos píldoras: fijas | recientes).

 - Launcher.setupViews: UlDockView.attach(this) bajo el hotseat.
 - Launcher.startActivitySafely: RecentApps.record(...) (últimas apps abiertas desde el launcher, solo local).
 - DeviceProfile.getHotseatLayoutPadding: con R.bool.ul_huawei_dock, las celdas del hotseat miden R.dimen.ul_dock_cell
   y el grupo queda centrado (el desplazamiento por los recientes lo aplica UlDockView).
"""
import pathlib
B = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
def sub(f, old, new):
    p = B / f; t = p.read_text()
    if new in t: return
    assert old in t, (f, old)
    p.write_text(t.replace(old, new, 1))
sub("Launcher.java", "        mHotseat.setWorkspace(mWorkspace);\n",
    "        mHotseat.setWorkspace(mWorkspace);\n        com.qtekfun.ultimatelauncher.dock.UlDockView.attach(this); // UltimateLauncher 0040\n")
sub("Launcher.java", "    public RunnableList startActivitySafely(View v, Intent intent, ItemInfo item) {\n",
    "    public RunnableList startActivitySafely(View v, Intent intent, ItemInfo item) {\n        com.qtekfun.ultimatelauncher.dock.RecentApps.record(this, intent, item); // UltimateLauncher 0040\n")
sub("DeviceProfile.java", "        Rect hotseatBarPadding = new Rect();\n        if (isVerticalBarLayout()) {",
    "        Rect hotseatBarPadding = new Rect();\n"
    "        if (!isVerticalBarLayout() && context.getResources().getBoolean(R.bool.ul_huawei_dock)) { // UltimateLauncher 0040\n"
    "            int dockCell = context.getResources().getDimensionPixelSize(R.dimen.ul_dock_cell);\n"
    "            int side = Math.max(0, (mDeviceProperties.getWidthPx()\n"
    "                    - dockCell * mHotseatProfile.getNumShownIcons()) / 2);\n"
    "            hotseatBarPadding.set(side, 0, side, getHotseatBarBottomPadding());\n"
    "            return hotseatBarPadding;\n"
    "        }\n"
    "        if (isVerticalBarLayout()) {")
