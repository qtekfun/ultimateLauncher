package com.qtekfun.ultimatelauncher.layoutsync

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.android.launcher3.R
import kotlin.concurrent.thread

/**
 * Exportar/importar la disposición a un archivo JSON con el selector de documentos del sistema (sin red, docs/06).
 * El archivo lista las apps instaladas: es información sensible; se puede cifrar con una frase de paso (AES-GCM, ver LayoutCrypto).
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

    /** Pide una frase de paso; [onDone] recibe null si se cancela y un array vacío si se deja en blanco (sin cifrar al exportar). */
    private fun askPassphrase(title: Int, hint: Int, onDone: (CharArray?) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setHint(hint)
        }
        AlertDialog.Builder(this).setTitle(title).setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ -> onDone(input.text.toString().toCharArray()) }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun doExport(uri: Uri) = askPassphrase(R.string.ul_sync_pass_export, R.string.ul_sync_pass_hint_export) { pass ->
        if (pass != null) exportTo(uri, pass)
    }

    private fun exportTo(uri: Uri, pass: CharArray) = thread {
        val msg = runCatching {
            val s = store.export()
            val json = LayoutJson.toJson(s)
            val out = if (pass.isEmpty()) json else LayoutCrypto.encrypt(json, pass)
            contentResolver.openOutputStream(uri, "wt")!!.use { it.write(out.toByteArray()) }
            getString(R.string.ul_sync_exported, s.pages.sumOf { it.items.size } + s.hotseat.size)
        }.getOrElse { getString(R.string.ul_sync_error, it.message ?: it.javaClass.simpleName) }
        runOnUiThread { status.text = msg }
    }

    private fun doImport(uri: Uri) = thread {
        val text = runCatching { contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }
        if (text.isSuccess && LayoutCrypto.isEncrypted(text.getOrThrow())) {
            runOnUiThread { askPassphrase(R.string.ul_sync_pass_import, R.string.ul_sync_pass_hint_import) { pass ->
                if (pass != null) thread { showPlan(runCatching { LayoutCrypto.decrypt(text.getOrThrow(), pass) }) } } }
        } else showPlan(text)
    }

    private fun showPlan(text: Result<String>) {
        val result = runCatching {
            val snap = LayoutJson.fromJson(text.getOrThrow())
            snap to ImportPlanner.plan(snap, store.deviceState())
        }
        runOnUiThread {
            result.onFailure { status.text = when (it) {
                is UnsupportedSchemaException -> getString(R.string.ul_sync_schema, it.found)
                is LayoutCrypto.WrongPasswordException -> getString(R.string.ul_sync_wrong_pass)
                else -> getString(R.string.ul_sync_error, it.message ?: "") } }
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
