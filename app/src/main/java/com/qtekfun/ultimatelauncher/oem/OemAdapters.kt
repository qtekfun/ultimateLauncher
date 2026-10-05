// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.oem

import android.content.Context
import android.os.Build

/** Registro de adaptadores. GenericAdapter es la reserva y va el último. */
object OemAdapters {
    fun all(context: Context? = null): List<OemAdapter> =
        listOf(ColorOsAdapter(context), VivoAdapter(context), HyperOsAdapter(context), MagicOsAdapter(context), GenericAdapter(context))
    fun select(device: DeviceInfo, context: Context? = null): OemAdapter =
        all(context).first { it.matches(device) }
    fun forThisDevice(context: Context): OemAdapter =
        select(DeviceInfo(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty()), context)
}
