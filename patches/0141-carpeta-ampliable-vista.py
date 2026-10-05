#!/usr/bin/env python3
"""Parche 0141 (reaplicable, idempotente): carpetas ampliables, vista.
 - PreviewBackground.setup: si la carpeta está ampliada, la baldosa (previewSize y desplazamientos) es el cuadrado de
   FolderExpand.backgroundTile en vez del icono de 1 celda; así el fondo, el arrastre, las animaciones y el punto de
   notificación usan los mismos límites.
 - FolderIcon.dispatchDraw: en vez de los mini-iconos del preview, FolderExpand.drawIcons dibuja las apps (2x2 hasta 4,
   3x3 hasta 9, «+N» si hay más).
 - FolderIcon.onMeasure: FolderExpand.layoutLabel coloca el nombre bajo la baldosa (y lo devuelve a su sitio en 1x1).
 - FolderIcon.onTouchEvent: guarda dónde se tocó (el clic llega sin coordenadas, ver 0142).
 - FolderIcon.drawDot: el punto de notificaciones se coloca en la esquina de la baldosa.
 - FolderIcon.onDragEnter: sin la animación «aceptar» de 1 celda cuando está ampliada.
 - FolderIcon.onItemsChanged / updatePreviewItems(Predicate): se descartan los iconos en caché.
 - ShortcutAndWidgetContainer.measureChild: sin relleno superior de centrado en carpetas ampliadas."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
FE = "com.qtekfun.ultimatelauncher.folder.FolderExpand"


def sub(f, old, new, marker):
    p = R / f
    t = p.read_text()
    if marker in t:
        return
    assert t.count(old) == 1, (f, old, t.count(old))
    p.write_text(t.replace(old, new, 1))


sub("folder/PreviewBackground.java",
    "        basePreviewOffsetY = topPadding + grid.getFolderProfile().getFolderIconOffsetYPx();\n",
    "        basePreviewOffsetY = topPadding + grid.getFolderProfile().getFolderIconOffsetYPx();\n"
    "        int[] ulTile = " + FE + ".backgroundTile(invalidateDelegate, availableSpaceX); // UltimateLauncher 0141\n"
    "        if (ulTile != null) {\n"
    "            previewSize = ulTile[0];\n"
    "            basePreviewOffsetX = ulTile[1];\n"
    "            basePreviewOffsetY = ulTile[2];\n"
    "        }\n", "FolderExpand.backgroundTile")

F = "folder/FolderIcon.java"
sub(F, "        mPreviewItemManager.draw(canvas);\n\n        if (!mBackground.drawingDelegated()) {\n            mBackground.drawBackgroundStroke(canvas);",
    "        if (" + FE + ".isExpanded(this)) { // UltimateLauncher 0141\n"
    "            mBackground.getBounds(mUlTileBounds);\n"
    "            " + FE + ".drawIcons(this, canvas, mUlTileBounds);\n"
    "        } else {\n"
    "            mPreviewItemManager.draw(canvas);\n"
    "        }\n\n        if (!mBackground.drawingDelegated()) {\n            mBackground.drawBackgroundStroke(canvas);",
    "FolderExpand.drawIcons")
sub(F, "    private Rect mTouchArea = new Rect();\n",
    "    private Rect mTouchArea = new Rect();\n    private final Rect mUlTileBounds = new Rect(); // UltimateLauncher 0141\n",
    "mUlTileBounds = new Rect")
sub(F, "    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {\n",
    "    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {\n"
    "        " + FE + ".layoutLabel(this, widthMeasureSpec, heightMeasureSpec); // UltimateLauncher 0141\n",
    "FolderExpand.layoutLabel")
sub(F, "    public boolean onTouchEvent(MotionEvent event) {\n        return onDelegateTouchEvent(event);",
    "    public boolean onTouchEvent(MotionEvent event) {\n"
    "        " + FE + ".recordTouch(this, event); // UltimateLauncher 0141\n"
    "        return onDelegateTouchEvent(event);",
    "FolderExpand.recordTouch")
sub(F, "            Utilities.scaleRectAboutCenter(iconBounds, iconScale);\n",
    "            Utilities.scaleRectAboutCenter(iconBounds, iconScale);\n"
    "            " + FE + ".dotBounds(this, iconBounds); // UltimateLauncher 0141\n",
    "FolderExpand.dotBounds")
sub(F, "        mBackground.animateToAccept(cl, lp.getCellX(), lp.getCellY());\n",
    "        if (!" + FE + ".isExpanded(this)) { // UltimateLauncher 0141\n"
    "            mBackground.animateToAccept(cl, lp.getCellX(), lp.getCellY());\n"
    "        }\n",
    "UltimateLauncher 0141\n            mBackground.animateToAccept")
sub(F, "    public void onItemsChanged(boolean animate) {\n",
    "    public void onItemsChanged(boolean animate) {\n"
    "        " + FE + ".invalidateIcons(this); // UltimateLauncher 0141\n",
    "FolderExpand.invalidateIcons(this); // UltimateLauncher 0141\n        updatePreviewItems(false)")
sub(F, "    public void updatePreviewItems(Predicate<ItemInfo> itemCheck) {\n",
    "    public void updatePreviewItems(Predicate<ItemInfo> itemCheck) {\n"
    "        " + FE + ".invalidateIcons(this); // UltimateLauncher 0141 (icono actualizado)\n",
    "(icono actualizado)")
sub("ShortcutAndWidgetContainer.java",
    "            child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, 0);\n",
    "            cellPaddingY = " + FE + ".cellPaddingY(child, cellPaddingY); // UltimateLauncher 0141\n"
    "            child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, 0);\n",
    "FolderExpand.cellPaddingY")
