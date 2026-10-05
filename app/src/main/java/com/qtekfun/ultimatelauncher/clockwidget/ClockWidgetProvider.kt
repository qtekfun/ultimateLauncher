package com.qtekfun.ultimatelauncher.clockwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.util.SizeF
import android.widget.RemoteViews
import com.android.launcher3.R
import java.util.Locale

/**
 * Widget «Reloj digital»: tarjeta cuadrada de esquinas muy redondeadas con la hora en dígitos grandes y,
 * debajo (o a la derecha), día y fecha. Idea propia de un reloj digital minimalista; sin recursos de terceros.
 *
 * - La hora la dibuja un `TextClock` dentro del propio widget: el sistema lo mantiene al minuto, así que no
 *   hace falta `AlarmManager`, permisos ni `updatePeriodMillis` (0). Respeta 12/24 h del sistema.
 * - Tres diseños según el tamaño (`RemoteViews(Map<SizeF, RemoteViews>)`, API 31 = minSdk): ver [ClockWidgetLogic].
 * - Toque: `ACTION_SHOW_ALARMS` (app de reloj del sistema) con `PendingIntent` inmutable; si nadie lo resuelve, no se
 *   asigna ninguna acción (el toque no hace nada).
 */
abstract class ClockWidgetProvider(private val dark: Boolean) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) manager.updateAppWidget(id, buildViews(context, dark))
    }

    companion object {
        /** Variante clara: tarjeta blanca con números negros (la del original). */
        fun buildViews(context: Context, dark: Boolean): RemoteViews {
            val pending = clockPendingIntent(context)
            val sizes = linkedMapOf<SizeF, RemoteViews>()
            for ((layout, min) in ClockWidgetLogic.minSizes) {
                sizes[SizeF(min.first, min.second)] = build(context, layout, dark, pending)
            }
            return RemoteViews(sizes)
        }

        internal fun build(context: Context, layout: ClockLayout, dark: Boolean, pending: PendingIntent?): RemoteViews {
            val res = when (layout) {
                ClockLayout.COMPACT -> R.layout.ul_clock_compact
                ClockLayout.SQUARE -> R.layout.ul_clock_square
                ClockLayout.WIDE -> R.layout.ul_clock_wide
            }
            val rv = RemoteViews(context.packageName, res)
            val primary = if (dark) Color.WHITE else Color.BLACK
            val secondary = if (dark) 0xB3FFFFFF.toInt() else 0x99000000.toInt()
            rv.setInt(
                R.id.ul_clock_card, "setBackgroundResource",
                if (dark) R.drawable.ul_clock_card_dark else R.drawable.ul_clock_card_light,
            )
            rv.setTextColor(R.id.ul_clock_time, primary)
            when (layout) {
                ClockLayout.COMPACT -> {}
                ClockLayout.SQUARE -> {
                    setDate(context, rv, R.id.ul_clock_date, ClockWidgetLogic.SKELETON_DAY_DATE, secondary)
                }
                ClockLayout.WIDE -> {
                    setDate(context, rv, R.id.ul_clock_weekday, ClockWidgetLogic.SKELETON_WEEKDAY, primary)
                    setDate(context, rv, R.id.ul_clock_date, ClockWidgetLogic.SKELETON_DAY_MONTH, secondary)
                }
            }
            if (pending != null) rv.setOnClickPendingIntent(R.id.ul_clock_card, pending)
            return rv
        }

        /** Pone patrón localizado (según la configuración regional en el momento de la actualización) y color. */
        private fun setDate(context: Context, rv: RemoteViews, viewId: Int, skeleton: String, color: Int) {
            val locale: Locale = context.resources.configuration.locales[0]
            val pattern = ClockWidgetLogic.datePattern(skeleton) { DateFormat.getBestDateTimePattern(locale, it) }
            rv.setCharSequence(viewId, "setFormat12Hour", pattern)
            rv.setCharSequence(viewId, "setFormat24Hour", pattern)
            rv.setTextColor(viewId, color)
        }

        /** `PendingIntent` inmutable hacia la app de reloj, o `null` si ninguna actividad resuelve el intent. */
        private fun clockPendingIntent(context: Context): PendingIntent? {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resolves = try {
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
            } catch (_: Exception) {
                false
            }
            if (!resolves) return null
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}

/** Cuatro proveedores (selector de widgets): el tamaño inicial (2x2 o 4x2) lo fija el XML de cada uno. */
class ClockWidgetSquareLight : ClockWidgetProvider(false)
class ClockWidgetWideLight : ClockWidgetProvider(false)
class ClockWidgetSquareDark : ClockWidgetProvider(true)
class ClockWidgetWideDark : ClockWidgetProvider(true)
