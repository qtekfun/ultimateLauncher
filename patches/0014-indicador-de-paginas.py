#!/usr/bin/env python3
"""Parche 0014 (reaplicable): indicador de páginas como OPPO.

1) PageIndicatorDots.onDraw dibuja el punto también con una sola página (AOSP lo oculta con < 2).
2) Workspace.setPageIndicatorInset sube el indicador R.dimen.ul_page_indicator_lift (tokens, generado).
"""
import pathlib
B = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
def sub(f, old, new):
    p = B / f; t = p.read_text()
    if new in t: return
    assert old in t, (f, old)
    p.write_text(t.replace(old, new, 1))
sub("pageindicators/PageIndicatorDots.java", "    protected void onDraw(Canvas canvas) {\n        if (mNumPages < 2) {",
    "    protected void onDraw(Canvas canvas) {\n        if (mNumPages < 1) { // UltimateLauncher 0014: punto también con una página")
sub("Workspace.java", "lp.bottomMargin = grid.getHotseatProfile().getBarSizePx();",
    "lp.bottomMargin = grid.getHotseatProfile().getBarSizePx()\n                + getResources().getDimensionPixelSize(R.dimen.ul_page_indicator_lift); // UltimateLauncher 0014")
# 3) El punto no se oculta solo cuando hay una sola página (OPPO lo mantiene visible).
sub("pageindicators/PageIndicatorDots.java", "    private void hideAfterDelay() {\n        mDelayedPaginationFadeHandler.removeCallbacksAndMessages(null);\n",
    "    private void hideAfterDelay() {\n        mDelayedPaginationFadeHandler.removeCallbacksAndMessages(null);\n        if (mNumPages <= 1) return; // UltimateLauncher 0014: con una página el punto se queda\n")
sub("pageindicators/PageIndicatorDots.java", "        if (mShouldAutoHide && mTotalScroll == 0) {",
    "        if (mShouldAutoHide && mTotalScroll == 0 && mNumPages > 1) { // UltimateLauncher 0014")
