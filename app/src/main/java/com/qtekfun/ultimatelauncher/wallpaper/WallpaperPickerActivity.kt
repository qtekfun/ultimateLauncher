package com.qtekfun.ultimatelauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.android.launcher3.R
import com.qtekfun.ultimatelauncher.ui.ContextMenuStyle
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

/**
 * «Fondos de UltimateLauncher»: pack propio de fondos (generados por código, ver `tools/gen-wallpapers.py`, empaquetados
 * en el APK; no usa red). Rejilla de miniaturas, vista previa a pantalla completa con desenfoque y oscurecimiento opcionales
 * y botones Inicio / Bloqueo / Ambos con `WallpaperManager` (permiso SET_WALLPAPER, ya declarado). El recorte es «cover»
 * centrado ([WallpaperLogic.coverCrop]) a la proporción actual de la pantalla, así que sirve en móvil y en tablet apaisada.
 */
class WallpaperPickerActivity : ComponentActivity() {
    private class Wp(val res: Int, val name: Int)

    private val catalog = listOf(
        Wp(R.drawable.wp_01_aurora, R.string.ul_wp_aurora), Wp(R.drawable.wp_02_atardecer, R.string.ul_wp_atardecer),
        Wp(R.drawable.wp_03_oceano, R.string.ul_wp_oceano), Wp(R.drawable.wp_04_seda, R.string.ul_wp_seda),
        Wp(R.drawable.wp_05_dunas, R.string.ul_wp_dunas), Wp(R.drawable.wp_06_montanas, R.string.ul_wp_montanas),
        Wp(R.drawable.wp_07_luna, R.string.ul_wp_luna), Wp(R.drawable.wp_08_bokeh, R.string.ul_wp_bokeh),
        Wp(R.drawable.wp_09_geometrico, R.string.ul_wp_geometrico), Wp(R.drawable.wp_10_facetas, R.string.ul_wp_facetas),
        Wp(R.drawable.wp_11_tinta, R.string.ul_wp_tinta), Wp(R.drawable.wp_12_olas, R.string.ul_wp_olas),
    )

    private val ui = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val thumbs = arrayOfNulls<Bitmap>(catalog.size)
    private lateinit var root: FrameLayout
    private lateinit var gridPage: LinearLayout
    private lateinit var grid: GridView
    private lateinit var previewPage: FrameLayout
    private lateinit var previewImage: ImageView
    private lateinit var panel: LinearLayout
    private lateinit var closeBtn: TextView
    private lateinit var blurValue: TextView
    private lateinit var dimValue: TextView
    private lateinit var blurBar: SeekBar
    private lateinit var dimBar: SeekBar

    private var current = -1
    private var source: Bitmap? = null
    private var previewBase: Bitmap? = null
    private var blurPct = 0
    private var dimPct = 0
    private var generation = 0
    private var sideInset = Rect()

    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildViews()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (current >= 0) closePreview() else { isEnabled = false; finish() }
            }
        })
        io.execute {
            // Miniaturas pequeñas (1/8 de la imagen) para no gastar memoria con la rejilla.
            for ((i, w) in catalog.withIndex()) {
                val o = BitmapFactory.Options().apply { inSampleSize = 8 }
                thumbs[i] = BitmapFactory.decodeResource(resources, w.res, o)
                ui.post { (grid.adapter as? BaseAdapter)?.notifyDataSetChanged() }
            }
        }
        savedInstanceState?.let {
            blurPct = it.getInt("blur"); dimPct = it.getInt("dim")
            val idx = it.getInt("current", -1)
            if (idx in catalog.indices) openPreview(idx)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current", current); outState.putInt("blur", blurPct); outState.putInt("dim", dimPct)
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdownNow()
    }

    // ------------------------------------------------------------------ interfaz
    private fun buildViews() {
        root = FrameLayout(this).apply { setBackgroundColor(0xFF101114.toInt()) }

        gridPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        gridPage.addView(TextView(this).apply {
            setText(R.string.ul_wp_title); textSize = 26f; setTextColor(Color.WHITE)
            setPadding(dp(20f), dp(20f), dp(20f), dp(2f))
        })
        gridPage.addView(TextView(this).apply {
            setText(R.string.ul_wp_subtitle); textSize = 14f; setTextColor(0xB3FFFFFF.toInt())
            setPadding(dp(20f), 0, dp(20f), dp(12f))
        })
        val dm = resources.displayMetrics
        val cols = max(2, min(5, (dm.widthPixels / dm.density / 160f).toInt()))
        grid = GridView(this).apply {
            numColumns = cols
            horizontalSpacing = dp(12f); verticalSpacing = dp(12f)
            setPadding(dp(16f), dp(4f), dp(16f), dp(24f)); clipToPadding = false
            selector = ColorDrawable(Color.TRANSPARENT)
            adapter = ThumbAdapter(cols)
            setOnItemClickListener { _, _, pos, _ -> openPreview(pos) }
        }
        gridPage.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(gridPage, FrameLayout.LayoutParams(-1, -1))

        previewPage = FrameLayout(this).apply { setBackgroundColor(Color.BLACK); visibility = View.GONE }
        previewImage = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        previewPage.addView(previewImage, FrameLayout.LayoutParams(-1, -1))
        closeBtn = pill(getString(R.string.ul_wp_back)) { closePreview() }
        previewPage.addView(closeBtn, FrameLayout.LayoutParams(-2, dp(44f), Gravity.TOP or Gravity.START).apply {
            setMargins(dp(16f), dp(16f), 0, 0)
        })
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextMenuStyle.cardDrawable(this@WallpaperPickerActivity)
            setPadding(dp(18f), dp(14f), dp(18f), dp(12f))
        }
        val (blurRow, bb, bv) = sliderRow(R.string.ul_wp_blur) { blurPct = it; refreshPreview() }
        val (dimRow, db, dv) = sliderRow(R.string.ul_wp_dim) { dimPct = it; refreshPreview() }
        blurBar = bb; blurValue = bv; dimBar = db; dimValue = dv
        panel.addView(blurRow); panel.addView(dimRow)
        panel.addView(TextView(this).apply {
            setText(R.string.ul_wp_apply_to); textSize = 13f; setTextColor(0xB3FFFFFF.toInt()); setPadding(0, dp(10f), 0, dp(6f))
        })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((label, flags) in listOf(
            R.string.ul_wp_home to WallpaperManager.FLAG_SYSTEM,
            R.string.ul_wp_lock to WallpaperManager.FLAG_LOCK,
            R.string.ul_wp_both to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
        )) {
            buttons.addView(pill(getString(label)) { apply(flags, label) }.apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(0, dp(46f), 1f).apply { marginStart = dp(3f); marginEnd = dp(3f) })
        }
        panel.addView(buttons)
        previewPage.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            setMargins(dp(12f), 0, dp(12f), dp(12f))
        })
        root.addView(previewPage, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        // Edge-to-edge (targetSdk 37): los márgenes del sistema se aplican a los contenidos, no al fondo.
        root.setOnApplyWindowInsetsListener { _, ins ->
            val b = ins.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            sideInset = Rect(b.left, b.top, b.right, b.bottom)
            gridPage.setPadding(b.left, b.top, b.right, b.bottom)
            (closeBtn.layoutParams as FrameLayout.LayoutParams).apply { setMargins(dp(16f) + b.left, dp(16f) + b.top, 0, 0) }
            (panel.layoutParams as FrameLayout.LayoutParams).apply { setMargins(dp(12f) + b.left, 0, dp(12f) + b.right, dp(12f) + b.bottom) }
            closeBtn.requestLayout(); panel.requestLayout()
            WindowInsets.CONSUMED
        }
    }

    /** Botón en forma de píldora con el aspecto de las filas de los menús (texto blanco, estado pulsado translúcido). */
    private fun pill(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setPadding(dp(18f), 0, dp(18f), 0)
        val bg = { c: Int -> GradientDrawable().apply { setColor(c); cornerRadius = dp(23f).toFloat() } }
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), bg(0x55FFFFFF))
            addState(intArrayOf(), bg(0x26FFFFFF))
        }
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun sliderRow(label: Int, onChange: (Int) -> Unit): Triple<View, SeekBar, TextView> {
        val value = TextView(this).apply { textSize = 13f; setTextColor(0xB3FFFFFF.toInt()); text = "0 %"; minWidth = dp(44f); gravity = Gravity.END }
        val bar = SeekBar(this).apply {
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    value.text = "$p %"
                    if (fromUser) onChange(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply { setText(label); textSize = 14f; setTextColor(Color.WHITE); minWidth = dp(96f) })
        row.addView(bar, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(value)
        return Triple(row, bar, value)
    }

    private inner class ThumbAdapter(private val cols: Int) : BaseAdapter() {
        override fun getCount() = catalog.size
        override fun getItem(position: Int) = catalog[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val cell = (convertView as? FrameLayout) ?: FrameLayout(this@WallpaperPickerActivity).apply {
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, o: Outline) = o.setRoundRect(0, 0, v.width, v.height, dp(16f).toFloat())
                }
                addView(ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }, FrameLayout.LayoutParams(-1, -1))
                addView(TextView(context).apply {
                    textSize = 13f; setTextColor(Color.WHITE); setPadding(dp(10f), dp(18f), dp(10f), dp(8f))
                    background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x00000000, 0x99000000.toInt()))
                }, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
            }
            // Altura = 1,45 x el ancho de la celda (proporción de móvil vertical); en tablet las columnas son más.
            val cellW = (grid.width - grid.paddingLeft - grid.paddingRight - grid.horizontalSpacing * (cols - 1)) / cols
            cell.layoutParams = AbsListViewLayout(if (cellW > 0) cellW else dp(140f), ((if (cellW > 0) cellW else dp(140f)) * 1.45f).toInt())
            (cell.getChildAt(0) as ImageView).setImageBitmap(thumbs[position])
            (cell.getChildAt(1) as TextView).setText(catalog[position].name)
            return cell
        }
    }

    @Suppress("FunctionName")
    private fun AbsListViewLayout(w: Int, h: Int) = android.widget.AbsListView.LayoutParams(w, h)

    // ------------------------------------------------------------------ vista previa
    private fun screenSize(): Pair<Int, Int> {
        val b = getSystemService(android.view.WindowManager::class.java).currentWindowMetrics.bounds
        return b.width() to b.height()
    }

    private fun openPreview(index: Int) {
        current = index
        previewPage.visibility = View.VISIBLE
        blurBar.progress = blurPct; dimBar.progress = dimPct
        previewImage.setImageDrawable(ColorDrawable(Color.BLACK))
        val gen = ++generation
        val res = catalog[index].res
        val (sw, sh) = screenSize()
        io.execute {
            val src = BitmapFactory.decodeResource(resources, res) ?: return@execute
            val crop = WallpaperLogic.coverCrop(src.width, src.height, sw, sh)
            // Base de la vista previa: recorte a la proporción de la pantalla con el lado largo en 1100 px como máximo.
            val (pw, ph) = WallpaperLogic.outputSize(sw, sh, 1100)
            val base = render(src, crop.toRect(), pw, ph, 0, 0)
            ui.post {
                if (gen != generation) { return@post }
                source?.recycle()
                source = src; previewBase = base
                refreshPreview()
            }
        }
    }

    private fun closePreview() {
        current = -1; generation++
        previewPage.visibility = View.GONE
        previewImage.setImageDrawable(null)
        source = null; previewBase = null
    }

    private fun refreshPreview() {
        val base = previewBase ?: return
        val gen = ++generation
        val blur = blurPct; val dim = dimPct
        io.execute {
            val bmp = if (blur == 0 && dim == 0) base
            else render(base, Rect(0, 0, base.width, base.height), base.width, base.height, blur, dim)
            ui.post { if (gen == generation && current >= 0) previewImage.setImageBitmap(bmp) }
        }
    }

    // ------------------------------------------------------------------ aplicar
    private fun apply(flags: Int, whereLabel: Int) {
        val src = source ?: return
        val blur = blurPct; val dim = dimPct
        val (sw, sh) = screenSize()
        val appCtx = applicationContext
        Toast.makeText(this, R.string.ul_wp_applying, Toast.LENGTH_SHORT).show()
        val where = getString(whereLabel)
        io.execute {
            val msg = runCatching {
                val wm = WallpaperManager.getInstance(appCtx)
                check(wm.isWallpaperSupported && wm.isSetWallpaperAllowed) { "denied" }
                val crop = WallpaperLogic.coverCrop(src.width, src.height, sw, sh)
                val (ow, oh) = WallpaperLogic.outputSize(sw, sh, 3000)
                val out = render(src, crop.toRect(), ow, oh, blur, dim)
                wm.setBitmap(out, null, true, flags)
                out.recycle()
                appCtx.getString(R.string.ul_wp_applied, where)
            }.getOrElse { appCtx.getString(R.string.ul_wp_error) }
            ui.post { Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show() }
        }
    }

    private fun WallpaperLogic.Crop.toRect() = Rect(left, top, right, bottom)

    companion object {
        /**
         * Dibuja el recorte `crop` de `src` a `outW`x`outH`, lo desenfoca (0..100 sobre una copia reducida, barato incluso en
         * 3000 px) y lo oscurece. Es un puro cálculo de mapas de bits: se puede llamar fuera del hilo principal.
         */
        @JvmStatic
        fun render(src: Bitmap, crop: Rect, outW: Int, outH: Int, blurPct: Int, dimPct: Int): Bitmap {
            val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            val c = Canvas(out)
            c.drawBitmap(src, crop, Rect(0, 0, outW, outH), paint)
            if (blurPct > 0) {
                val ds = WallpaperLogic.blurDownscale(outW)
                val sw = max(1, outW / ds); val sh = max(1, outH / ds)
                val small = Bitmap.createScaledBitmap(out, sw, sh, true)
                val px = IntArray(sw * sh)
                small.getPixels(px, 0, sw, 0, 0, sw, sh)
                WallpaperLogic.boxBlur(px, sw, sh, WallpaperLogic.blurRadius(blurPct, sw))
                small.setPixels(px, 0, sw, 0, 0, sw, sh)
                c.drawBitmap(small, null, Rect(0, 0, outW, outH), paint)
                small.recycle()
            }
            if (dimPct > 0) c.drawColor(Color.argb(WallpaperLogic.dimAlpha(dimPct), 0, 0, 0))
            return out
        }

        /** Intención explícita para abrir el selector desde el menú del escritorio o los ajustes. */
        @JvmStatic
        fun intent(context: Context) = android.content.Intent(context, WallpaperPickerActivity::class.java)
    }
}
