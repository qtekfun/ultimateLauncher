// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.layoutsync

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import com.android.launcher3.R
import kotlin.concurrent.thread

/**
 * «Guardar copia» y «Restaurar copia» de Ajustes de inicio (docs/06). Actividad sin interfaz propia: abre directamente el
 * selector de documentos del sistema (SAF, sin permisos) y enseña el resultado en un diálogo. La copia es un JSON del esquema
 * v2 (disposición + ajustes + widgets) SIN cifrar: la acción principal no pide frase. Si se restaura un archivo cifrado
 * (hecho desde «Exportar/importar disposición») se pide la frase. Sin red.
 */
class BackupActivity : Activity() {
    private val reqSave = 31
    private val reqRestore = 32
    private val store by lazy { LayoutStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_SAVE -> startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json").putExtra(Intent.EXTRA_TITLE, BackupPrefs.suggestedName()), reqSave)
            ACTION_RESTORE -> startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*"), reqRestore)
            else -> finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) { finish(); return }
        if (requestCode == reqSave) save(uri) else if (requestCode == reqRestore) restore(uri) else finish()
    }

    /** Muestra un mensaje final y cierra la actividad al descartarlo. */
    private fun done(title: Int, msg: String) = runOnUiThread {
        AlertDialog.Builder(this).setTitle(title).setMessage(msg).setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }.show()
    }

    private fun save(uri: Uri) = thread {
        runCatching {
            val s = store.export()
            contentResolver.openOutputStream(uri, "wt")!!.use { it.write(LayoutJson.toJson(s).toByteArray()) }
            getString(R.string.ul_backup_saved, s.pages.sumOf { it.items.size } + s.hotseat.size, s.prefs.size)
        }.fold({ done(R.string.ul_backup_save_title, it) }, { done(R.string.ul_backup_save_title, getString(R.string.ul_backup_error, it.javaClass.simpleName)) })
    }

    private fun restore(uri: Uri) = thread {
        val text = runCatching {
            val out = java.io.ByteArrayOutputStream()
            contentResolver.openInputStream(uri)!!.use { ins ->
                val buf = ByteArray(8192)
                while (true) { val n = ins.read(buf); if (n < 0) break; out.write(buf, 0, n); require(out.size() <= MAX_BYTES) { "too large" } }
            }
            out.toString("UTF-8")
        }
        text.onFailure { done(R.string.ul_backup_restore_title, getString(R.string.ul_backup_not_backup)); return@thread }
        val t = text.getOrThrow()
        if (LayoutCrypto.isEncrypted(t)) {
            runOnUiThread { askPassphrase { pass ->
                if (pass == null) finish() else thread { plan(runCatching { LayoutCrypto.decrypt(t, pass) }) } } }
        } else plan(Result.success(t))
    }

    private fun askPassphrase(onDone: (CharArray?) -> Unit) {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setHint(R.string.ul_sync_pass_hint_import) }
        AlertDialog.Builder(this).setTitle(R.string.ul_sync_pass_import).setView(input).setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> onDone(input.text.toString().toCharArray()) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onDone(null) }.show()
    }

    private fun plan(text: Result<String>) {
        val res = runCatching { ImportPlanner.plan(LayoutJson.fromJson(text.getOrThrow()), store.deviceState()) }
        res.onFailure { e -> done(R.string.ul_backup_restore_title, when (e) {
            is UnsupportedSchemaException -> getString(R.string.ul_sync_schema, e.found)
            is LayoutCrypto.WrongPasswordException -> getString(R.string.ul_sync_wrong_pass)
            else -> getString(R.string.ul_backup_not_backup) }) }
        res.onSuccess { plan -> runOnUiThread {
            AlertDialog.Builder(this).setTitle(R.string.ul_backup_restore_title).setCancelable(false)
                .setMessage(plan.summary() + "\n\n" + getString(R.string.ul_backup_widget_limit) + "\n\n" + getString(R.string.ul_backup_replace_note))
                .setPositiveButton(R.string.ul_backup_restore_btn) { _, _ -> apply(plan) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }.show() } }
    }

    private fun apply(plan: ImportPlan) = thread {
        runCatching { store.apply(plan).name }.fold(
            { done(R.string.ul_backup_restore_title, getString(R.string.ul_backup_restored, it)) },
            { done(R.string.ul_backup_restore_title, getString(R.string.ul_backup_error, it.javaClass.simpleName)) })
    }

    companion object {
        const val EXTRA_ACTION = "ul_backup_action"
        const val ACTION_SAVE = "save"
        const val ACTION_RESTORE = "restore"
        private const val MAX_BYTES = 8 * 1024 * 1024

        /** Lanzador común para las entradas de Ajustes de inicio (parche 0160). */
        @JvmStatic fun launch(context: Context, action: String) {
            context.startActivity(Intent(context, BackupActivity::class.java).putExtra(EXTRA_ACTION, action))
        }

        /** «Traer mi pantalla de inicio» (parche 0160). */
        @JvmStatic fun launchImport(context: Context) {
            context.startActivity(Intent(context, com.qtekfun.ultimatelauncher.importer.ForeignImportActivity::class.java))
        }
    }
}
