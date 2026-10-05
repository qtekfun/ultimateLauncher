package com.qtekfun.ultimatelauncher.importer

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.ScrollView
import android.widget.TextView
import com.android.launcher3.R
import com.qtekfun.ultimatelauncher.layoutsync.AppOrder
import com.qtekfun.ultimatelauncher.layoutsync.AppsByOrder
import com.qtekfun.ultimatelauncher.layoutsync.ImportPlan
import com.qtekfun.ultimatelauncher.layoutsync.ImportPlanner
import com.qtekfun.ultimatelauncher.layoutsync.LayoutStore
import com.qtekfun.ultimatelauncher.layoutsync.LayoutSyncActivity
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Asistente «Importar de otro launcher» (docs/06). Tres vías, todas sin red:
 *  1. Proveedor de contenido de un launcher derivado de Launcher3 instalado (solo si no exige un permiso que no tenemos).
 *  2. Archivo de copia (base de datos .db o ZIP .novabackup/.lawnchairbackup) elegido con el selector de documentos.
 *  3. Export propio de UltimateLauncher (se remite a la pantalla de exportar/importar).
 * Los launchers cerrados de fabricante se explican y se ofrecen alternativas. Antes de aplicar se enseña la vista previa.
 */
class ForeignImportActivity : Activity() {
    private val pickFile = 21
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private val lang get() = Locale.getDefault().language
    private val store by lazy { LayoutStore(this) }
    private val catalog by lazy {
        ImportSourceCatalog.parse(assets.open("launcher-import-sources.json").bufferedReader().use { it.readText() })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        fun text(s: String, size: Float = 16f, bold: Boolean = false) = TextView(this).apply {
            this.text = s; textSize = size; if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, pad / 2, 0, pad / 4)
        }
        root.addView(text(getString(R.string.ul_imp_title), 24f, true))
        root.addView(text(getString(R.string.ul_imp_intro)))
        root.addView(text(getString(R.string.ul_imp_detected), 18f, true))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        root.addView(text(getString(R.string.ul_imp_btn_file), 18f, true))
        root.addView(text(getString(R.string.ul_imp_file_help), 14f))
        root.addView(Button(this).apply { text = getString(R.string.ul_imp_btn_file); setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), pickFile) } })
        root.addView(Button(this).apply { text = getString(R.string.ul_imp_btn_own); setOnClickListener {
            startActivity(Intent(this@ForeignImportActivity, LayoutSyncActivity::class.java)) } })
        root.addView(text(getString(R.string.ul_imp_order_title), 18f, true))
        root.addView(text(getString(R.string.ul_imp_order_help), 14f))
        root.addView(Button(this).apply { text = getString(R.string.ul_imp_order_btn); setOnClickListener { askOrder() } })
        status = text("")
        root.addView(status)
        root.addView(text(getString(R.string.ul_imp_alt_title), 18f, true))
        root.addView(text(getString(R.string.ul_imp_alt_body), 14f))
        setContentView(ScrollView(this).apply { addView(root) })
        fillDetected(pad)
    }

    private fun fillDetected(pad: Int) {
        val found = ImportSourceCatalog.detect(catalog, SourceProbe.homePackages(this), packageName)
        if (found.isEmpty()) list.addView(TextView(this).apply { text = getString(R.string.ul_imp_none_detected) })
        for (d in found) {
            val name = SourceProbe.label(this, d.pkg)
            list.addView(TextView(this).apply { text = name; textSize = 16f; setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, pad / 2, 0, 0) })
            if (d.source.closed) { list.addView(TextView(this).apply { text = getString(R.string.ul_imp_closed, name); textSize = 14f }); continue }
            for (r in d.source.routes) {
                val unverified = if (r.verified) "" else " " + getString(R.string.ul_imp_route_unverified)
                if (r.isBackup) {
                    list.addView(TextView(this).apply { text = getString(R.string.ul_imp_backup_for, r.extensions.joinToString()) + " " + r.note(lang) + unverified; textSize = 14f })
                } else if (r.isProvider) {
                    val authority = r.authorityFor(d.pkg) ?: continue
                    val st = SourceProbe.providerStatus(this, authority)
                    val msg = when (st) {
                        ProviderStatus.Available -> getString(R.string.ul_imp_route_ok)
                        is ProviderStatus.NeedsPermission -> getString(R.string.ul_imp_route_perm, st.permission)
                        ProviderStatus.Absent -> getString(R.string.ul_imp_route_absent)
                    }
                    list.addView(TextView(this).apply { text = msg + unverified; textSize = 14f })
                    if (st == ProviderStatus.Available) list.addView(Button(this).apply {
                        text = getString(R.string.ul_imp_btn_read); setOnClickListener { readProvider(authority) } })
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (requestCode == pickFile && resultCode == RESULT_OK && uri != null) readBackup(uri)
    }

    /** «Colocar mis apps por orden»: elige orden y apps por página, y pasa la disposición generada por la vista previa normal. */
    private fun askOrder() {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val grid = com.qtekfun.ultimatelauncher.layoutsync.Grid(
            com.android.launcher3.LauncherAppState.getIDP(this).numColumns, com.android.launcher3.LauncherAppState.getIDP(this).numRows)
        val cap = AppsByOrder.capacity(grid)
        val orders = RadioGroup(this).apply {
            addView(RadioButton(this@ForeignImportActivity).apply { id = 1; text = getString(R.string.ul_imp_order_alpha); isChecked = true })
            addView(RadioButton(this@ForeignImportActivity).apply { id = 2; text = getString(R.string.ul_imp_order_install) })
        }
        val perPageLabel = TextView(this).apply { setPadding(0, pad, 0, 0) }
        val bar = SeekBar(this).apply { max = cap - 1; progress = cap - 1 }
        fun refresh() { perPageLabel.text = getString(R.string.ul_imp_order_per_page, bar.progress + 1, grid.columns, grid.rows) }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) = refresh()
            override fun onStartTrackingTouch(s: SeekBar?) = Unit
            override fun onStopTrackingTouch(s: SeekBar?) = Unit
        })
        refresh()
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, 0)
            addView(TextView(this@ForeignImportActivity).apply { text = getString(R.string.ul_imp_order_not_imported) })
            addView(orders); addView(perPageLabel); addView(bar) }
        AlertDialog.Builder(this).setTitle(R.string.ul_imp_order_btn).setView(ScrollView(this).apply { addView(body) })
            .setPositiveButton(R.string.ul_imp_order_next) { _, _ ->
                val order = if (orders.checkedRadioButtonId == 2) AppOrder.INSTALL_DATE else AppOrder.ALPHABETICAL
                val perPage = bar.progress + 1
                setStatus(getString(R.string.ul_imp_reading))
                thread { present { orderSnapshot(order, perPage, grid) to ParseStats(0, 0, 0, false, grid) } }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun orderSnapshot(order: AppOrder, perPage: Int, grid: com.qtekfun.ultimatelauncher.layoutsync.Grid): com.qtekfun.ultimatelauncher.layoutsync.Snapshot {
        val dm = resources.displayMetrics
        val device = com.qtekfun.ultimatelauncher.layoutsync.Device("", "", "", 0, dm.densityDpi, 0, 0)
        return AppsByOrder.snapshot(store.installedApps(), order, perPage, grid, store.currentHotseat(), device)
    }

    private fun setStatus(s: String) = runOnUiThread { status.text = s }

    private fun readProvider(authority: String) {
        setStatus(getString(R.string.ul_imp_reading))
        thread { present { ForeignLayoutParser.parse(ProviderTableReader(contentResolver, authority), createdAt = "") } }
    }

    private fun readBackup(uri: Uri) {
        setStatus(getString(R.string.ul_imp_reading))
        thread {
            val dir = File(cacheDir, "import-tmp")
            try {
                dir.deleteRecursively()
                val kind = contentResolver.openInputStream(uri)!!.use { BackupExtractor.open(it).first }
                if (kind == BackupKind.TEXT) { setStatus(getString(R.string.ul_imp_own_file)); return@thread }
                val cands = contentResolver.openInputStream(uri)!!.use { BackupExtractor.extractDbs(it, dir) }
                var last: Throwable? = null
                for (c in cands) {
                    try {
                        val r = SqliteFileTableReader(c.file).use { ForeignLayoutParser.parse(it) }
                        present { r }; return@thread
                    } catch (e: ForeignLayoutException) { last = e } catch (e: android.database.sqlite.SQLiteException) { last = e }
                }
                setStatus(getString(R.string.ul_imp_err_notlayout))
                last?.let { /* sin registrar: el mensaje podría incluir nombres del archivo */ }
            } catch (e: BackupTooLargeException) { setStatus(getString(R.string.ul_imp_err_large))
            } catch (e: NoLayoutInBackupException) { setStatus(getString(R.string.ul_imp_err_notlayout))
            } catch (e: Exception) { setStatus(getString(R.string.ul_imp_err_generic, e.javaClass.simpleName))
            } finally { dir.deleteRecursively() }
        }
    }

    /** Analiza, planifica contra lo instalado aquí y enseña la vista previa. Llamar fuera del hilo principal. */
    private fun present(parse: () -> Pair<com.qtekfun.ultimatelauncher.layoutsync.Snapshot, ParseStats>) {
        val res = runCatching {
            val (snap, stats) = parse()
            val plan = ImportPlanner.plan(snap, store.deviceState())
            plan to stats
        }
        res.onFailure { e -> setStatus(when (e) {
            is SecurityException -> getString(R.string.ul_imp_err_denied)
            is ForeignLayoutException -> getString(R.string.ul_imp_err_notlayout)
            else -> getString(R.string.ul_imp_err_generic, e.javaClass.simpleName) }) }
        res.onSuccess { (plan, stats) -> runOnUiThread { status.text = ""; showPreview(plan, stats) } }
    }

    private fun showPreview(plan: ImportPlan, stats: ParseStats) {
        val c = ImportPreview.counts(plan, stats)
        val msg = buildString {
            appendLine(getString(R.string.ul_imp_preview_msg, c.apps, c.folders, c.folderApps, c.widgets, c.dock, c.pages, c.omitted))
            if (stats.truncated) appendLine(getString(R.string.ul_imp_preview_trunc))
            if (plan.omitted.isNotEmpty()) {
                appendLine(getString(R.string.ul_imp_preview_omitted))
                plan.omitted.take(12).forEach { appendLine("  – ${it.what}: ${it.reason}") }
                if (plan.omitted.size > 12) appendLine("  … +${plan.omitted.size - 12}")
            }
            plan.changes.forEach { appendLine("• $it") }
            append(getString(R.string.ul_imp_preview_replace))
        }
        val apply = c.apps + c.folders + c.widgets + c.dock > 0
        AlertDialog.Builder(this).setTitle(R.string.ul_imp_preview_title).setMessage(msg)
            .apply { if (apply) setPositiveButton(R.string.ul_imp_apply) { _, _ -> thread {
                val m = runCatching { store.apply(plan).name }.fold({ getString(R.string.ul_imp_done, it) }, { getString(R.string.ul_imp_err_generic, it.javaClass.simpleName) })
                setStatus(m) } } }
            .setNegativeButton(android.R.string.cancel, null).show()
    }
}
