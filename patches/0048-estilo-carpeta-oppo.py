#!/usr/bin/env python3
"""Parche 0048 (reaplicable, idempotente): carpeta abierta al estilo OPPO medido (sin tarjeta, título arriba,
fondo desenfocado, inicio oculto). Lógica en app/.../folder/FolderStyle.kt; interruptor pref_ul_folder_style.

Folder.java:
 - constructor: tarjeta transparente.
 - onFinishInflate (tras mFolderName): título arriba, texto blanco.
 - centerAboutIcon: posición centrada con el título a la altura de OPPO.
 - animateOpen: FolderStyle.onOpen; animateClosed / closeComplete: FolderStyle.onClose.
launcher_preferences.xml: interruptor."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
F = "com.qtekfun.ultimatelauncher.folder.FolderStyle"
p = R / "src/com/android/launcher3/folder/Folder.java"; t = p.read_text()
def sub(old, new):
    global t
    if new in t: return
    assert old in t, old
    t = t.replace(old, new, 1)
sub("        mBackground.setCallback(this);\n",
    "        mBackground.setCallback(this);\n"
    f"        if ({F}.transparentCard(context)) mBackground.setColor(android.graphics.Color.TRANSPARENT); // UltimateLauncher 0048\n")
sub("        mFolderName.setOnBackKeyListener(this);\n",
    "        mFolderName.setOnBackKeyListener(this);\n"
    f"        {F}.styleFolder(this, mFooter, mFolderName); // UltimateLauncher 0048\n")
sub("        left = inOutPosition[0];\n        top = inOutPosition[1];\n",
    "        left = inOutPosition[0];\n        top = inOutPosition[1];\n"
    f"        int[] ulPos = new int[]{{left, top}}; // UltimateLauncher 0048\n"
    f"        {F}.position((com.android.launcher3.Launcher) mActivityContext, width, getFooterHeight(), ulPos);\n"
    "        left = ulPos[0];\n        top = ulPos[1];\n")
sub("        mIsOpen = true;\n\n        BaseDragLayer dragLayer = mActivityContext.getDragLayer();",
    "        mIsOpen = true;\n"
    f"        {F}.onOpen((com.android.launcher3.Launcher) mActivityContext); // UltimateLauncher 0048\n\n"
    "        BaseDragLayer dragLayer = mActivityContext.getDragLayer();")
sub("    private void animateClosed() {\n",
    "    private void animateClosed() {\n"
    f"        {F}.onClose((com.android.launcher3.Launcher) mActivityContext); // UltimateLauncher 0048\n")
sub("    private void closeComplete(boolean wasAnimated) {\n        mIsOpen = false;\n",
    "    private void closeComplete(boolean wasAnimated) {\n        mIsOpen = false;\n"
    f"        {F}.onClose((com.android.launcher3.Launcher) mActivityContext); // UltimateLauncher 0048\n")
p.write_text(t)
pa = R / "src/com/android/launcher3/folder/FolderAnimationManager.java"; ta = pa.read_text()
old = "        mFolderBackground.mutate();\n        mFolderBackground.setColor(mIsOpening ? initialColor : finalColor);\n"
new = ("        mFolderBackground.mutate();\n"
       "        mFolderBackground.setColor(com.qtekfun.ultimatelauncher.folder.FolderStyle.transparentCard(mContext)\n"
       "                ? android.graphics.Color.TRANSPARENT : (mIsOpening ? initialColor : finalColor)); // UltimateLauncher 0048\n")
if "UltimateLauncher 0048" not in ta:
    assert old in ta; ta = ta.replace(old, new, 1)
    old2 = 'mBgColorAnimator = getAnimator(mFolderBackground, "color", initialColor, finalColor);'
    assert old2 in ta
    ta = ta.replace(old2, 'mBgColorAnimator = getAnimator(mFolderBackground, "color",\n                com.qtekfun.ultimatelauncher.folder.FolderStyle.transparentCard(mContext) ? 0 : initialColor,\n                com.qtekfun.ultimatelauncher.folder.FolderStyle.transparentCard(mContext) ? 0 : finalColor); // UltimateLauncher 0048', 1)
    pa.write_text(ta)
px = R / "res/xml/launcher_preferences.xml"; t = px.read_text()
if "pref_ul_folder_style" not in t:
    add = '''
    <!-- UltimateLauncher 0048 -->
    <SwitchPreference
        android:key="pref_ul_folder_style"
        android:title="@string/ul_pref_folder_style"
        android:summary="@string/ul_pref_folder_style_summary"
        android:defaultValue="true"
        android:persistent="true" />

'''
    px.write_text(t.replace("</androidx.preference.PreferenceScreen>", add + "</androidx.preference.PreferenceScreen>", 1))
