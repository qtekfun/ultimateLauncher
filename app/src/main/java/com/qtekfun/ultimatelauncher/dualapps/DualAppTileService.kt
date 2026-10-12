// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dualapps

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.android.launcher3.R

/**
 * Mosaico de Ajustes rápidos «Abrir cuenta 2» (docs/10): abre la otra copia de la última app emparejada abierta. Solo
 * existe en API 35+ (`@bool/ul_dual_tile_enabled`). No muestra el nombre de la app en el mosaico: los Ajustes rápidos
 * se ven con la pantalla bloqueada. Desde un mosaico solo se puede abrir una actividad con `startActivityAndCollapse`,
 * así que abre [DualLaunchActivity] (propia, invisible y no exportada), que lanza la otra copia en primer plano.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class DualAppTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.state = if (DualLaunchActivity.resolve(this) != null) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
        tile.updateTile()
    }

    override fun onClick() {
        if (DualLaunchActivity.resolve(this) == null) {
            Toast.makeText(this, R.string.ul_dual_tile_none, Toast.LENGTH_SHORT).show()
            return
        }
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, DualLaunchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (isLocked) unlockAndRun { startActivityAndCollapse(pi) } else startActivityAndCollapse(pi)
    }
}

/** Trampolín del mosaico: abre la otra copia de la última app emparejada y se cierra sin mostrarse. */
class DualLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = resolve(this)
        if (target == null) {
            Toast.makeText(this, R.string.ul_dual_tile_none, Toast.LENGTH_SHORT).show()
        } else {
            DualApps.launch(this, target, null)
        }
        finish()
    }

    companion object {
        /** Otra copia de la última app emparejada abierta, o null. */
        fun resolve(context: android.content.Context): DualTarget? {
            val (pkg, kind) = DualApps.last(context) ?: return null
            return DualApps.counterpart(context, pkg, null, kind)
        }
    }
}
