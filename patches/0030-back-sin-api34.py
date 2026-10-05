#!/usr/bin/env python3
"""Parche 0030 (reaplicable): WidgetPickerActivity y AddItemActivity sin depender de OnBackAnimationCallback (API 34).

Antes implementaban `OnBackAnimationCallback` (API 34) y llamaban a `onBackInvokedDispatcher` (API 33) sin compuerta:
en Android 12-13 el selector de widgets y la confirmación de «añadir elemento» fallaban al abrirse.
Ahora:
  - API >= 33: se registra un `OnBackInvokedCallback` (API 33; la animación de retroceso no aportaba nada, `onBackInvoked`
    solo hacía finish()) -> mismo comportamiento que en API 34+.
  - API 31-32: botón/gesto clásico -> `onBackPressed()`; si hay callbacks de androidx (Compose BackHandler) los atiende
    el `OnBackPressedDispatcher` único de la actividad, y si no, finish().
  - El `OnBackPressedDispatcher` pasa a ser único (antes se creaba uno nuevo en cada lectura de la propiedad).
"""
import pathlib
B = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"

def edit(f, pairs):
    p = B / f; t = p.read_text()
    for old, new in pairs:
        if new in t: continue
        assert old in t, (f, old)
        t = t.replace(old, new)
    p.write_text(t)

DISP_OLD = """    override val onBackPressedDispatcher: OnBackPressedDispatcher
        get() =
            OnBackPressedDispatcher().apply {
                if (Build.VERSION.SDK_INT >= 33) {
                    setOnBackInvokedDispatcher(onBackInvokedDispatcher)
                }
            }
"""
DISP_NEW = """    // UltimateLauncher 0030: un único dispatcher; sin tipos de API 34 (OnBackAnimationCallback).
    private val backDispatcher: OnBackPressedDispatcher by lazy {
        OnBackPressedDispatcher().apply {
            if (Build.VERSION.SDK_INT >= 33) {
                setOnBackInvokedDispatcher(onBackInvokedDispatcher)
            }
        }
    }

    override val onBackPressedDispatcher: OnBackPressedDispatcher
        get() = backDispatcher
"""
REG_OLD_T = """    override fun registerBackDispatcher() {
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            this,
        )
    }

    override fun onBackInvoked() {
        finish()
    }
"""
def reg_new(vis, extra_old_back):
    return f"""    // UltimateLauncher 0030: API 33+ usa OnBackInvokedCallback; en API 31-32 rige onBackPressed().
    {vis}fun registerBackDispatcher() {{
        if (Build.VERSION.SDK_INT >= 33) {{
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback {{ finish() }},
            )
        }}
    }}
"""
IMP_OLD = "import android.window.OnBackAnimationCallback\n"
IMP_NEW = "import android.window.OnBackInvokedCallback\n"

edit("widgetpicker/WidgetPickerActivity.kt", [
    (IMP_OLD, IMP_NEW),
    ("BaseActivity(), OnBackPressedDispatcherOwner, OnBackAnimationCallback, LifecycleOwner {",
     "BaseActivity(), OnBackPressedDispatcherOwner, LifecycleOwner {"),
    (DISP_OLD, DISP_NEW),
    (REG_OLD_T, reg_new("override ", "") + """
    // API 31-32 (sin OnBackInvokedDispatcher): atiende primero los callbacks de androidx y, si no hay, cierra.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backDispatcher.hasEnabledCallbacks()) {
            backDispatcher.onBackPressed()
        } else {
            finish()
        }
    }
"""),
])
edit("dragndrop/AddItemActivity.kt", [
    (IMP_OLD, IMP_NEW),
    ("BaseActivity(), OnBackPressedDispatcherOwner, OnBackAnimationCallback, PinItemAddHandler {",
     "BaseActivity(), OnBackPressedDispatcherOwner, PinItemAddHandler {"),
    (DISP_OLD, DISP_NEW),
    (REG_OLD_T.replace("    override fun registerBackDispatcher", "    public override fun registerBackDispatcher"),
     reg_new("public override ", "")),
])
