#!/usr/bin/env python3
"""Parche 0090 (reaplicable): ATRAS ya no cierra el launcher en Android 12-13 (API < 34).

Hallazgo en la MatePad (Android 12, build con R8): al pulsar ATRAS, `Launcher.onBackPressed()` llamaba a
`getOnBackAnimationCallback().onBackInvoked()` por la interfaz `android.window.OnBackAnimationCallback` (API 34). D8 incluye un
stub VACIO de esa interfaz (sin el metodo abstracto `onBackInvoked`) y ART lanza
`NoSuchMethodError: No interface method onBackInvoked()V in class Landroid/window/OnBackAnimationCallback` -> el proceso muere y
EMUI devuelve el rol de inicio al launcher de Huawei.

Solucion: con API < 34, `onBackPressed()` sigue el mismo orden de prioridad (modo de accion, arrastre, vista flotante, manejadores,
estado) pero sin invocar nada a traves de la interfaz de API 34; los manejadores personalizados (interfaz) se llaman por reflexion.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/Launcher.java"
t = p.read_text()
old = """    public void onBackPressed() {
        getOnBackAnimationCallback().onBackInvoked();
    }
"""
new = """    public void onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // UltimateLauncher 0090
            onBackPressedPreU();
            return;
        }
        getOnBackAnimationCallback().onBackInvoked();
    }

    /** UltimateLauncher 0090: ATRAS sin la interfaz OnBackAnimationCallback (stub vacio de D8 en API < 34). */
    @android.annotation.SuppressLint("NewApi") // canHandleBack es de nuestra interfaz; solo se usa por reflexión en API < 34
    private void onBackPressedPreU() {
        if (isInAutoCancelActionMode()) {
            finishAutoCancelActionMode();
            return;
        }
        if (mDragController.isDragging()) {
            mDragController.cancelDrag();
            return;
        }
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(Launcher.this);
        if (topView != null && topView.canHandleBack()) {
            topView.onBackInvoked(); // metodo de clase, no de la interfaz
            return;
        }
        for (BackPressHandler handler : mBackPressedHandlers) {
            if (handler.canHandleBack()) {
                try {
                    java.lang.reflect.Method m = handler.getClass().getMethod("onBackInvoked");
                    m.setAccessible(true);
                    m.invoke(handler);
                } catch (ReflectiveOperationException e) {
                    Log.e(TAG, "ATRAS: no se pudo invocar el manejador " + handler, e);
                }
                return;
            }
        }
        onStateBack();
    }
"""
if "UltimateLauncher 0090" not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
