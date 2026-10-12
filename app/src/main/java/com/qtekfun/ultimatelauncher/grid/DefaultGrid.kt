// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.grid

import com.android.launcher3.InvariantDeviceProfile

/**
 * Rejilla por defecto según el tipo de dispositivo (parche 0009; plegables en docs/11).
 *
 * Móvil y tablet usan las rejillas medidas (`ultimate_phone`, `ultimate_tablet`). Un plegable (`TYPE_MULTI_DISPLAY`:
 * una pantalla de móvil y otra de tablet) o un escritorio devuelven null: se usan las rejillas de AOSP que admiten esa
 * categoría (`phone|multi_display`), con su disposición exterior/interior y los dos paneles, hasta que haya medidas.
 * Forzar `ultimate_phone` (solo `phone`) hacía que la base eligiera entre TODAS las rejillas, incluidas las de tablet.
 */
object DefaultGrid {
    @JvmStatic
    fun forDeviceType(deviceType: Int): String? = when (deviceType) {
        InvariantDeviceProfile.TYPE_PHONE -> "ultimate_phone"
        InvariantDeviceProfile.TYPE_TABLET -> "ultimate_tablet"
        else -> null
    }
}
