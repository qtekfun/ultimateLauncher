// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.debugpack

import android.app.Activity
import android.os.Bundle

/** Ancla del paquete de iconos de prueba (solo debug): los packs se anuncian con una actividad; esta se cierra al abrirse. */
class DebugPackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
