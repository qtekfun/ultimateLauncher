// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.layoutsync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutCryptoTest {
    private val it0 = LayoutCrypto.MIN_ITERATIONS // rápido en pruebas; el valor por defecto es mayor
    private val pass = "frase de paso ñandú".toCharArray()
    private val snap = Snapshot("2026-10-05T00:00:00Z", Device("phone", "OPPO", "CPH2841", 36, 560, 411, 905),
        Settings(Grid(5, 7)), emptyList(), listOf(Page(0, listOf(Item.App("a", "a.Main", "personal", Cell(0, 0))))))

    @Test fun roundTripReturnsSameText() {
        val plain = LayoutJson.toJson(snap)
        val env = LayoutCrypto.encrypt(plain, pass, it0)
        assertTrue(LayoutCrypto.isEncrypted(env)); assertFalse(LayoutCrypto.isEncrypted(plain))
        assertFalse("el paquete no debe verse en claro", env.contains("\"a.Main\""))
        assertEquals(plain, LayoutCrypto.decrypt(env, pass))
        assertEquals(snap, LayoutJson.fromJson(LayoutCrypto.decrypt(env, pass)))
    }

    @Test(expected = LayoutCrypto.WrongPasswordException::class) fun wrongPasswordFails() {
        LayoutCrypto.decrypt(LayoutCrypto.encrypt("secreto", pass, it0), "otra".toCharArray())
    }

    @Test fun twoEncryptionsDiffer() { // sal e IV aleatorios
        assertNotEquals(LayoutCrypto.encrypt("x", pass, it0), LayoutCrypto.encrypt("x", pass, it0))
    }

    @Test(expected = LayoutCrypto.WrongPasswordException::class) fun tamperedCiphertextFails() {
        val o = JSONObject(LayoutCrypto.encrypt("secreto", pass, it0))
        val d = java.util.Base64.getDecoder().decode(o.getString("data")); d[0] = (d[0].toInt() xor 1).toByte()
        o.put("data", java.util.Base64.getEncoder().encodeToString(d))
        LayoutCrypto.decrypt(o.toString(), pass)
    }

    @Test(expected = LayoutCrypto.WrongPasswordException::class) fun tamperedHeaderFails() { // la cabecera va como AAD
        val o = JSONObject(LayoutCrypto.encrypt("secreto", pass, it0))
        o.put("iterations", it0 + 1)
        LayoutCrypto.decrypt(o.toString(), pass)
    }

    @Test(expected = LayoutCrypto.UnsupportedEnvelopeException::class) fun weakIterationsRejected() {
        val o = JSONObject(LayoutCrypto.encrypt("secreto", pass, it0)); o.put("iterations", 1)
        LayoutCrypto.decrypt(o.toString(), pass)
    }

    @Test(expected = LayoutCrypto.UnsupportedEnvelopeException::class) fun newerEnvelopeRejected() {
        val o = JSONObject(LayoutCrypto.encrypt("secreto", pass, it0)); o.put("ulEncrypted", 2)
        LayoutCrypto.decrypt(o.toString(), pass)
    }

    @Test(expected = LayoutCrypto.UnsupportedEnvelopeException::class) fun plainTextIsNotAnEnvelope() {
        LayoutCrypto.decrypt(LayoutJson.toJson(snap), pass)
    }

    @Test(expected = IllegalArgumentException::class) fun emptyPassphraseRejected() {
        LayoutCrypto.encrypt("x", CharArray(0), it0)
    }
}
