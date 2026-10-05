#!/usr/bin/env python3
"""Parche 0023 (reaplicable): barra de desplazamiento rápido A-Z siempre visible a la derecha, como OPPO.

Requiere el flag `letter_fast_scroller` = true (tools/gen-flags.py, lista ENABLED; andamiaje propio, no es parche de AOSP).
Con el flag, la base ya construye una lista de letras (ConstraintLayout `scroll_letter_layout`) pero la deja invisible
(alpha 0) hasta que se arrastra el pulgar, con una fila por sección repartida por TODA la altura, un círculo opaco
detrás de cada letra y, en esta versión de ConstraintLayout, letras de tamaño 0x0 (ratio "v,1:1" sin altura fija).
Aquí:
- AllAppsRecyclerView.setLettersToScrollLayout: no reconstruye la lista en cada evento de scroll si las secciones no
  cambian; cada letra mide paso x paso (R.dimen.ul_drawer_az_pitch); pista y letras ocupan paso*filas y se colocan a
  R.dimen.ul_drawer_az_top del borde superior de la pantalla (medido en OPPO: 24 letras, paso 48 px, y=998..2129),
  pegadas a la derecha (R.dimen.ul_drawer_az_end_margin). Se aplica con post() porque se llama desde dentro de un pase
  de layout del RecyclerView y la petición de layout se perdía. Letras visibles en reposo (alpha 1).
- Se sustituye la cadena de restricciones por restricciones explícitas por letra (arriba = fila*paso, borde derecho).
- LetterListTextView: tamaño de letra y color desde tokens, fondo transparente (se conserva la animación de bulto).
- RecyclerViewFastScroller: la lista de letras no se oculta al soltar; el pulgar (medio círculo) solo se dibuja al arrastrar.
"""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
J = R / "src/com/android/launcher3"
def sub(p, old, new):
    p = pathlib.Path(p); t = p.read_text()
    if new in t: return
    assert old in t, (p.name, old)
    p.write_text(t.replace(old, new, 1))

a = J / "allapps/AllAppsRecyclerView.java"
sub(a, "        if (fastScrollSections.isEmpty()) {\n            return;\n        }\n        if (mLetterList != null) {\n            mLetterList.removeAllViews();\n        }",
"""        if (fastScrollSections.isEmpty()) {
            return;
        }
        // UltimateLauncher 0023: no reconstruir la lista de letras en cada evento de scroll.
        StringBuilder ulKey = new StringBuilder();
        for (AlphabeticalAppsList.FastScrollSectionInfo s : fastScrollSections) {
            ulKey.append(s.sectionName).append('|');
        }
        if (mLetterList != null && mLetterList.getChildCount() > 0
                && ulKey.toString().equals(mUlLetterKey)) {
            return;
        }
        mUlLetterKey = ulKey.toString();
        if (mLetterList != null) {
            mLetterList.removeAllViews();
        }""")
sub(a, "    public void setLettersToScrollLayout(",
    "    private String mUlLetterKey = \"\"; // UL 0023\n\n    public void setLettersToScrollLayout(")
sub(a, "        lastLetterListTextView.setVisibility(INVISIBLE);",
    "        lastLetterListTextView.setVisibility(INVISIBLE);\n        lastLetterListTextView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(\n                Math.round(getResources().getDimension(R.dimen.ul_drawer_az_pitch)),\n                Math.round(getResources().getDimension(R.dimen.ul_drawer_az_pitch)),\n                android.view.Gravity.END | android.view.Gravity.TOP)); // UL 0023")
sub(a, "    private ConstraintLayout mLetterList;", "    private android.widget.FrameLayout mLetterList; // UL 0023: FrameLayout en vez de ConstraintLayout")
sub(a, "    public ConstraintLayout getLetterList() {", "    public android.widget.FrameLayout getLetterList() {")
sub(a, "        constraintTextViewsVertically(mLetterList, textViews);\n        mLetterList.setVisibility(VISIBLE);\n        // Set the alpha to 0 to avoid the letter list being shown when it shouldn't be.\n        mLetterList.setAlpha(0);",
"""        for (int ulI = 0; ulI < textViews.size(); ulI++) { // UL 0023: posición explícita en un FrameLayout, sin cadena
            android.widget.FrameLayout.LayoutParams ulCl =
                    (android.widget.FrameLayout.LayoutParams) textViews.get(ulI).getLayoutParams();
            ulCl.topMargin = Math.round(ulI * getResources().getDimension(R.dimen.ul_drawer_az_pitch));
            ulCl.setMarginEnd(Math.round(getResources().getDimension(R.dimen.ul_drawer_az_end_margin)));
            textViews.get(ulI).setLayoutParams(ulCl);
        }
        mLetterList.setVisibility(VISIBLE);
        // UltimateLauncher 0023: pista y letras compactas (paso fijo) y siempre visibles.
        final int ulHeight = Math.round(getResources().getDimension(R.dimen.ul_drawer_az_pitch)
                * textViews.size()) + getScrollBarTop() + getScrollBarMarginBottom();
        final int ulTop = Math.round(getResources().getDimension(R.dimen.ul_drawer_az_top))
                - getScrollBarTop();
        mLetterList.post(() -> {
            View ulParent = (View) mLetterList.getParent();
            for (View ulV : new View[] {mLetterList, mScrollbar}) {
                if (ulV.getLayoutParams() instanceof android.widget.RelativeLayout.LayoutParams) {
                    android.widget.RelativeLayout.LayoutParams ulLp =
                            (android.widget.RelativeLayout.LayoutParams) ulV.getLayoutParams();
                    ulLp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
                    ulLp.removeRule(android.widget.RelativeLayout.ALIGN_TOP);
                    ulLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
                    ulLp.height = ulHeight;
                    ulLp.topMargin = ulTop - ulParent.getPaddingTop();
                    ulV.setLayoutParams(ulLp);
                }
            }
            ulParent.requestLayout();
        });
        mLetterList.setAlpha(1);""")
l = J / "allapps/LetterListTextView.java"
sub(l, "        mTextColor = context.getColor(R.color.materialColorOnSurface);",
    "        mTextColor = context.getColor(R.color.ul_drawer_az_color); // UL 0023")
sub(l, "        setBackground(mLetterBackground);",
    "        setBackground(new android.graphics.drawable.ColorDrawable(\n                android.graphics.Color.TRANSPARENT)); // UL 0023: sin círculo opaco")
sub(l, "        setTextSize(mLetterListTextWidthAndHeight);",
    "        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,\n                getResources().getDimension(R.dimen.ul_drawer_az_text_size)); // UL 0023")
sub(l, "        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(\n                MATCH_CONSTRAINT, WRAP_CONTENT);\n        lp.dimensionRatio = \"v,1:1\";",
    "        int ulPitch = Math.round(getResources().getDimension(R.dimen.ul_drawer_az_pitch));\n        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(\n                ulPitch, ulPitch, android.view.Gravity.END | android.view.Gravity.TOP); // UL 0023: letra de paso x paso")

f = J / "views/RecyclerViewFastScroller.java"
sub(f, "                mRv.getLetterList().animate().alpha(visible ? 1f : 0f)",
    "                mRv.getLetterList().animate().alpha(1f) // UL 0023: siempre visibles")
sub(f, "            canvas.drawCircle(-halfW, halfW, r * 2, mThumbPaint);",
    "            if (mIsDragging) { // UL 0023: en reposo no se ve el pulgar\n                canvas.drawCircle(-halfW, halfW, r * 2, mThumbPaint);\n            }")

# El contenedor de letras pasa de ConstraintLayout a FrameLayout (el ConstraintLayout dejaba las letras en 0x0).
sub(J / "allapps/ActivityAllAppsContainerView.java", "    private ConstraintLayout mFastScrollLetterLayout;", "    private android.widget.FrameLayout mFastScrollLetterLayout; // UL 0023")
sub(J / "allapps/ActivityAllAppsContainerView.java", "    ConstraintLayout getFastScrollerLetterList() {", "    android.widget.FrameLayout getFastScrollerLetterList() { // UL 0023")
sub(f, "        ConstraintLayout mLetterList = mRv.getLetterList();", "        android.widget.FrameLayout mLetterList = mRv.getLetterList(); // UL 0023")
sub(R / "res/layout/all_apps_fast_scroller.xml", "    <androidx.constraintlayout.widget.ConstraintLayout\n        android:id=\"@+id/scroll_letter_layout\"", "    <FrameLayout\n        android:id=\"@+id/scroll_letter_layout\"")
sub(R / "res/layout/all_apps_fast_scroller.xml", "        android:outlineProvider=\"none\"\n        />\n</merge>", "        android:outlineProvider=\"none\"\n        />\n</merge>")
sub(J / "FastScrollRecyclerView.java", "    public ConstraintLayout getLetterList() {", "    public android.widget.FrameLayout getLetterList() { // UL 0023")
