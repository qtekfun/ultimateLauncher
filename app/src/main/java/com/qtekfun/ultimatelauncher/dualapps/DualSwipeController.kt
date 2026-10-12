// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dualapps

import android.graphics.Rect
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.BubbleTextView
import com.android.launcher3.CellLayout
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.touch.SingleAxisSwipeDetector
import com.android.launcher3.util.TouchController

/**
 * «Deslizar hacia arriba sobre un icono del par abre la otra copia» (docs/10; parche 0220). Va ANTES de
 * `AllAppsSwipeController` en `Launcher.createTouchControllers`: solo intercepta si el toque empieza sobre un icono de
 * app con par en el escritorio o el dock; en cualquier otro sitio el deslizamiento hacia arriba sigue abriendo el
 * cajón. Usa el mismo `SingleAxisSwipeDetector` (y el mismo umbral) que el cajón; la pulsación larga y el arrastre del
 * icono no cambian porque empiezan sin deslizar.
 */
class DualSwipeController(private val launcher: Launcher) : TouchController, SingleAxisSwipeDetector.Listener {
    private val detector = SingleAxisSwipeDetector(launcher, this, SingleAxisSwipeDetector.VERTICAL)
    private val density = launcher.resources.displayMetrics.density
    private val tmpRect = Rect()

    private var noIntercept = true
    private var target: DualTarget? = null
    private var source: View? = null
    private var dx = 0f
    private var dy = 0f
    private var cancelled = false

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            target = null; source = null
            noIntercept = !canIntercept(ev)
            if (noIntercept) return false
            dx = 0f; dy = 0f; cancelled = false
            // Solo hacia arriba (POSITIVE en VERTICAL de Launcher3).
            detector.setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_POSITIVE, false)
        }
        if (noIntercept) return false
        if (ev.actionMasked == MotionEvent.ACTION_MOVE && launcher.workspace.isHandlingTouch) {
            noIntercept = true
            return false
        }
        onControllerTouchEvent(ev)
        return detector.isDraggingOrSettling
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        cancelled = ev.actionMasked == MotionEvent.ACTION_CANCEL
        return detector.onTouchEvent(ev)
    }

    private fun canIntercept(ev: MotionEvent): Boolean {
        if (ev.source == InputDevice.SOURCE_MOUSE) return false
        if (!launcher.isInState(LauncherState.NORMAL)) return false
        if (AbstractFloatingView.getTopOpenView(launcher) != null) return false
        if (launcher.isWorkspaceLoading || launcher.workspace.isSwitchingState) return false
        if (!DualApps.isSupported(launcher)) return false
        val config = DualApps.config(launcher)
        if (!config.active || !config.swipe) return false
        val icon = iconAt(ev.x, ev.y) ?: return false
        val other = DualApps.counterpart(launcher, icon.tag as? ItemInfo) ?: return false
        target = other
        source = icon
        return true
    }

    /** Icono de app bajo el punto (coordenadas del DragLayer) en la página actual o en el dock. */
    private fun iconAt(x: Float, y: Float): BubbleTextView? {
        val pages = listOfNotNull(
            launcher.workspace.getChildAt(launcher.workspace.currentPage) as? CellLayout,
            launcher.hotseat,
        )
        for (page in pages) {
            val container = page.shortcutsAndWidgets
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i) as? BubbleTextView ?: continue
                launcher.dragLayer.getDescendantRectRelativeToSelf(child, tmpRect)
                if (tmpRect.contains(x.toInt(), y.toInt())) return child
            }
        }
        return null
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
        val other = target ?: return
        if (cancelled) return
        val ok = DualAppsLogic.isSwipeUp(
            dx, dy, velocity,
            DualAppsLogic.MIN_DISTANCE_DP * density, DualAppsLogic.MIN_FLING_DP_PER_S * density,
        )
        if (!ok) return
        launcher.dragLayer.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        DualApps.launch(launcher, other, source)
    }

    override fun dump(): String = "DualSwipeController(target=${target != null})"
}
