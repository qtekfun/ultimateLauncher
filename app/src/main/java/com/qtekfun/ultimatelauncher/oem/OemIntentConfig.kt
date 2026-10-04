package com.qtekfun.ultimatelauncher.oem

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

/**
 * Lee assets/oem-intents.json (rutas de ajustes por familia). Los componentes concretos cambian entre versiones de ROM,
 * por eso viven en datos (docs/05) y cada entrada declara si está verificada en un dispositivo real.
 */
object OemIntentConfig {
    fun load(context: Context, adapterId: String, kind: String): List<Intent> {
        val root = JSONObject(context.assets.open("oem-intents.json").bufferedReader().use { it.readText() })
        val arr = root.optJSONObject(adapterId)?.optJSONArray(kind) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val intent = Intent()
            o.optString("action").takeIf { it.isNotEmpty() }?.let { intent.action = it }
            o.optString("component").takeIf { it.contains("/") }?.let {
                val (pkg, cls) = it.split("/", limit = 2)
                intent.setClassName(pkg, if (cls.startsWith(".")) pkg + cls else cls)
            }
            o.optString("data").takeIf { it.isNotEmpty() }?.let {
                intent.data = Uri.parse(it.replace("{package}", context.packageName))
            }
            intent
        }
    }
}
