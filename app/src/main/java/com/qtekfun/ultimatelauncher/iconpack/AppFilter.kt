package com.qtekfun.ultimatelauncher.iconpack

/**
 * Lector del `appfilter.xml` de los paquetes de iconos de launchers (formato ADW/Nova/Apex, sin código de terceros):
 *
 *  - `<item component="ComponentInfo{paquete/Clase}" drawable="nombre"/>`: icono del pack para esa actividad.
 *  - `<iconback img1="..." img2="..."/>`: fondos para las apps sin icono en el pack (se elige uno por paquete).
 *  - `<iconmask img1="..."/>` y `<iconupon img1="..."/>`: máscara que recorta el icono original y capa por encima.
 *  - `<scale factor="0.8"/>`: escala del icono original dentro del fondo.
 *
 * El lector es propio y tolerante (no usa un analizador XML): los packs reales traen comentarios, CDATA, comillas
 * simples, entidades, etiquetas desconocidas y a veces XML mal cerrado. Tiene límites duros (bytes, nº de ítems, longitud
 * de atributos) porque el archivo viene de una app de terceros. Las etiquetas pueden venir también de un
 * `XmlPullParser` (res/xml/appfilter compilado): ver [parseTags].
 */
object AppFilter {
    const val MAX_BYTES = 4 * 1024 * 1024
    const val MAX_ITEMS = 20_000
    const val MAX_ATTR_LEN = 400
    const val MAX_IMG_ATTRS = 12

    /** Etiqueta de inicio con sus atributos (nombre en minúsculas). */
    class Tag(val name: String, val attrs: Map<String, String>)

    /** Contenido útil de un appfilter. `icons` va de «paquete/clase completa» al nombre del drawable. */
    class Data(
        val icons: Map<String, String>,
        val backs: List<String>,
        val mask: String?,
        val upon: String?,
        val scale: Float,
        /** true si se alcanzó algún límite y se descartó el resto. */
        val truncated: Boolean,
    ) {
        val isEmpty get() = icons.isEmpty() && backs.isEmpty() && mask == null && upon == null

        /** Fondo para un paquete concreto (estable entre ejecuciones), o null si el pack no trae fondos. */
        fun backFor(packageName: String): String? =
            if (backs.isEmpty()) null else backs[Math.floorMod(packageName.hashCode(), backs.size)]
    }

    @JvmStatic
    fun parseText(text: String): Data {
        val clipped = text.length > MAX_BYTES
        val src = if (clipped) text.substring(0, MAX_BYTES) else text
        return build(tokenize(src).iterator(), clipped)
    }

    /** Variante para etiquetas ya leídas (por ejemplo desde un `XmlPullParser` de un recurso compilado). */
    @JvmStatic
    fun parseTags(tags: Iterator<Tag>): Data = build(tags, false)

    private fun build(tags: Iterator<Tag>, startTruncated: Boolean): Data {
        val icons = LinkedHashMap<String, String>()
        var backs: List<String> = emptyList()
        var mask: String? = null
        var upon: String? = null
        var scale = 1f
        var truncated = startTruncated
        while (tags.hasNext()) {
            val t = tags.next()
            when (t.name) {
                "item" -> {
                    if (icons.size >= MAX_ITEMS) { truncated = true; continue }
                    val comp = normalizeComponent(t.attrs["component"]) ?: continue
                    val drawable = cleanDrawableName(t.attrs["drawable"]) ?: continue
                    icons.putIfAbsent(comp, drawable) // la primera entrada gana
                }
                "iconback" -> if (backs.isEmpty()) backs = imgs(t)
                "iconmask" -> if (mask == null) mask = imgs(t).firstOrNull()
                "iconupon" -> if (upon == null) upon = imgs(t).firstOrNull()
                "scale" -> t.attrs["factor"]?.trim()?.toFloatOrNull()?.let { f ->
                    if (f.isFinite()) scale = f.coerceIn(0.3f, 1.5f)
                }
            }
        }
        return Data(icons, backs, mask, upon, scale, truncated)
    }

    /** `img1`, `img2`... (o `img`) por orden numérico; vacíos descartados. */
    private fun imgs(t: Tag): List<String> =
        t.attrs.entries.filter { it.key == "img" || it.key.startsWith("img") }
            .sortedBy { it.key.removePrefix("img").toIntOrNull() ?: 0 }
            .take(MAX_IMG_ATTRS)
            .mapNotNull { cleanDrawableName(it.value) }

    /**
     * `ComponentInfo{pkg/cls}` (o `pkg/cls` a secas) -> `pkg/clsCompleta`; una clase que empieza por «.» se completa con el
     * paquete. Devuelve null si no hay paquete y clase válidos.
     */
    @JvmStatic
    fun normalizeComponent(raw: String?): String? {
        var s = raw?.trim() ?: return null
        if (s.startsWith("ComponentInfo{", ignoreCase = true) && s.endsWith("}")) s = s.substring(14, s.length - 1).trim()
        val slash = s.indexOf('/')
        if (slash <= 0 || slash == s.length - 1) return null
        val pkg = s.substring(0, slash).trim()
        var cls = s.substring(slash + 1).trim()
        if (pkg.isEmpty() || cls.isEmpty() || pkg.contains(' ') || cls.contains(' ') || cls.contains('/')) return null
        if (cls.startsWith(".")) cls = pkg + cls
        return "$pkg/$cls"
    }

    /** `@drawable/nombre` -> `nombre`; sin extensión de imagen; null si queda vacío o demasiado largo. */
    @JvmStatic
    fun cleanDrawableName(raw: String?): String? {
        var s = raw?.trim() ?: return null
        if (s.startsWith("@")) s = s.substringAfter('/', s.removePrefix("@"))
        s = s.removeSuffix(".png").removeSuffix(".webp").removeSuffix(".jpg")
        return if (s.isEmpty() || s.length > 160) null else s
    }

    // ---------------------------------------------------------------- tokenizador de texto XML

    /** Etiquetas de inicio (y autocerradas) de un texto XML, en orden. Ignora comentarios, CDATA, PI, DOCTYPE y cierres. */
    @JvmStatic
    fun tokenize(text: String): Sequence<Tag> = sequence {
        var i = if (text.startsWith("﻿")) 1 else 0
        val n = text.length
        while (i < n) {
            val lt = text.indexOf('<', i)
            if (lt < 0) return@sequence
            i = lt + 1
            if (i >= n) return@sequence
            when {
                text.startsWith("!--", i) -> { val e = text.indexOf("-->", i + 3); if (e < 0) return@sequence; i = e + 3 }
                text.startsWith("![CDATA[", i) -> { val e = text.indexOf("]]>", i + 8); if (e < 0) return@sequence; i = e + 3 }
                text[i] == '?' || text[i] == '!' -> { val e = text.indexOf('>', i); if (e < 0) return@sequence; i = e + 1 }
                text[i] == '/' -> { val e = text.indexOf('>', i); if (e < 0) return@sequence; i = e + 1 }
                else -> {
                    val start = i
                    while (i < n && !text[i].isWhitespace() && text[i] != '>' && text[i] != '/') i++
                    val name = text.substring(start, i).lowercase()
                    val attrs = LinkedHashMap<String, String>()
                    // atributos hasta '>' (respetando comillas)
                    while (i < n) {
                        while (i < n && (text[i].isWhitespace() || text[i] == '/')) i++
                        if (i >= n || text[i] == '>') break
                        val ks = i
                        while (i < n && !text[i].isWhitespace() && text[i] != '=' && text[i] != '>' && text[i] != '/') i++
                        val key = text.substring(ks, i).lowercase()
                        while (i < n && text[i].isWhitespace()) i++
                        var value = ""
                        if (i < n && text[i] == '=') {
                            i++
                            while (i < n && text[i].isWhitespace()) i++
                            if (i < n && (text[i] == '"' || text[i] == '\'')) {
                                val q = text[i]
                                val ve = text.indexOf(q, i + 1)
                                if (ve < 0) { value = text.substring(i + 1).take(MAX_ATTR_LEN + 1); i = n }
                                else { value = text.substring(i + 1, ve); i = ve + 1 }
                            } else {
                                val vs = i
                                while (i < n && !text[i].isWhitespace() && text[i] != '>') i++
                                value = text.substring(vs, i)
                            }
                        }
                        if (key.isNotEmpty() && value.length <= MAX_ATTR_LEN && attrs.size < 32) attrs[key] = decodeEntities(value)
                    }
                    if (i < n) i++ // '>'
                    if (name.isNotEmpty()) yield(Tag(name, attrs))
                }
            }
        }
    }

    @JvmStatic
    fun decodeEntities(s: String): String {
        if (s.indexOf('&') < 0) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c != '&') { sb.append(c); i++; continue }
            val semi = s.indexOf(';', i)
            if (semi < 0 || semi - i > 10) { sb.append(c); i++; continue }
            val ent = s.substring(i + 1, semi)
            val rep: String? = when {
                ent == "amp" -> "&"; ent == "lt" -> "<"; ent == "gt" -> ">"; ent == "quot" -> "\""; ent == "apos" -> "'"
                ent.startsWith("#x") || ent.startsWith("#X") -> ent.substring(2).toIntOrNull(16)?.let { runCatching { String(Character.toChars(it)) }.getOrNull() }
                ent.startsWith("#") -> ent.substring(1).toIntOrNull()?.let { runCatching { String(Character.toChars(it)) }.getOrNull() }
                else -> null
            }
            if (rep == null) { sb.append(c); i++ } else { sb.append(rep); i = semi + 1 }
        }
        return sb.toString()
    }
}
