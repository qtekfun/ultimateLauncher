#!/usr/bin/env python3
"""Parche 0150 (reaplicable, idempotente): todos los menús contextuales con el estilo del menú de recientes del dock.

Tarjeta de cristal oscuro (#F02C2C2E), esquinas de 18 dp, texto blanco de 16 sp, filas de 50 dp (ancho mínimo 220 dp)
con separadores finos, estado pulsado blanco al 20 %, acciones destructivas en rojo, SIN flecha, y aparición con zoom +
fundido de 180 ms desde el icono. La lógica común está en app/.../ui/ContextMenuStyle.kt y los valores en
app/src/main/res/values/ul_menu.xml. Este script solo toca AOSP:
 - ArrowPopup.java: radio, color único, sin flecha, colocación centrada (encima del ancla, si no debajo, acotada con 8 dp),
   animación propia, filas destructivas en rojo.
 - OptionsPopupView.java: las filas van dentro de un contenedor (una sola tarjeta) y sin flecha.
 - PopupContainerWithArrow.kt: las filas de atajos ocupan todo el ancho de la tarjeta (antes ancho fijo).
 - layouts de filas/contenedores, dimens.xml, popup_background.xml, styles.xml (colores de popup en ambos temas).
"""
import pathlib

B = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
CMS = "com.qtekfun.ultimatelauncher.ui.ContextMenuStyle"


def sub(f, old, new, marker=None, count=1):
    p = B / f
    t = p.read_text()
    marker = marker or new
    if marker in t:
        return
    assert t.count(old) == count, (f, old, t.count(old))
    p.write_text(t.replace(old, new))


# ---------------------------------------------------------------- ArrowPopup.java
A = "src/com/android/launcher3/popup/ArrowPopup.java"
sub(A, "import com.android.launcher3.views.BaseDragLayer;\n",
    "import com.android.launcher3.views.BaseDragLayer;\nimport com.qtekfun.ultimatelauncher.ui.ContextMenuStyle; // UltimateLauncher 0150\n")
sub(A, "mOutlineRadius = Themes.getDialogCornerRadius(context);",
    "mOutlineRadius = ContextMenuStyle.cornerRadiusPx(context); // UltimateLauncher 0150")
sub(A, "if (mActivityContext.canUseMultipleShadesForPopup()) {",
    "if (false /* UltimateLauncher 0150: una sola tarjeta */\n                && mActivityContext.canUseMultipleShadesForPopup()) {")
sub(A, "mColors = new int[]{getContext().getColor(R.color.materialColorSurfaceContainer)};",
    "mColors = new int[]{ContextMenuStyle.cardColor(getContext())}; // UltimateLauncher 0150")
sub(A, "    protected boolean shouldAddArrow() {\n        return true;",
    "    protected boolean shouldAddArrow() {\n        return false; // UltimateLauncher 0150: menús sin flecha")
sub(A, "        assignMarginsAndBackgrounds(this);\n        if (shouldAddArrow()) {\n            addArrow();\n        }\n        animateOpen();",
    "        assignMarginsAndBackgrounds(this);\n        ContextMenuStyle.styleRows(this); // UltimateLauncher 0150\n        if (shouldAddArrow()) {\n            addArrow();\n        }\n        animateOpen();")
sub(A, "            @Px int maxHeightPx) {\n        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);\n\n        int extraVerticalSpace",
    "            @Px int maxHeightPx) {\n        if (true) { // UltimateLauncher 0150: colocación común sin flecha\n"
    "            orientCentered(maxHeightPx);\n            return;\n        }\n"
    "        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);\n\n        int extraVerticalSpace",
    marker="orientCentered(maxHeightPx);\n            return;")
sub(A, "            Interpolator interpolator) {\n\n        setPivotForOpenCloseAnimation();\n",
    "            Interpolator interpolator) {\n        if (true) { // UltimateLauncher 0150: zoom + fundido desde el icono\n"
    "            return getUlOpenCloseAnimator(isOpening);\n        }\n\n        setPivotForOpenCloseAnimation();\n",
    marker="return getUlOpenCloseAnimator(isOpening);")

NEW_METHODS = '''    // UltimateLauncher 0150: abscisa (relativa a la tarjeta) del centro del icono, pivote de la animación.
    private float mUlPivotX;

    /**
     * UltimateLauncher 0150: colocación sin flecha. Centrada sobre el objetivo y ENCIMA si cabe; si no, debajo; si
     * tampoco, centrada en vertical junto al objetivo. Acotada a la pantalla con el margen de ContextMenuStyle.
     */
    private void orientCentered(@Px int maxHeightPx) {
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        // Los márgenes entre contenedores se añaden después de este método: se cuentan aquí.
        int numVisibleChildren = 0;
        for (int i = getChildCount() - 1; i >= 0; --i) {
            if (getChildAt(i).getVisibility() == VISIBLE) {
                numVisibleChildren++;
            }
        }
        int childMargins = Math.max(0, numVisibleChildren - 1) * mChildContainerMargin;
        int height = getMeasuredHeight() + childMargins;
        int width = getMeasuredWidth() + getPaddingLeft() + getPaddingRight();

        getTargetObjectLocation(mTempRect);
        InsettableFrameLayout dragLayer = getPopupContainer();
        Rect insets = dragLayer.getInsets();
        Resources res = getResources();
        int gap = res.getDimensionPixelSize(R.dimen.ul_menu_anchor_gap);
        int margin = res.getDimensionPixelSize(R.dimen.ul_menu_screen_margin);
        Rect bounds = new Rect(insets.left, insets.top, dragLayer.getWidth() - insets.right,
                dragLayer.getHeight() - insets.bottom);
        ContextMenuStyle.Placement p = ContextMenuStyle.place(mTempRect, width, height, bounds, gap,
                margin, maxHeightPx);
        mIsAboveIcon = p.getAbove();
        mIsLeftAligned = true;
        int x = p.getX();
        mGravity = 0;
        if (!p.getFits()) {
            // No cabe ni encima ni debajo: centrado en vertical, junto al objetivo (derecha si cabe).
            mGravity = Gravity.CENTER_VERTICAL;
            int rightX = mTempRect.right + gap;
            int leftX = mTempRect.left - gap - width;
            x = rightX + width <= bounds.right - margin ? rightX : Math.max(bounds.left + margin, leftX);
            mIsAboveIcon = true;
        }
        mUlPivotX = ContextMenuStyle.pivotX(mTempRect.centerX(), x, width);
        // Los insets ya se suman al margen izquierdo al añadir la vista: se restan del desplazamiento.
        setX(x - insets.left);
        if (Gravity.isVertical(mGravity)) {
            return;
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (mIsAboveIcon) {
            // Gravedad inferior: al cargar los atajos o añadir márgenes la tarjeta crece hacia arriba.
            lp.gravity = Gravity.BOTTOM;
            lp.bottomMargin = dragLayer.getHeight() - (mTempRect.top - gap);
        } else {
            lp.gravity = Gravity.TOP;
            lp.topMargin = mTempRect.bottom + gap;
        }
    }

    /** UltimateLauncher 0150: entrada/salida con zoom y fundido, pivote en la base centrada sobre el icono. */
    private AnimatorSet getUlOpenCloseAnimator(boolean isOpening) {
        setPivotX(mUlPivotX);
        setPivotY(mIsAboveIcon ? getMeasuredHeight() : 0f);
        float s = ContextMenuStyle.START_SCALE;
        float from = isOpening ? s : 1f;
        float to = isOpening ? 1f : s;
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(this, View.SCALE_X, from, to),
                ObjectAnimator.ofFloat(this, View.SCALE_Y, from, to),
                ObjectAnimator.ofFloat(this, View.ALPHA, isOpening ? 0f : 1f, isOpening ? 1f : 0f));
        set.setDuration(getResources().getInteger(
                isOpening ? R.integer.ul_menu_open_ms : R.integer.ul_menu_close_ms));
        set.setInterpolator(isOpening ? new android.view.animation.DecelerateInterpolator()
                : new android.view.animation.AccelerateInterpolator());
        return set;
    }

'''
sub(A, "    @Override\n    protected void onLayout(boolean changed, int l, int t, int r, int b) {\n        super.onLayout(changed, l, t, r, b);\n\n        // enforce contained",
    NEW_METHODS + "    @Override\n    protected void onLayout(boolean changed, int l, int t, int r, int b) {\n        super.onLayout(changed, l, t, r, b);\n\n        // enforce contained",
    marker="private void orientCentered(")

# ---------------------------------------------------------------- OptionsPopupView.java
O = "src/com/android/launcher3/views/OptionsPopupView.java"
sub(O, "        return mShouldAddArrow;", "        return false; // UltimateLauncher 0150: menús sin flecha", marker="UltimateLauncher 0150: menús sin flecha")
p = B / O
t = p.read_text()
if "UltimateLauncher 0150: filas en un contenedor" not in t:
    a = t.index("    @Override\n    public void assignMarginsAndBackgrounds(ViewGroup viewGroup) {")
    b = t.index("    public static void showNoReturn(")
    t = t[:a] + t[b:]
    old = "        for (OptionItem item : items) {\n            DeepShortcutView view = popup.inflateAndAdd(R.layout.system_shortcut, popup);"
    assert t.count(old) == 1
    t = t.replace(old,
        "        // UltimateLauncher 0150: filas en un contenedor (una sola tarjeta, como los menús de apps).\n"
        "        ViewGroup rows = popup.inflateAndAdd(R.layout.system_shortcut_rows_container, popup);\n"
        "        for (OptionItem item : items) {\n            DeepShortcutView view = popup.inflateAndAdd(R.layout.system_shortcut, rows);")
    p.write_text(t)

# ---------------------------------------------------------------- PopupContainerWithArrow.kt
K = "src/com/android/launcher3/popup/PopupContainerWithArrow.kt"
sub(K, "            v.layoutParams.width = containerWidth\n",
    "            v.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT // UltimateLauncher 0150\n")
sub(K, "        view.layoutParams.width = containerWidth\n",
    "        view.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT // UltimateLauncher 0150\n")

# ---------------------------------------------------------------- recursos
sub("res/layout/system_shortcut.xml",
    '    android:layout_width="@dimen/bg_popup_item_width"\n',
    '    android:layout_width="match_parent"\n    android:minWidth="@dimen/ul_menu_min_width"\n',
    marker="ul_menu_min_width")
sub("res/layout/deep_shortcut.xml",
    '    android:layout_width="@dimen/bg_popup_item_width"\n',
    '    android:layout_width="match_parent"\n    android:minWidth="@dimen/ul_menu_min_width"\n',
    marker="ul_menu_min_width")
for f in ("res/layout/system_shortcut_content.xml", "res/layout/deep_shortcut.xml"):
    sub(f, 'android:textSize="14sp"', 'android:textSize="16sp"')
for f in ("res/layout/system_shortcut_rows_container.xml", "res/layout/deep_shortcut_container.xml"):
    sub(f, '    android:layout_width="wrap_content"\n',
        '    android:layout_width="match_parent"\n    android:clipToOutline="true"\n'
        '    android:divider="@drawable/ul_menu_divider"\n    android:showDividers="middle"\n',
        marker="ul_menu_divider")
sub("res/layout/system_shortcut_icons_container.xml",
    '    android:elevation="@dimen/deep_shortcuts_elevation"/>',
    '    android:clipToOutline="true"\n    android:elevation="@dimen/deep_shortcuts_elevation"/>',
    marker="clipToOutline")
sub("res/drawable/popup_background.xml",
    '    <solid android:color="@color/materialColorSurfaceContainer"/>\n    <corners android:radius="@dimen/dialogCornerRadius"/>',
    '    <solid android:color="@color/ul_menu_card"/>\n    <corners android:radius="@dimen/ul_menu_corner_radius"/>')
sub("res/values/dimens.xml", '<dimen name="bg_popup_item_width">216dp</dimen>', '<dimen name="bg_popup_item_width">220dp</dimen>')
sub("res/values/dimens.xml", '<dimen name="bg_popup_item_height">52dp</dimen>', '<dimen name="bg_popup_item_height">50dp</dimen>')
sub("res/values/dimens.xml", '<dimen name="system_shortcut_header_height">52dp</dimen>', '<dimen name="system_shortcut_header_height">50dp</dimen>')
sub("res/values/dimens.xml", '<dimen name="popup_margin">2dp</dimen>', '<dimen name="popup_margin">6dp</dimen>')
sub("res/values/dimens.xml", '<dimen name="popup_single_item_radius">100dp</dimen>', '<dimen name="popup_single_item_radius">18dp</dimen>')
sub("res/values/dimens.xml", '<dimen name="popup_smaller_radius">4dp</dimen>', '<dimen name="popup_smaller_radius">0dp</dimen>')
p = B / "res/values/styles.xml"
t = p.read_text()
for old, new in (
    ('<item name="popupColorTertiary">@color/popup_color_tertiary_light</item>', '<item name="popupColorTertiary">@color/ul_menu_pressed</item>'),
    ('<item name="popupColorTertiary">@color/popup_color_tertiary_dark</item>', '<item name="popupColorTertiary">@color/ul_menu_pressed</item>'),
    ('<item name="popupTextColor">@color/system_on_surface_light</item>', '<item name="popupTextColor">@color/ul_menu_text</item>'),
    ('<item name="popupTextColor">@color/system_on_surface_dark</item>', '<item name="popupTextColor">@color/ul_menu_text</item>'),
):
    if old in t:
        t = t.replace(old, new)
p.write_text(t)
