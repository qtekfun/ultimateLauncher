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
