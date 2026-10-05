#!/usr/bin/env python3
"""Parche 0172 (reaplicable, idempotente): conecta los paquetes de iconos con la cadena de iconos del launcher.

- `LauncherIconProvider`: `applyIconPack` delega en `IconPackManager.applyPack` y `getApplicationInfoHash` añade el testigo
  del pack (pack + versión instalada + interruptor del fondo) al identificador de frescura de cada icono en caché; sin pack
  no añade nada, así que la caché de siempre sigue válida.
- `ThemeManager.ulNotifyIconSourceChanged()`: avisa a los oyentes de tema; `ModelInitializer` ya reacciona con
  `refreshAndReloadLauncher` (vacía el grupo de iconos, `IconCache.updateIconParams` borra la base y se recarga el modelo).
Archivos: launcher3-base/src/com/android/launcher3/icons/LauncherIconProvider.java, .../graphics/ThemeManager.kt
"""
import pathlib

R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"

p = R / "icons/LauncherIconProvider.java"
t = p.read_text()
if "applyIconPack" not in t:
    old = """    @Override
    public void updateSystemState() {"""
    new = """    // UltimateLauncher 0172: paquetes de iconos
    @Override
    protected Drawable applyIconPack(ComponentInfo info, Drawable original, int iconDpi) {
        return com.qtekfun.ultimatelauncher.iconpack.IconPackManager.get(mContext).applyPack(info, original, iconDpi);
    }

    @Override
    protected PersistedItemState getApplicationInfoHash(ApplicationInfo appInfo) {
        PersistedItemState state = super.getApplicationInfoHash(appInfo);
        String token = com.qtekfun.ultimatelauncher.iconpack.IconPackManager.get(mContext).stateToken();
        return token.isEmpty() ? state : state.withAdditionalValues(token);
    }

    @Override
    public void updateSystemState() {"""
    assert t.count(old) == 1
    t = t.replace(old, new, 1)
    for imp in ("import android.content.pm.ApplicationInfo;", "import android.content.pm.ComponentInfo;",
                "import android.graphics.drawable.Drawable;"):
        if imp not in t:
            t = t.replace("import javax.inject.Inject;", imp + "\nimport javax.inject.Inject;", 1)
    p.write_text(t)

p = R / "graphics/ThemeManager.kt"
t = p.read_text()
if "ulNotifyIconSourceChanged" not in t:
    old = "    @AnyThread fun addChangeListener(listener: ThemeChangeListener) = listeners.add(listener)\n"
    new = """    /**
     * UltimateLauncher 0172: el origen de los iconos cambió (paquete de iconos): se avisa como en un cambio de tema, lo que
     * vacía la caché de iconos y recarga el modelo.
     */
    @AnyThread fun ulNotifyIconSourceChanged() = listeners.forEach { it.onThemeChanged() }

""" + old
    assert t.count(old) == 1
    p.write_text(t.replace(old, new, 1))
