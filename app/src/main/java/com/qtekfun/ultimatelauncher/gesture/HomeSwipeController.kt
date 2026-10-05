// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.gesture

import android.appwidget.AppWidgetHostView
import android.graphics.Rect
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.Toast
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.CellLayout
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.touch.SingleAxisSwipeDetector
import com.android.launcher3.util.TouchController

/**
 * Controlador táctil del escritorio para «deslizar hacia abajo» (parche 0180). Se añade tras `AllAppsSwipeController`,
 * que solo detecta el deslizamiento hacia arriba en el estado NORMAL; este solo detecta hacia abajo. Usa el mismo
 * `SingleAxisSwipeDetector` que el cajón, así que respeta el umbral de arrastre (touch slop) de Launcher3 y exige que el
 * movimiento sea más vertical que horizontal: un gesto horizontal sigue siendo del paginado del escritorio.
 *
 * No actúa: con el interruptor apagado, fuera del estado NORMAL (modo edición, arrastre, cajón), con una vista flotante
 * abierta (menús, carpetas), con el paginado horizontal ya en marcha, con ratón o si el toque empieza sobre un widget
 * (puede desplazarse en vertical por su cuenta). Sobre iconos y huecos sí actúa: el icono solo usa pulsación larga y el
 * arrastre empieza tras ella, no con un deslizamiento rápido.
 */
class HomeSwipeController(private val launcher: Launcher) : TouchController, SingleAxisSwipeDetector.Listener {
    private val detector = SingleAxisSwipeDetector(launcher, this, SingleAxisSwipeDetector.VERTICAL)
    private val density = launcher.resources.displayMetrics.density
    private val tmpRect = Rect()

    private var noIntercept = true
    private var config = HomeSwipeConfig()
    private var downX = 0f
    private var dx = 0f
    private var dy = 0f
    private var cancelled = false

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            config = HomeSwipePrefs.read(launcher)
            noIntercept = !canIntercept(ev)
            if (noIntercept) return false
            dx = 0f; dy = 0f; cancelled = false
            // Solo hacia abajo (NEGATIVE en VERTICAL de Launcher3). El cajón se queda con «hacia arriba».
            detector.setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_NEGATIVE, false)
        }
        if (noIntercept) return false
        // Si el paginado horizontal ya ha tomado el gesto, no se discute.
        if (ev.actionMasked == MotionEvent.ACTION_MOVE && launcher.workspace.isHandlingTouch) {
            noIntercept = true
            return false
        }
        onControllerTouchEvent(ev)
        return detector.isDraggingOrSettling
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) downX = ev.x
        cancelled = ev.actionMasked == MotionEvent.ACTION_CANCEL
        return detector.onTouchEvent(ev)
    }

    private fun canIntercept(ev: MotionEvent): Boolean {
        if (!config.enabled) return false
        if (ev.source == InputDevice.SOURCE_MOUSE) return false
        if (!launcher.isInState(LauncherState.NORMAL)) return false
        if (AbstractFloatingView.getTopOpenView(launcher) != null) return false
        if (launcher.isWorkspaceLoading || launcher.workspace.isSwitchingState) return false
        return !isOverWidget(ev.x, ev.y)
    }

    /** ¿El punto (en coordenadas del DragLayer) cae sobre un widget de la página actual? */
    private fun isOverWidget(x: Float, y: Float): Boolean {
        val page = launcher.workspace.getChildAt(launcher.workspace.currentPage) as? CellLayout ?: return false
        val container = page.shortcutsAndWidgets
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child !is AppWidgetHostView) continue
            launcher.dragLayer.getDescendantRectRelativeToSelf(child, tmpRect)
            if (tmpRect.contains(x.toInt(), y.toInt())) return true
        }
        return false
    }

    override fun onDragStart(start: Boolean, startDisplacement: Float) {}

    override fun onDrag(displacement: Float): Boolean {
        dy = displacement
        return true
    }

    override fun onDrag(displacement: Float, orthogonalDisplacement: Float, ev: MotionEvent): Boolean {
        dx = orthogonalDisplacement
        return onDrag(displacement)
    }

    override fun onDragEnd(velocity: Float) {
        detector.finishedScrolling()
        if (cancelled) return
        val width = launcher.dragLayer.width.toFloat()
        val panel = HomeSwipeLogic.classify(
            config, downX, width, dx, dy, velocity,
            HomeSwipeLogic.MIN_DISTANCE_DP * density, HomeSwipeLogic.MIN_FLING_DP_PER_S * density,
        )
        if (panel == Panel.NONE) return
        launcher.dragLayer.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        if (!StatusBarPanels.expand(launcher, panel)) {
            Toast.makeText(launcher, R.string.ul_swipe_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun dump(): String = "HomeSwipeController(enabled=${config.enabled}, split=${config.splitHalves}, swap=${config.swapped})"
}
