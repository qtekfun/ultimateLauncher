package com.qtekfun.ultimatelauncher.importer

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Qué es el archivo que eligió el usuario, según sus primeros bytes (no por la extensión, que no es fiable). */
enum class BackupKind { SQLITE, ZIP, TEXT, UNKNOWN }

class BackupTooLargeException : IOException("El archivo supera el límite de tamaño")
class NoLayoutInBackupException : IOException("No se encontró ninguna base de datos de disposición en el archivo")

/** Una base de datos candidata extraída de la copia (copiada a un archivo propio; el nombre original nunca se usa como ruta). */
data class DbCandidate(val entryName: String, val file: File)

/**
 * Obtiene bases de datos SQLite de un archivo de copia de seguridad: una `.db` suelta o un ZIP (`.novabackup`,
 * `.lawnchairbackup`…). Lectura en flujo (compatible con el selector de documentos), con tope de entradas y de bytes
 * descomprimidos contra bombas ZIP. Pura JVM: se prueba sin Android.
 */
object BackupExtractor {
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)

    fun sniff(head: ByteArray, n: Int): BackupKind = when {
        n >= 16 && head.copyOf(16).contentEquals(SQLITE_MAGIC) -> BackupKind.SQLITE
        n >= 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() && head[2].toInt() == 3 && head[3].toInt() == 4 -> BackupKind.ZIP
        n >= 1 && (head[0] == '{'.code.toByte() || head[0] == '['.code.toByte() || head[0] == 'U'.code.toByte()) -> BackupKind.TEXT
        else -> BackupKind.UNKNOWN
    }

    /** Devuelve el tipo y un flujo que conserva los bytes leídos. */
    fun open(input: InputStream): Pair<BackupKind, BufferedInputStream> {
        val b = BufferedInputStream(input, 64 * 1024)
        b.mark(32)
        val head = ByteArray(16); var n = 0
        while (n < 16) { val r = b.read(head, n, 16 - n); if (r < 0) break; n += r }
        b.reset()
        return sniff(head, n) to b
    }

    /** Copia [input] a [dest] hasta [max] bytes; lanza [BackupTooLargeException] si lo supera (y borra lo escrito). */
    private fun copyLimited(input: InputStream, dest: File, max: Long) {
        var total = 0L
        try {
            dest.outputStream().use { out ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val r = input.read(buf); if (r < 0) break
                    total += r
                    if (total > max) throw BackupTooLargeException()
                    out.write(buf, 0, r)
                }
            }
        } catch (e: IOException) { dest.delete(); throw e }
    }

    private fun score(name: String): Int {
        val base = name.substringAfterLast('/').lowercase()
        return when {
            base == "launcher.db" -> 0
            base.startsWith("launcher") -> 1
            "favorites" in base || "home" in base -> 2
            base.endsWith(".db") -> 3
            else -> 4
        }
    }

    /**
     * Devuelve las bases de datos candidatas ordenadas por probabilidad (la primera suele ser `launcher.db`), copiadas en
     * [destDir]. El llamador prueba cada una hasta que `ForeignLayoutParser` acepte una y debe borrar [destDir] al terminar.
     */
    fun extractDbs(input: InputStream, destDir: File, limits: ImportLimits = ImportLimits()): List<DbCandidate> {
        destDir.mkdirs()
        val (kind, stream) = open(input)
        return when (kind) {
            BackupKind.SQLITE -> listOf(DbCandidate("launcher.db", File(destDir, "cand0.db").also { copyLimited(stream, it, limits.maxFileBytes) }))
            BackupKind.ZIP -> {
                val found = mutableListOf<DbCandidate>()
                var budget = limits.maxFileBytes * 2 // tope total descomprimido
                var entries = 0
                ZipInputStream(stream).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        if (++entries > limits.maxZipEntries) throw IOException("Demasiadas entradas en el ZIP")
                        if (e.isDirectory) continue
                        // Se mira la cabecera de cada entrada; solo se copia si es SQLite (y como mucho 4 candidatas).
                        val b = BufferedInputStream(zip, 8 * 1024); b.mark(32)
                        val head = ByteArray(16); var n = 0
                        while (n < 16) { val r = b.read(head, n, 16 - n); if (r < 0) break; n += r }
                        b.reset()
                        if (sniff(head, n) == BackupKind.SQLITE && found.size < 4) {
                            val f = File(destDir, "cand${found.size}.db")
                            copyLimited(b, f, minOf(limits.maxFileBytes, budget))
                            budget -= f.length()
                            found += DbCandidate(e.name, f)
                        } else {
                            // Entrada que no interesa: se descarta contando bytes por si es una bomba.
                            val buf = ByteArray(16 * 1024); var skipped = 0L
                            while (true) { val r = b.read(buf); if (r < 0) break; skipped += r; if (skipped > limits.maxFileBytes) throw BackupTooLargeException() }
                            budget -= skipped
                        }
                        if (budget < 0) throw BackupTooLargeException()
                    }
                }
                if (found.isEmpty()) throw NoLayoutInBackupException()
                found.sortedBy { score(it.entryName) }
            }
            else -> throw NoLayoutInBackupException()
        }
    }
}
