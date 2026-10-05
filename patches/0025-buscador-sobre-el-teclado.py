#!/usr/bin/env python3
"""Parche 0025 (reaplicable): con el buscador abajo (parche 0021) el teclado lo taparía, porque la ventana del launcher
usa SOFT_INPUT_ADJUST_NOTHING. AppsSearchContainerLayout se sube con la altura del teclado (inset IME menos la barra de
navegación, que ya cuenta en su margen inferior) mediante un WindowInsetsAnimation.Callback (API 30; minSdk 31).
Además, el contenedor oculta la barra A-Z mientras se busca (0023).
"""
import pathlib
J = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
def sub(p, old, new):
    p = pathlib.Path(p); t = p.read_text()
    if new in t: return
    assert old in t, (p.name, old)
    p.write_text(t.replace(old, new, 1))

s = J / "allapps/search/AppsSearchContainerLayout.java"
sub(s, "        mAppsView.getAppsStore().addUpdateListener(this);\n    }",
"""        mAppsView.getAppsStore().addUpdateListener(this);
        // UltimateLauncher 0025: subir el buscador con el teclado.
        setWindowInsetsAnimationCallback(new android.view.WindowInsetsAnimation.Callback(
                android.view.WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override
            public android.view.WindowInsets onProgress(android.view.WindowInsets insets,
                    java.util.List<android.view.WindowInsetsAnimation> animations) {
                ulShiftForIme(insets);
                return insets;
            }

            @Override
            public void onEnd(android.view.WindowInsetsAnimation animation) {
                ulShiftForIme(getRootWindowInsets());
            }
        });
    }

    private void ulShiftForIme(android.view.WindowInsets insets) {
        if (insets == null) {
            return;
        }
        int ime = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom;
        int nav = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom;
        setTranslationY(-Math.max(0, ime - nav));
    }""")

c = J / "allapps/ActivityAllAppsContainerView.java"
sub(c, "    protected void updateSearchResultsVisibility() {\n        if (isSearching()) {",
    "    protected void updateSearchResultsVisibility() {\n        mFastScrollLetterLayout.setAlpha(isSearching() ? 0f : 1f); // UL 0025: sin letras al buscar\n        if (isSearching()) {")
