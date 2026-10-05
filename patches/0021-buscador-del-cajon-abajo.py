#!/usr/bin/env python3
"""Parche 0021 (reaplicable): buscador del cajón abajo, como OPPO (drawer.searchBar = "bottom").

La base trae las ramas «buscador flotante» (lista a partir del borde superior, hueco reservado abajo, rueda de
desplazamiento por encima), pero nadie las activa ni coloca el buscador (eso lo hacía código de quickstep).
Aquí se activan y el buscador se queda como hijo del propio cajón, pegado abajo (así se mueve con la hoja y la
búsqueda local sigue siendo la misma AppsSearchContainerLayout/AllAppsSearchBarController).

- ActivityAllAppsContainerView: isSearchBarFloating() = true; el buscador siempre se añade al cajón (no al DragLayer);
  altura y márgenes desde R.dimen.ul_drawer_search_* (tokens); hueco inferior de la lista = R.dimen.ul_drawer_search_reserved.
- AppsSearchContainerLayout: ancho = ancho útil menos márgenes laterales de tokens; sin desplazamiento «contentOverlap»;
  setInsets aplica el margen inferior (barra de gestos) en lugar del superior.
- FloatingHeaderView: no suma la altura del buscador al espacio de la cabecera (ya no está arriba).
"""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
J = R / "src/com/android/launcher3"
def sub(p, old, new):
    p = pathlib.Path(p); t = p.read_text()
    if new in t: return
    assert old in t, (p.name, old)
    p.write_text(t.replace(old, new, 1))

c = J / "allapps/ActivityAllAppsContainerView.java"
sub(c, "        if (!isSearchBarFloating()) {\n            // Add the search box above everything else",
    "        if (true) { // UltimateLauncher 0021: el buscador va siempre dentro del cajón, abajo\n            // Add the search box above everything else")
sub(c, "            addView(mSearchContainer);\n",
    """            RelativeLayout.LayoutParams ulLp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelSize(R.dimen.ul_drawer_search_height));
            ulLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            ulLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            ulLp.bottomMargin = getResources().getDimensionPixelSize(
                    R.dimen.ul_drawer_search_bottom_margin);
            mSearchContainer.setLayoutParams(ulLp); // UL 0021
            addView(mSearchContainer);
""")
sub(c, "        super.onAttachedToWindow();\n        if (isSearchBarFloating()) {",
    "        super.onAttachedToWindow();\n        if (isSearchBarFloating() && mSearchContainer.getParent() == null) { // UL 0021")
sub(c, "            scrollerLayoutParams.bottomMargin = mSearchContainer.getHeight()\n                    + getResources().getDimensionPixelSize(\n                            R.dimen.fastscroll_bottom_margin_floating_search);",
    "            scrollerLayoutParams.bottomMargin = getResources().getDimensionPixelSize(\n                    R.dimen.ul_drawer_search_reserved); // UL 0021")
sub(c, "                    bottomOffset += mSearchContainer.getHeight();",
    "                    bottomOffset += getResources().getDimensionPixelSize(\n                            R.dimen.ul_drawer_search_reserved); // UL 0021")
sub(c, "    protected boolean isSearchBarFloating() {\n        return mSearchUiDelegate.isSearchBarFloating();",
    "    protected boolean isSearchBarFloating() {\n        return true; // UltimateLauncher 0021: buscador abajo")
if "import android.view.ViewGroup;" not in c.read_text():
    sub(c, "import android.view.View;\n", "import android.view.View;\nimport android.view.ViewGroup;\n")

s = J / "allapps/search/AppsSearchContainerLayout.java"
sub(s, "        int myWidth = rowWidth - iconPadding + getPaddingLeft() + getPaddingRight();\n        super.onMeasure(makeMeasureSpec(myWidth, EXACTLY), heightMeasureSpec);",
    "        int myWidth = myRequestedWidth - 2 * getResources().getDimensionPixelSize(\n                R.dimen.ul_drawer_search_side_margin); // UltimateLauncher 0021\n        super.onMeasure(makeMeasureSpec(myWidth, EXACTLY), heightMeasureSpec);")
sub(s, "        offsetTopAndBottom(mContentOverlap);", "        // UltimateLauncher 0021: sin desplazamiento vertical (buscador abajo)")
sub(s, "        mlp.topMargin = insets.top;\n        requestLayout();",
    "        mlp.topMargin = 0; // UltimateLauncher 0021: buscador abajo\n        mlp.bottomMargin = insets.bottom + getResources().getDimensionPixelSize(\n                R.dimen.ul_drawer_search_bottom_margin);\n        requestLayout();")

sub(J / "allapps/FloatingHeaderView.java",
    "boolean shouldAddSearchBarHeight = mSearchBarOffset > 0 && !Flags.floatingSearchBar();",
    "boolean shouldAddSearchBarHeight = false; // UltimateLauncher 0021: el buscador está abajo")

# Texto del buscador alineado a la izquierda como OPPO (icono + «Buscar»)
x = R / "res/layout/search_container_all_apps.xml"
sub(x, 'android:gravity="center"', 'android:gravity="center_vertical|start"')
sub(x, 'android:padding="8dp"', 'android:paddingStart="20dp"\n    android:paddingEnd="8dp"')

# La lista termina sobre el buscador (OPPO no deja ver los iconos a través de la píldora): se recorta el dibujo
# del RecyclerView por abajo con setClipBounds (el relleno de la píldora es muy translúcido).
sub(c, "            mRecyclerView.addItemDecoration(focusedItemDecorator);",
    """            mRecyclerView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or2, ob) -> v.setClipBounds(
                    new Rect(0, 0, r - l, (b - t) - v.getPaddingBottom()))); // UL 0021
            mRecyclerView.addItemDecoration(focusedItemDecorator);""")

# FloatingHeaderView reescribe los recortes del RecyclerView al desplazarse: conservar el recorte inferior.
h = J / "allapps/FloatingHeaderView.java"
sub(h, "        if (mMainRV != null) {\n            mMainRV.setClipBounds(mRVClip);\n        }\n        if (mWorkRV != null) {\n            mWorkRV.setClipBounds(mRVClip);\n        }\n        if (mSearchRV != null) {\n            mSearchRV.setClipBounds(mRVClip);\n        }",
"""        // UltimateLauncher 0021: la lista termina sobre el buscador (recorte inferior = relleno inferior)
        if (mMainRV != null) {
            mRVClip.bottom = mMainRV.getHeight() > 0
                    ? mMainRV.getHeight() - mMainRV.getPaddingBottom() : Integer.MAX_VALUE;
            mMainRV.setClipBounds(mRVClip);
        }
        if (mWorkRV != null) {
            mRVClip.bottom = mWorkRV.getHeight() > 0
                    ? mWorkRV.getHeight() - mWorkRV.getPaddingBottom() : Integer.MAX_VALUE;
            mWorkRV.setClipBounds(mRVClip);
        }
        if (mSearchRV != null) {
            mRVClip.bottom = mSearchRV.getHeight() > 0
                    ? mSearchRV.getHeight() - mSearchRV.getPaddingBottom() : Integer.MAX_VALUE;
            mSearchRV.setClipBounds(mRVClip);
        }""")
