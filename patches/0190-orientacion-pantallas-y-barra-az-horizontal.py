#!/usr/bin/env python3
"""Parche 0190 (reaplicable, idempotente): orientación de las pantallas propias y barra A-Z en horizontal.

1) «El selector de fondos se abre en vertical solo». Las pantallas secundarias (Ajustes, selector de fondos, copia,
   importación, apps ocultas, asistente...) no declaran orientación y, con la rotación automática del sistema apagada, se
   abrían con la rotación de usuario aunque el launcher estuviera en horizontal en la tablet.
   - LauncherApplication.onCreate: `ScreenOrientation.install(this)` (código propio, app/.../orientation/) registra un
     ActivityLifecycleCallbacks que fija `requestedOrientation` en `onActivityPreCreated` de toda actividad menos el Launcher.
   - RotationHelper: anota la orientación actual del launcher (inicializar y cada cambio de DeviceProfile) para que las
     demás pantallas la hereden. La lógica es una función pura con pruebas (`ScreenOrientationLogic`).
2) «En horizontal, la barra A-Z del cajón se corta por abajo». El parche 0023 coloca la barra con las medidas de OPPO en
   vertical (primera letra a ~281 dp, paso 13,7 dp x 24 letras), que en una pantalla baja se sale. AllAppsRecyclerView
   recibe `ulFitLetterBar()`: tras el diseño de 0023 recalcula con `AzBarLogic.fit` (sube la barra y, si hace falta,
   reduce paso y texto) y se repite cuando cambia el alto del contenedor (rotación).
Para reaplicar: este script tras 0023 y 0061. No se retocan las inserciones de 0023 (su reaplicación sigue siendo válida)."""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
J = R / "src/com/android/launcher3"


def sub(p, old, new, marker):
    p = pathlib.Path(p)
    t = p.read_text()
    if marker in t:
        return
    assert t.count(old) == 1, (p.name, old, t.count(old))
    p.write_text(t.replace(old, new, 1))


# --- 1) Orientación -------------------------------------------------------------------------------------------------
sub(J / "LauncherApplication.java",
    "        super.onCreate();\n",
    "        super.onCreate();\n"
    "        com.qtekfun.ultimatelauncher.orientation.ScreenOrientation.install(this); // UltimateLauncher 0190\n",
    "UltimateLauncher 0190")

rh = J / "states/RotationHelper.java"
sub(rh,
    "    public void onDeviceProfileChanged(DeviceProfile dp) {\n",
    "    public void onDeviceProfileChanged(DeviceProfile dp) {\n"
    "        // UltimateLauncher 0190: las demás pantallas heredan esta orientación.\n"
    "        com.qtekfun.ultimatelauncher.orientation.ScreenOrientation.rememberLauncherOrientation(\n"
    "                dp.getDeviceProperties().isLandscape());\n",
    "UltimateLauncher 0190")
sub(rh,
    "        setIgnoreAutoRotateSettings(info.isLargeScreen(info.realBounds));\n        ListenableDiffAwareRef",
    "        setIgnoreAutoRotateSettings(info.isLargeScreen(info.realBounds));\n"
    "        // UltimateLauncher 0190\n"
    "        android.content.res.Configuration ulCfg = mActivity.getResources().getConfiguration();\n"
    "        com.qtekfun.ultimatelauncher.orientation.ScreenOrientation.rememberLauncherOrientation(\n"
    "                ulCfg.screenWidthDp > ulCfg.screenHeightDp);\n"
    "        ListenableDiffAwareRef",
    "// UltimateLauncher 0190\n        android.content.res.Configuration")

# --- 2) Barra A-Z ---------------------------------------------------------------------------------------------------
a = J / "allapps/AllAppsRecyclerView.java"
t = a.read_text()
if "UltimateLauncher 0190" not in t:
    old_call = "        mLetterList.setAlpha(1);\n"
    assert t.count(old_call) == 1, t.count(old_call)
    t = t.replace(old_call, old_call + "        ulScheduleLetterFit(); // UltimateLauncher 0190\n", 1)
    anchor = "    private void constraintTextViewsVertically(ConstraintLayout constraintLayout,"
    assert t.count(anchor) == 1
    method = '''    // UltimateLauncher 0190: la barra A-Z cabe en pantallas bajas (horizontal). Se ejecuta tras el post() de 0023.
    private boolean mUlFitListener;

    private void ulScheduleLetterFit() {
        mLetterList.post(this::ulFitLetterBar);
        if (!mUlFitListener && mLetterList.getParent() instanceof View) {
            mUlFitListener = true;
            ((View) mLetterList.getParent()).addOnLayoutChangeListener(
                    (v, l, t, r, b, ol, ot, or, ob) -> {
                        if (b - t != ob - ot) {
                            mLetterList.post(this::ulFitLetterBar);
                        }
                    });
        }
    }

    private void ulFitLetterBar() {
        if (mLetterList == null || mLetterList.getChildCount() == 0
                || !(mLetterList.getParent() instanceof View)) {
            return;
        }
        final View parent = (View) mLetterList.getParent();
        final android.content.res.Resources res = getResources();
        final float density = res.getDisplayMetrics().density;
        final int count = mLetterList.getChildCount();
        final int pitch0 = Math.round(res.getDimension(R.dimen.ul_drawer_az_pitch));
        final com.qtekfun.ultimatelauncher.drawer.AzBarLogic.Fit fit =
                com.qtekfun.ultimatelauncher.drawer.AzBarLogic.fit(
                        parent.getHeight(), count, pitch0,
                        Math.round(res.getDimension(R.dimen.ul_drawer_az_top)),
                        Math.round(com.qtekfun.ultimatelauncher.drawer.AzBarLogic.BOTTOM_RESERVE_DP * density),
                        parent.getPaddingTop() + Math.round(
                                com.qtekfun.ultimatelauncher.drawer.AzBarLogic.MIN_TOP_DP * density));
        final float textPx = res.getDimension(R.dimen.ul_drawer_az_text_size) * fit.getTextScale();
        for (int i = 0; i < count; i++) {
            View letter = mLetterList.getChildAt(i);
            if (!(letter.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams)) {
                continue;
            }
            android.widget.FrameLayout.LayoutParams lp =
                    (android.widget.FrameLayout.LayoutParams) letter.getLayoutParams();
            lp.width = fit.getPitch();
            lp.height = fit.getPitch();
            lp.topMargin = i * fit.getPitch();
            letter.setLayoutParams(lp);
            if (letter instanceof TextView) {
                ((TextView) letter).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textPx);
            }
        }
        final int height = fit.getPitch() * count + getScrollBarTop() + getScrollBarMarginBottom();
        final int topMargin = fit.getTop() - getScrollBarTop() - parent.getPaddingTop();
        for (View v : new View[] {mLetterList, mScrollbar}) {
            if (v != null && v.getLayoutParams() instanceof android.widget.RelativeLayout.LayoutParams) {
                android.widget.RelativeLayout.LayoutParams lp =
                        (android.widget.RelativeLayout.LayoutParams) v.getLayoutParams();
                if (lp.height != height || lp.topMargin != topMargin) {
                    lp.height = height;
                    lp.topMargin = topMargin;
                    v.setLayoutParams(lp);
                }
            }
        }
    }

'''
    t = t.replace(anchor, method + anchor, 1)
    a.write_text(t)
