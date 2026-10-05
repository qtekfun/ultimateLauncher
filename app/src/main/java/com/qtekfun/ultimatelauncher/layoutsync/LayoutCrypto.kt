// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.layoutsync

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/**
 * Cifrado opcional del archivo de exportación (docs/06 y docs/09). Solo APIs del JDK/Android, sin dependencias.
 *
 * Formato del sobre (versionado, JSON de texto para que siga siendo un archivo `.json`):
 * `{"ulEncrypted":1,"cipher":"AES-256-GCM","kdf":"PBKDF2WithHmacSHA256","iterations":N,"salt":"b64","iv":"b64","data":"b64"}`
 * La clave de 256 bits sale de la frase de paso con PBKDF2-HMAC-SHA256 y sal aleatoria de 16 bytes; el IV de GCM
 * (12 bytes) es aleatorio por archivo. Los campos de cabecera van como datos autenticados adicionales (AAD), de
 * modo que alterar el número de iteraciones, la sal o el IV hace fallar la verificación.
 * Argon2 no está en la plataforma; PBKDF2 con muchas iteraciones es lo disponible sin librerías.
 */
object LayoutCrypto {
    const val ENVELOPE_VERSION = 1
    const val DEFAULT_ITERATIONS = 600_000
    /** El importador rechaza cabeceras con el coste rebajado (un archivo manipulado no puede debilitar la clave). */
    const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 5_000_000
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val CIPHER = "AES-256-GCM"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    class WrongPasswordException : Exception("Frase de paso incorrecta o archivo alterado")
    class UnsupportedEnvelopeException(msg: String) : Exception(msg)

    /** ¿El texto es un sobre cifrado (y no un layout en claro)? */
    fun isEncrypted(text: String): Boolean = runCatching { JSONObject(text).has("ulEncrypted") }.getOrDefault(false)

    /** Cifra [plain] con la frase de paso. [iterations] por debajo de [MIN_ITERATIONS] no se podría volver a importar. */
    fun encrypt(plain: String, passphrase: CharArray, iterations: Int = DEFAULT_ITERATIONS, random: SecureRandom = SecureRandom()): String {
        require(passphrase.isNotEmpty()) { "La frase de paso no puede estar vacía" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Iteraciones fuera de rango" }
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(iterations, salt, iv))
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val b64 = Base64.getEncoder()
        return JSONObject().put("ulEncrypted", ENVELOPE_VERSION).put("cipher", CIPHER).put("kdf", KDF).put("iterations", iterations)
            .put("salt", b64.encodeToString(salt)).put("iv", b64.encodeToString(iv)).put("data", b64.encodeToString(data)).toString(2)
    }

    /** Descifra un sobre. Lanza [WrongPasswordException] si la frase no es la correcta o el archivo se alteró. */
    fun decrypt(envelope: String, passphrase: CharArray): String {
        val o = try { JSONObject(envelope) } catch (e: Exception) { throw UnsupportedEnvelopeException("No es un sobre cifrado") }
        val version = o.optInt("ulEncrypted", 0)
        if (version == 0) throw UnsupportedEnvelopeException("No es un sobre cifrado")
        if (version > ENVELOPE_VERSION) throw UnsupportedEnvelopeException("Sobre cifrado v$version, más nuevo de lo soportado")
        if (o.optString("cipher") != CIPHER || o.optString("kdf") != KDF) throw UnsupportedEnvelopeException("Algoritmo no soportado")
        val iterations = o.optInt("iterations", 0)
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw UnsupportedEnvelopeException("Número de iteraciones fuera de rango")
        val b64 = Base64.getDecoder()
        val salt: ByteArray
        val iv: ByteArray
        val data: ByteArray
        try {
            salt = b64.decode(o.getString("salt")); iv = b64.decode(o.getString("iv")); data = b64.decode(o.getString("data"))
        } catch (e: Exception) {
            throw UnsupportedEnvelopeException("Sobre cifrado dañado")
        }
        if (salt.size != SALT_BYTES || iv.size != IV_BYTES) throw UnsupportedEnvelopeException("Sobre cifrado dañado")
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad(iterations, salt, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        } catch (e: GeneralSecurityException) {
            throw WrongPasswordException()
        }
    }

    private fun key(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        try {
            return SecretKeySpec(SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun aad(iterations: Int, salt: ByteArray, iv: ByteArray): ByteArray {
        val b64 = Base64.getEncoder()
        return "ul-layout|$ENVELOPE_VERSION|$CIPHER|$KDF|$iterations|${b64.encodeToString(salt)}|${b64.encodeToString(iv)}".toByteArray(Charsets.UTF_8)
    }
}
