package com.qtekfun.ultimatelauncher.importer

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.Closeable
import java.io.File

private fun Cursor.valueAt(i: Int): Any? = when (getType(i)) {
    Cursor.FIELD_TYPE_INTEGER -> getLong(i)
    Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
    Cursor.FIELD_TYPE_STRING -> getString(i)
    else -> null // NULL y BLOB (iconos antiguos): no se leen
}

private fun Cursor.readRows(wanted: List<String>, max: Int): List<Map<String, Any?>> {
    val idx = wanted.mapNotNull { n -> getColumnIndex(n).takeIf { it >= 0 }?.let { n to it } }
    val out = ArrayList<Map<String, Any?>>()
    while (moveToNext() && out.size < max) out += idx.associate { (n, i) -> n to valueAt(i) }
    return out
}

/** Lee un archivo SQLite ajeno en SOLO LECTURA (una copia en nuestra caché, nunca el original del otro launcher). */
class SqliteFileTableReader(file: File) : TableReader, Closeable {
    private val db: SQLiteDatabase = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)

    override fun columns(table: String): Set<String> =
        // `table` siempre es una constante nuestra («favorites», «workspaceScreens»), nunca texto del usuario.
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            val n = c.getColumnIndex("name"); val s = mutableSetOf<String>()
            while (n >= 0 && c.moveToNext()) s += c.getString(n)
            s
        }

    override fun rows(table: String, wanted: List<String>, maxRows: Int): List<Map<String, Any?>> =
        db.query(table, wanted.toTypedArray(), null, null, null, null, null, maxRows.toString()).use { it.readRows(wanted, maxRows) }

    override fun close() = db.close()
}

/** Lee `content://<autoridad>/<tabla>` de otro launcher. Puede lanzar [SecurityException] si no tenemos su permiso de lectura. */
class ProviderTableReader(private val resolver: ContentResolver, private val authority: String) : TableReader {
    private fun uri(table: String) = Uri.parse("content://$authority/$table")

    override fun columns(table: String): Set<String> =
        resolver.query(uri(table), null, "0", null, null)?.use { it.columnNames.toSet() } ?: emptySet()

    override fun rows(table: String, wanted: List<String>, maxRows: Int): List<Map<String, Any?>> {
        // Con proyección explícita no se leen las columnas pesadas (iconos); si el proveedor la rechaza, se pide todo.
        val c = try { resolver.query(uri(table), wanted.toTypedArray(), null, null, null) }
        catch (e: IllegalArgumentException) { resolver.query(uri(table), null, null, null, null) }
        return c?.use { it.readRows(wanted, maxRows) } ?: emptyList()
    }
}

/** Estado de la vía «proveedor» de un launcher instalado. */
sealed class ProviderStatus {
    object Available : ProviderStatus()
    /** El proveedor existe pero exige un permiso que esta compilación no tiene (la mayoría de los casos en AOSP moderno). */
    data class NeedsPermission(val permission: String) : ProviderStatus()
    object Absent : ProviderStatus()
}

object SourceProbe {
    /** Paquetes con actividad HOME (launchers instalados). Requiere el `<queries>` de HOME del manifiesto. */
    fun homePackages(context: Context): List<String> =
        context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.distinct()

    fun label(context: Context, pkg: String): String =
        runCatching { context.packageManager.getApplicationInfo(pkg, 0).loadLabel(context.packageManager).toString() }.getOrDefault(pkg)

    fun providerStatus(context: Context, authority: String): ProviderStatus {
        val info = runCatching { context.packageManager.resolveContentProvider(authority, 0) }.getOrNull() ?: return ProviderStatus.Absent
        val perm = info.readPermission
        if (perm != null && context.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) return ProviderStatus.NeedsPermission(perm)
        return ProviderStatus.Available
    }
}
