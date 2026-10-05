// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.hidden

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

/**
 * Autenticación del dispositivo para las apps ocultas, con el `BiometricPrompt` del framework (sin librerías):
 * huella o rostro (clase débil, la de BIOMETRIC_WEAK) o, como alternativa, PIN, patrón o contraseña de pantalla
 * (DEVICE_CREDENTIAL). Necesita el permiso normal USE_BIOMETRIC (docs/09). `minSdk` es 31, así que
 * `setAllowedAuthenticators` (API 30) está siempre disponible.
 */
object DeviceAuth {
    /** ¿Hay un bloqueo de pantalla (PIN, patrón, contraseña o biometría)? No necesita permisos. */
    @JvmStatic fun hasScreenLock(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    /** Muestra el cuadro del sistema. `onResult(true)` si el usuario se identifica; `false` si cancela o falla. */
    @JvmStatic fun authenticate(
        context: Context,
        title: CharSequence,
        subtitle: CharSequence?,
        onResult: (Boolean) -> Unit,
    ): CancellationSignal {
        val signal = CancellationSignal()
        try {
            val prompt = BiometricPrompt.Builder(context)
                .setTitle(title)
                .apply { if (subtitle != null) setSubtitle(subtitle) }
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build()
            prompt.authenticate(signal, context.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onResult(false)
                // onAuthenticationFailed (dedo no reconocido): el sistema deja reintentar, no se hace nada.
            })
        } catch (e: Exception) {
            onResult(false)
        }
        return signal
    }
}
