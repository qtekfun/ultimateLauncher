package com.qtekfun.ultimatelauncher.layoutsync

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.android.launcher3.R
import kotlin.concurrent.thread

/**
 * Exportar/importar la disposición a un archivo JSON con el selector de documentos del sistema (sin red, docs/06).
 * El archivo lista las apps instaladas: es información sensible; esta versión NO lo cifra.
 */
class LayoutSyncActivity : Activity() {
    private val export = 1
    private val import = 2
    private lateinit var status: TextView
    private val store by lazy { LayoutStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        root.addView(TextView(this).apply { text = getString(R.string.ul_sync_title); textSize = 24f })
        root.addView(TextView(this).apply { text = getString(R.string.ul_sync_warning); setPadding(0, pad, 0, pad) })
        root.addView(Button(this).apply { text = getString(R.string.ul_sync_export); setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "ultimatelauncher-layout.json"), export) } })
        root.addView(Button(this).apply { text = getString(R.string.ul_sync_import); setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), import) } })
        status = TextView(this).apply { setPadding(0, pad, 0, 0) }
        root.addView(status)
        setContentView(root)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) return
        if (requestCode == export) doExport(uri) else if (requestCode == import) doImport(uri)
    }

    private fun doExport(uri: Uri) = thread {
        val msg = runCatching {
            val s = store.export()
            contentResolver.openOutputStream(uri, "wt")!!.use { it.write(LayoutJson.toJson(s).toByteArray()) }
            getString(R.string.ul_sync_exported, s.pages.sumOf { it.items.size } + s.hotseat.size)
        }.getOrElse { getString(R.string.ul_sync_error, it.message ?: it.javaClass.simpleName) }
        runOnUiThread { status.text = msg }
    }

    private fun doImport(uri: Uri) = thread {
        val result = runCatching {
            val snap = LayoutJson.fromJson(contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            snap to ImportPlanner.plan(snap, store.deviceState())
        }
        runOnUiThread {
            result.onFailure { status.text = if (it is UnsupportedSchemaException) getString(R.string.ul_sync_schema, it.found) else getString(R.string.ul_sync_error, it.message ?: "") }
            result.onSuccess { (_, plan) ->
                AlertDialog.Builder(this).setTitle(R.string.ul_sync_summary).setMessage(plan.summary())
                    .setPositiveButton(R.string.ul_sync_apply) { _, _ -> thread {
                        val m = runCatching { store.apply(plan).name }.fold({ getString(R.string.ul_sync_done, it) }, { getString(R.string.ul_sync_error, it.message ?: "") })
                        runOnUiThread { status.text = m; Toast.makeText(this, m, Toast.LENGTH_LONG).show() } } }
                    .setNegativeButton(android.R.string.cancel, null).show()
            }
        }
    }
}
