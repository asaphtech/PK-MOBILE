package com.pkmobile.keyboard.data.importer

import com.pkmobile.keyboard.data.db.ShortcutEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Universal Shortcut Importer & Exporter.
 * Mendukung penuh format JSON (Array of Objects & Map Key-Value),
 * format XML Perfect Keyboard / Macro Text Wizard (MTW) / Smart Keyboard,
 * serta Delimited Text (CSV, TSV, key=value).
 */
object ShortcutImporter {

    // Kumpulan nama atribut / key untuk Kata Kunci (Shortcut Trigger)
    private val KEY_NAMES = listOf(
        "shortcut", "key", "trigger", "name", "abbreviation", "abbr",
        "src", "from", "keyword", "k", "word", "code", "tscut"
    )

    // Kumpulan nama atribut / key untuk Hasil Teks (Expansion)
    private val VALUE_NAMES = listOf(
        "expansion", "replacement", "value", "text", "macrotext", "macro_text",
        "phrase", "dst", "to", "content", "v", "exp"
    )

    /**
     * Membersihkan string kata kunci trigger secara menyeluruh:
     * 1. Menghilangkan semua blok CDATA (<![CDATA[ ... ]]>)
     * 2. Menghilangkan semua tag XML/HTML menggunakan Regex <[^>]*> (termasuk <tscut>, </tscut>, dll.)
     * 3. Menghilangkan tag spesifik jika terdapat tag yang tidak tertutup sempurna
     * 4. Menghilangkan entitas XML (&amp;, &lt;, &gt;, dll.)
     * 5. Menghilangkan spasi ekstra, non-breaking space (\u00A0), dan BOM (\uFEFF)
     * 6. Menghasilkan kata kunci trigger murni (contoh: "/baca", "/balance1", "/otosc")
     */
    fun cleanTrigger(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""

        // 1. Bersihkan CDATA
        s = s.replace("<![CDATA[", "").replace("]]>", "")

        // 2. Bersihkan semua tag HTML/XML dari string menggunakan Regex <[^>]*>
        s = s.replace(Regex("<[^>]*>"), "")

        // 3. Bersihkan tag spesifik jika terdapat tag yang tidak tertutup sempurna
        s = s.replace(Regex("(?i)</?tscut>"), "")
            .replace(Regex("(?i)</?trigger>"), "")
            .replace(Regex("(?i)</?key>"), "")
            .replace(Regex("(?i)</?shortcut>"), "")
            .replace(Regex("(?i)</?name>"), "")

        // 4. Bersihkan entitas XML
        s = s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

        // 5. Bersihkan karakter spasi khusus, BOM, dan lakukan .trim()
        s = s.replace("\uFEFF", "")
            .replace("\uFFFE", "")
            .replace("\u00A0", " ")
            .trim()

        return s
    }

    /**
     * Entry point utama parser: Membaca teks mentah dan menguraikan seluruh pasangan shortcut.
     * Mengutamakan JSON jika diawali dengan '[' atau '{', dan XML jika diawali dengan '<'.
     * Seluruh trigger yang dihasilkan dijamin bersih tanpa tag HTML/XML ataupun CDATA.
     */
    fun parse(rawContent: String): List<Pair<String, String>> {
        val content = cleanBomAndWhitespace(rawContent)
        if (content.isEmpty()) return emptyList()

        val rawResults = mutableListOf<Pair<String, String>>()

        // 1. Prioritaskan JSON jika teks diawali dengan kurung siku '[' atau kurung kurawal '{'
        if (content.startsWith("[") || content.startsWith("{")) {
            val jsonResults = parseJson(content)
            if (jsonResults.isNotEmpty()) {
                rawResults.addAll(jsonResults)
            }
        }

        // 2. Prioritaskan XML jika teks diawali dengan '<'
        if (rawResults.isEmpty() && content.startsWith("<")) {
            val xmlResults = parseXml(content)
            if (xmlResults.isNotEmpty()) {
                rawResults.addAll(xmlResults)
            }
        }

        // 3. Fallback jika memuat tag XML di bagian dalam (misal terdapat komentar atau prolog)
        if (rawResults.isEmpty() && content.contains("<") && content.contains(">")) {
            val xmlResults = parseXml(content)
            if (xmlResults.isNotEmpty()) {
                rawResults.addAll(xmlResults)
            }
        }

        // 4. Fallback jika memuat format JSON di bagian dalam
        if (rawResults.isEmpty() && (content.contains("{") || content.contains("["))) {
            val jsonResults = parseJson(content)
            if (jsonResults.isNotEmpty()) {
                rawResults.addAll(jsonResults)
            }
        }

        // 5. Fallback ke Bulk Text (TRIGGER = TEKS, TRIGGER -> TEKS, TRIGGER \t TEKS)
        if (rawResults.isEmpty()) {
            val bulkResults = parseBulkText(content)
            if (bulkResults.isNotEmpty()) {
                rawResults.addAll(bulkResults)
            }
        }

        // 6. Fallback ke Delimited text (CSV, TSV, key=value)
        if (rawResults.isEmpty()) {
            val delimitedResults = parseDelimitedText(content)
            if (delimitedResults.isNotEmpty()) {
                rawResults.addAll(delimitedResults)
            }
        }

        // 7. Fallback pemindaian baris demi baris
        if (rawResults.isEmpty()) {
            rawResults.addAll(parseLineByLineFallback(content))
        }

        // Jaminan pembersihan total (Sanitasi akhir trigger & expansion)
        return rawResults.mapNotNull { (trigger, expansion) ->
            val cleanK = cleanTrigger(trigger)
            val cleanV = cleanTextFormatting(expansion)
            if (cleanK.isNotBlank() && cleanV.isNotBlank()) {
                Pair(cleanK, cleanV)
            } else null
        }.distinctBy { it.first.lowercase() }
    }

    /**
     * PARSER BULK TEKS (Notepad / Aplikasi Teks):
     * - Memproses teks baris demi baris (split by '\n').
     * - Untuk setiap baris, dukung format pemisah:
     *   a) TRIGGER = TEKS (contoh: /otosc = Pesan...)
     *   b) TRIGGER -> TEKS (contoh: /gantirek -> Pesan...)
     *   c) Format Tab: TRIGGER [TAB] TEKS
     * - Mengambil bagian kiri sebagai shortcut/trigger (di-trim) dan bagian kanan sebagai expansion/teks pengganti.
     * - Mengabaikan baris kosong atau baris komentar (# atau //).
     * - Menangani karakter baris baru \n dan \t secara tepat.
     */
    fun parseBulkText(rawText: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val cleanContent = cleanBomAndWhitespace(rawText)
        if (cleanContent.isEmpty()) return emptyList()

        val lines = cleanContent.split("\n")
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

            var trigger: String? = null
            var expansion: String? = null

            // Format a: TRIGGER -> TEKS
            if (line.contains("->")) {
                val parts = line.split("->", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    trigger = parts[0].trim()
                    expansion = cleanTextFormatting(cleanQuotes(parts[1].trim()))
                }
            }
            // Format b: TRIGGER = TEKS
            else if (line.contains("=") && !line.startsWith("=")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    trigger = parts[0].trim()
                    expansion = cleanTextFormatting(cleanQuotes(parts[1].trim()))
                }
            }
            // Format c: Format Tab: TRIGGER [TAB] TEKS
            else if (line.contains("\t")) {
                val parts = line.split("\t", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    trigger = parts[0].trim()
                    expansion = cleanTextFormatting(cleanQuotes(parts[1].trim()))
                }
            }

            if (!trigger.isNullOrBlank() && !expansion.isNullOrBlank()) {
                result.add(Pair(trigger, expansion))
            }
        }

        return result.distinctBy { it.first.lowercase() }
    }

    /**
     * 1. PARSER JSON UNIVERSAL:
     * - Mendukung format Array of Objects: [{"shortcut": "...", "expansion": "..."}, ...]
     * - Mendukung format Map Key-Value sederhana: {"/otosc": "Pesan...", "/gantirek": "Pesan..."}
     * - Menangani karakter baris baru \n dan tab \t secara tepat.
     */
    fun parseJson(jsonContent: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val cleanContent = cleanBomAndWhitespace(jsonContent)
        if (cleanContent.isEmpty()) return emptyList()

        try {
            if (cleanContent.startsWith("[")) {
                // FORMAT A: Array of Objects
                val array = JSONArray(cleanContent)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    extractPairFromJson(obj)?.let { result.add(it) }
                }
            } else {
                // FORMAT B: Root Object (Bisa membungkus array atau berupa Map Key-Value)
                val rootObj = JSONObject(cleanContent)

                // Cek pembungkus array umum: "shortcuts", "data", "items", "macros"
                val shortcutsArray = rootObj.optJSONArray("shortcuts")
                    ?: rootObj.optJSONArray("data")
                    ?: rootObj.optJSONArray("items")
                    ?: rootObj.optJSONArray("macros")

                if (shortcutsArray != null) {
                    for (i in 0 until shortcutsArray.length()) {
                        val obj = shortcutsArray.optJSONObject(i) ?: continue
                        extractPairFromJson(obj)?.let { result.add(it) }
                    }
                } else {
                    // FORMAT C: Map Key-Value sederhana (contoh: {"/otosc": "Halo bosku...", "/gantirek": "..."})
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        // Jika value langsung bertipe string
                        val v = rootObj.optString(k, "")
                        if (k.isNotBlank() && v.isNotBlank()) {
                            result.add(Pair(k.trim(), cleanTextFormatting(v)))
                        } else {
                            // Jika value berupa nested object {"trigger": "...", "text": "..."}
                            val nestedObj = rootObj.optJSONObject(k)
                            if (nestedObj != null) {
                                extractPairFromJson(nestedObj)?.let { result.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Regex fallback jika JSON memiliki koma di akhir (trailing comma) atau format santai
            result.addAll(parseJsonWithRegex(cleanContent))
        }

        return result.distinctBy { it.first.lowercase() }
    }

    private fun extractPairFromJson(obj: JSONObject): Pair<String, String>? {
        var shortcut: String? = null
        var expansion: String? = null

        for (kName in KEY_NAMES) {
            if (obj.has(kName)) {
                val v = obj.optString(kName, "")
                if (v.isNotBlank()) {
                    shortcut = v
                    break
                }
            }
        }

        for (vName in VALUE_NAMES) {
            if (obj.has(vName)) {
                val v = obj.optString(vName, "")
                if (v.isNotBlank()) {
                    expansion = v
                    break
                }
            }
        }

        return if (!shortcut.isNullOrBlank() && !expansion.isNullOrBlank()) {
            Pair(shortcut.trim(), cleanTextFormatting(expansion))
        } else null
    }

    private fun parseJsonWithRegex(content: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        // Pola 1: "shortcut": "...", "expansion": "..."
        val objPattern = Pattern.compile(
            "\\{\\s*\"(?:${KEY_NAMES.joinToString("|")})\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"(?:${VALUE_NAMES.joinToString("|")})\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
            Pattern.CASE_INSENSITIVE
        )
        val matcher1 = objPattern.matcher(content)
        while (matcher1.find()) {
            val k = matcher1.group(1) ?: ""
            val v = matcher1.group(2) ?: ""
            if (k.isNotBlank() && v.isNotBlank()) {
                list.add(Pair(k.trim(), cleanTextFormatting(v)))
            }
        }

        // Pola 2: Key-Value map: "key": "value"
        if (list.isEmpty()) {
            val mapPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"")
            val matcher2 = mapPattern.matcher(content)
            while (matcher2.find()) {
                val k = matcher2.group(1) ?: ""
                val v = matcher2.group(2) ?: ""
                if (k.isNotBlank() && v.isNotBlank() && !k.equals("shortcut", true) && !k.equals("expansion", true)) {
                    list.add(Pair(k.trim(), cleanTextFormatting(v)))
                }
            }
        }
        return list
    }

    /**
     * 2. PARSER XML UNIVERSAL:
     * - Mendukung format container nested (<macro>, <record>, <item>, <shortcut>, <entry>)
     * - Mendukung format tag dengan atribut dalam urutan apa pun
     * - Menghilangkan CDATA, mengonversi entitas XML, dan menangani tag enter (<ent__>)
     */
    private fun parseXml(xmlContent: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()

        // METODE 1: Container Blocks (Nested Tags)
        val containerPattern = Pattern.compile(
            "<(?:macro|record|item|shortcut|entry|macro_item|data|autotext|word)\\b[^>]*>([\\s\\S]*?)</(?:macro|record|item|shortcut|entry|macro_item|data|autotext|word)>",
            Pattern.CASE_INSENSITIVE
        )
        val containerMatcher = containerPattern.matcher(xmlContent)

        while (containerMatcher.find()) {
            val block = containerMatcher.group(1) ?: continue
            val pair = extractPairFromXmlBlock(block)
            if (pair != null) {
                results.add(pair)
            }
        }

        // METODE 2: Tag tunggal dengan atribut (Single-tag XML attributes, bebas urutan)
        if (results.isEmpty()) {
            val singleTagPattern = Pattern.compile("<(?:item|shortcut|entry|record|macro|word)\\b([^>]+)/?>", Pattern.CASE_INSENSITIVE)
            val tagMatcher = singleTagPattern.matcher(xmlContent)
            while (tagMatcher.find()) {
                val attributesString = tagMatcher.group(1) ?: continue
                val pair = extractPairFromAttributes(attributesString)
                if (pair != null) {
                    results.add(pair)
                }
            }
        }

        // METODE 3: Line-by-line regex fallback khusus dokumen XML
        if (results.isEmpty()) {
            results.addAll(parseLineByLineFallback(xmlContent))
        }

        return results.distinctBy { it.first.lowercase() }
    }

    private fun extractPairFromXmlBlock(block: String): Pair<String, String>? {
        var key: String? = null
        var value: String? = null

        // 0. Penanganan khusus Macro Text Wizard (MTW) / Perfect Keyboard: <tscut> & <macroText>
        val tscutPattern = Pattern.compile("<tscut\\b[^>]*>([\\s\\S]*?)</tscut>", Pattern.CASE_INSENSITIVE)
        val tscutMatcher = tscutPattern.matcher(block)
        if (tscutMatcher.find()) {
            key = cleanTrigger(tscutMatcher.group(1) ?: "")
        }

        val macroTextPattern = Pattern.compile("<macroText\\b[^>]*>([\\s\\S]*?)</macroText>", Pattern.CASE_INSENSITIVE)
        val macroTextMatcher = macroTextPattern.matcher(block)
        if (macroTextMatcher.find()) {
            value = cleanXmlEntities(macroTextMatcher.group(1) ?: "")
        }

        // 1. Cari child element untuk Kata Kunci (Shortcut) secara umum jika belum dapat
        if (key.isNullOrBlank()) {
            val keyTagPattern = Pattern.compile(
                "<(?:${KEY_NAMES.joinToString("|")})\\b[^>]*>([\\s\\S]*?)</(?:${KEY_NAMES.joinToString("|")})>",
                Pattern.CASE_INSENSITIVE
            )
            val keyMatcher = keyTagPattern.matcher(block)
            if (keyMatcher.find()) {
                var rawKey = keyMatcher.group(1) ?: ""
                val nestedMatcher = Pattern.compile("<(?:tscut|key|shortcut)\\b[^>]*>([\\s\\S]*?)</", Pattern.CASE_INSENSITIVE).matcher(rawKey)
                if (nestedMatcher.find()) {
                    rawKey = nestedMatcher.group(1) ?: rawKey
                }
                key = cleanTrigger(rawKey)
            }
        }

        // 2. Cari child element untuk Hasil Ekspansi (Value) secara umum jika belum dapat
        if (value.isNullOrBlank()) {
            val valueTagPattern = Pattern.compile(
                "<(?:${VALUE_NAMES.joinToString("|")})\\b[^>]*>([\\s\\S]*?)</(?:${VALUE_NAMES.joinToString("|")})>",
                Pattern.CASE_INSENSITIVE
            )
            val valueMatcher = valueTagPattern.matcher(block)
            if (valueMatcher.find()) {
                val rawValue = valueMatcher.group(1) ?: ""
                value = cleanXmlEntities(rawValue)
            }
        }

        // 3. Jika tidak ditemukan di child element, periksa atribut di dalam container
        if (key.isNullOrBlank() || value.isNullOrBlank()) {
            val attrPair = extractPairFromAttributes(block)
            if (attrPair != null) {
                if (key.isNullOrBlank()) key = cleanTrigger(attrPair.first)
                if (value.isNullOrBlank()) value = attrPair.second
            }
        }

        // Pembersihan spasi ekstra di awal dan akhir trigger (contoh: " /otosc " -> "/otosc")
        val cleanKey = cleanTrigger(key ?: "")
        val cleanValue = value?.trim()

        return if (cleanKey.isNotBlank() && !cleanValue.isNullOrBlank()) {
            Pair(cleanKey, cleanValue)
        } else null
    }

    private fun extractPairFromAttributes(attrStr: String): Pair<String, String>? {
        var shortcut: String? = null
        var expansion: String? = null

        for (kName in KEY_NAMES) {
            val pattern = Pattern.compile("\\b$kName\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(attrStr)
            if (matcher.find()) {
                shortcut = cleanTrigger(matcher.group(1) ?: "")
                if (shortcut.isNotBlank()) break
            }
        }

        for (vName in VALUE_NAMES) {
            val pattern = Pattern.compile("\\b$vName\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(attrStr)
            if (matcher.find()) {
                expansion = cleanXmlEntities(matcher.group(1) ?: "")
                if (expansion.isNotBlank()) break
            }
        }

        return if (!shortcut.isNullOrBlank() && !expansion.isNullOrBlank()) {
            Pair(cleanTrigger(shortcut), expansion.trim())
        } else null
    }

    private fun parseLineByLineFallback(content: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val lines = content.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val attrPair = extractPairFromAttributes(trimmed)
            if (attrPair != null) {
                results.add(attrPair)
                continue
            }

            val inlineTagPattern = Pattern.compile(
                "<(?:${KEY_NAMES.joinToString("|")})>([^<]+)</[^>]+>[\\s\\S]*?<(?:${VALUE_NAMES.joinToString("|")})>([^<]+)</[^>]+>",
                Pattern.CASE_INSENSITIVE
            )
            val inlineMatcher = inlineTagPattern.matcher(trimmed)
            if (inlineMatcher.find()) {
                val k = cleanTrigger(inlineMatcher.group(1) ?: "")
                val v = cleanXmlEntities(inlineMatcher.group(2) ?: "")
                if (k.isNotBlank() && v.isNotBlank()) {
                    results.add(Pair(k, v))
                    continue
                }
            }

            if (trimmed.contains("=") && !trimmed.startsWith("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    val k = cleanTrigger(cleanQuotes(parts[0]))
                    val v = cleanQuotes(parts[1].trim())
                    if (k.isNotBlank() && v.isNotBlank()) {
                        results.add(Pair(k, v))
                        continue
                    }
                }
            }
        }

        return results.distinctBy { it.first.lowercase() }
    }

    /**
     * Membersihkan teks entitas XML, CDATA, dan tag enter (<ent__>).
     */
    fun cleanXmlEntities(text: String): String {
        var s = text.trim()

        if (s.startsWith("<![CDATA[") && s.endsWith("]]>")) {
            s = s.substring(9, s.length - 3)
        } else {
            s = s.replace("<![CDATA[", "").replace("]]>", "")
        }

        s = cleanTextFormatting(s)

        // Decode entitas XML standar
        s = s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

        // Decode entitas numerik dengan Kotlin Regex yang 100% thread-safe dan kompatibel
        s = Regex("&#(\\d+);").replace(s) { mr ->
            try {
                mr.groupValues[1].toInt().toChar().toString()
            } catch (_: Exception) { "" }
        }
        s = Regex("&#x([0-9a-fA-F]+);", RegexOption.IGNORE_CASE).replace(s) { mr ->
            try {
                mr.groupValues[1].toInt(16).toChar().toString()
            } catch (_: Exception) { "" }
        }

        return s.trim()
    }

    /**
     * Normalisasi baris baru, karakter tab, serta pembersihan format text#macro: dan HTML.
     * Mengonversi semua variasi tag enter (<ent__>, <enter>, <br>) menjadi karakter newline nyata \n
     * serta mengekstrak isi teks murni dari tag <body> jika berformat HTML.
     */
    fun cleanTextFormatting(text: String): String {
        var s = text

        // 1. Bersihkan prefix text#macro: jika ada
        if (s.startsWith("text#macro:", ignoreCase = true)) {
            s = s.replace(Regex("(?i)^text#macro:\\s*"), "")
        }

        // 2. Ekstrak konten di dalam tag <body> jika berformat HTML
        val bodyMatch = Regex("(?i)<body[^>]*>([\\s\\S]*?)</body>").find(s)
        if (bodyMatch != null && bodyMatch.groupValues.size > 1) {
            s = bodyMatch.groupValues[1]
        }

        // 3. Konversi tag enter & pemisah blok HTML menjadi newline
        s = s
            .replace(Regex("(?i)<ent__>"), "\n")
            .replace(Regex("(?i)<enter>"), "\n")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)</tr>"), "\n")
            .replace(Regex("(?i)</li>"), "\n")
            .replace(Regex("(?i)<tab__>"), "\t")

        // 4. Hapus blok style / script dan seluruh sisa tag HTML
        s = s.replace(Regex("(?i)<style\\b[^>]*>[\\s\\S]*?</style>"), "")
        s = s.replace(Regex("(?i)<script\\b[^>]*>[\\s\\S]*?</script>"), "")
        s = s.replace(Regex("<[^>]+>"), "")

        // 5. Decode HTML & XML Entities umum
        s = s
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")

        return s
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .trim()
    }

    private fun cleanBomAndWhitespace(text: String): String {
        var s = text
        if (s.startsWith("\uFEFF") || s.startsWith("\uFFFE")) {
            s = s.substring(1)
        }
        return s.trim()
    }

    private fun parseDelimitedText(textContent: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val lines = textContent.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

            if (line.contains("\t")) {
                val parts = line.split("\t", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), cleanQuotes(cleanTextFormatting(parts[1]))))
                    continue
                }
            }

            if (line.contains("=") && !line.startsWith("=")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), cleanQuotes(cleanTextFormatting(parts[1]))))
                    continue
                }
            }

            if (line.contains(";")) {
                val parts = line.split(";", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), cleanQuotes(cleanTextFormatting(parts[1]))))
                    continue
                }
            }

            if (line.contains(",")) {
                val parts = splitCsvLine(line)
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), cleanTextFormatting(parts[1])))
                    continue
                }
            }
        }

        return result.distinctBy { it.first.lowercase() }
    }

    private fun cleanQuotes(text: String): String {
        return if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'"))) {
            text.substring(1, text.length - 1).replace("\"\"", "\"")
        } else {
            text
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
                if (tokens.size == 1) {
                    val remainder = line.substring(i + 1).trim()
                    tokens.add(cleanQuotes(remainder))
                    return tokens
                }
            } else {
                sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString().trim())
        }
        return tokens
    }

    /**
     * 3. FITUR EKSPOR / AUTO-CONVERT KE JSON:
     * Mengubah daftar shortcut Room Database menjadi String JSON yang terstruktur rapi (indented 2 spasi).
     */
    fun exportToJson(shortcuts: List<ShortcutEntity>): String {
        val array = JSONArray()
        for (item in shortcuts) {
            val obj = JSONObject()
            obj.put("shortcut", item.shortcut)
            obj.put("expansion", item.expansion)
            array.put(obj)
        }
        return array.toString(2)
    }

    /**
     * Ekspor daftar shortcut ke format Map Key-Value JSON {"/otosc": "...", ...}
     */
    fun exportToMapJson(shortcuts: List<ShortcutEntity>): String {
        val root = JSONObject()
        for (item in shortcuts) {
            root.put(item.shortcut, item.expansion)
        }
        return root.toString(2)
    }

    /**
     * Ekspor daftar shortcut ke teks CSV standar.
     */
    fun exportToCsv(shortcuts: List<ShortcutEntity>): String {
        val sb = StringBuilder()
        sb.append("# Format Shortcut: shortcut,expansion\n")
        for (item in shortcuts) {
            val escapedExp = if (item.expansion.contains(",") || item.expansion.contains("\"") || item.expansion.contains("\n")) {
                "\"${item.expansion.replace("\"", "\"\"")}\""
            } else {
                item.expansion
            }
            sb.append("${item.shortcut},$escapedExp\n")
        }
        return sb.toString()
    }
}
