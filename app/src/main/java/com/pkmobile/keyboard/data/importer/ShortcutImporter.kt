package com.pkmobile.keyboard.data.importer

import com.pkmobile.keyboard.data.db.ShortcutEntity
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Engine import/export cerdas untuk memindahkan shortcut dari Perfect Keyboard
 * maupun format backup lainnya (XML, CSV, TSV, JSON, Key-Value).
 */
object ShortcutImporter {

    /**
     * Membaca string mentah (dari file atau paste clipboard) dan mengekstrak
     * seluruh pasangan shortcut dan teks ekspansinya.
     */
    fun parse(rawContent: String): List<Pair<String, String>> {
        val content = rawContent.trim()
        if (content.isEmpty()) return emptyList()

        return when {
            content.startsWith("<") && content.endsWith(">") -> parseXml(content)
            content.startsWith("[") || content.startsWith("{") -> parseJson(content)
            else -> parseDelimitedText(content)
        }
    }

    /**
     * Parsing format backup XML Perfect Keyboard / Smart Keyboard.
     * Mendukung berbagai struktur tag XML (baik berbasis attribute maupun child elements).
     */
    private fun parseXml(xmlContent: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentKey: String? = null
            var currentValue: String? = null
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.lowercase()

                        // Cek apakah atribut tag langsung memuat shortcut/expansion
                        // Contoh: <shortcut key="omw" value="On my way!" />
                        val attrShortcut = parser.getAttributeValue(null, "shortcut")
                            ?: parser.getAttributeValue(null, "key")
                            ?: parser.getAttributeValue(null, "name")
                            ?: parser.getAttributeValue(null, "from")

                        val attrExpansion = parser.getAttributeValue(null, "expansion")
                            ?: parser.getAttributeValue(null, "value")
                            ?: parser.getAttributeValue(null, "replacement")
                            ?: parser.getAttributeValue(null, "to")
                            ?: parser.getAttributeValue(null, "text")

                        if (!attrShortcut.isNullOrBlank() && !attrExpansion.isNullOrBlank()) {
                            result.add(Pair(attrShortcut.trim(), attrExpansion.trim()))
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            when (currentTag) {
                                "key", "shortcut", "from", "keyword" -> currentKey = text
                                "value", "expansion", "to", "replacement", "text" -> currentValue = text
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name.lowercase()
                        if (endTag == "record" || endTag == "item" || endTag == "shortcut" || endTag == "word") {
                            if (!currentKey.isNullOrBlank() && !currentValue.isNullOrBlank()) {
                                result.add(Pair(currentKey!!.trim(), currentValue!!.trim()))
                            }
                            currentKey = null
                            currentValue = null
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Fallback dengan regex jika XML parser menemukan formatting tag tidak standar
            result.addAll(parseXmlWithRegex(xmlContent))
        }

        return result.distinctBy { it.first.lowercase() }
    }

    /**
     * Regex fallback untuk XML yang mungkin sedikit korup atau tidak well-formed.
     */
    private fun parseXmlWithRegex(content: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()

        // Pola 1: <key>...</key><value>...</value>
        val patternTags = Regex("<(?:key|shortcut)>\\s*([^<]+)\\s*</(?:key|shortcut)>[\\s\\S]*?<(?:value|expansion|replacement|text)>\\s*([^<]+)\\s*</(?:value|expansion|replacement|text)>", RegexOption.IGNORE_CASE)
        for (match in patternTags.findAll(content)) {
            val k = match.groupValues[1].trim()
            val v = match.groupValues[2].trim()
            if (k.isNotEmpty() && v.isNotEmpty()) {
                result.add(Pair(k, v))
            }
        }

        // Pola 2: key="..." value="..."
        val patternAttrs = Regex("(?:key|shortcut)=\"([^\"]+)\"[^>]*?(?:value|expansion|text)=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        for (match in patternAttrs.findAll(content)) {
            val k = match.groupValues[1].trim()
            val v = match.groupValues[2].trim()
            if (k.isNotEmpty() && v.isNotEmpty()) {
                result.add(Pair(k, v))
            }
        }

        return result
    }

    /**
     * Parsing format JSON array atau object.
     * Contoh: [{"shortcut":"omw", "expansion":"On my way!"}]
     */
    private fun parseJson(jsonContent: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            if (jsonContent.startsWith("[")) {
                val array = JSONArray(jsonContent)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    extractPairFromJson(obj)?.let { result.add(it) }
                }
            } else {
                val rootObj = JSONObject(jsonContent)
                val shortcutsArray = rootObj.optJSONArray("shortcuts")
                    ?: rootObj.optJSONArray("data")
                    ?: rootObj.optJSONArray("items")

                if (shortcutsArray != null) {
                    for (i in 0 until shortcutsArray.length()) {
                        val obj = shortcutsArray.optJSONObject(i) ?: continue
                        extractPairFromJson(obj)?.let { result.add(it) }
                    }
                } else {
                    // Coba baca format Map key-value {"omw": "On my way!", "brb": "Be right back"}
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = rootObj.optString(k, "")
                        if (k.isNotBlank() && v.isNotBlank()) {
                            result.add(Pair(k.trim(), v.trim()))
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result.distinctBy { it.first.lowercase() }
    }

    private fun extractPairFromJson(obj: JSONObject): Pair<String, String>? {
        val shortcut = obj.optString("shortcut").takeIf { it.isNotBlank() }
            ?: obj.optString("key").takeIf { it.isNotBlank() }
            ?: obj.optString("name").takeIf { it.isNotBlank() }
            ?: obj.optString("from").takeIf { it.isNotBlank() }

        val expansion = obj.optString("expansion").takeIf { it.isNotBlank() }
            ?: obj.optString("value").takeIf { it.isNotBlank() }
            ?: obj.optString("replacement").takeIf { it.isNotBlank() }
            ?: obj.optString("text").takeIf { it.isNotBlank() }
            ?: obj.optString("to").takeIf { it.isNotBlank() }

        return if (!shortcut.isNullOrBlank() && !expansion.isNullOrBlank()) {
            Pair(shortcut.trim(), expansion.trim())
        } else null
    }

    /**
     * Parsing format Delimited Text (CSV, TSV, baris Tab-separated, atau key=value).
     */
    private fun parseDelimitedText(textContent: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val lines = textContent.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

            // 1. Coba deteksi pemisah Tab (TSV)
            if (line.contains("\t")) {
                val parts = line.split("\t", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), cleanQuotes(parts[1].trim())))
                    continue
                }
            }

            // 2. Coba deteksi tanda sama dengan (omw=On my way!)
            if (line.contains("=") && !line.startsWith("=")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), cleanQuotes(parts[1].trim())))
                    continue
                }
            }

            // 3. Coba deteksi titik koma (omw;On my way!)
            if (line.contains(";")) {
                val parts = line.split(";", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), cleanQuotes(parts[1].trim())))
                    continue
                }
            }

            // 4. Coba deteksi koma (CSV: omw,On my way!)
            if (line.contains(",")) {
                val parts = splitCsvLine(line)
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    result.add(Pair(parts[0].trim(), parts[1].trim()))
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
                    // Sisanya adalah teks ekspansi lengkap
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

    /**
     * Ekspor daftar shortcut ke format JSON.
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
}
