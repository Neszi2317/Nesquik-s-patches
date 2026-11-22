// SPDX-License-Identifier: MPL-2.0
package local.nesquikos.firefoxblocker

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal object DomainRules {
    private const val MAX_BYTES = 1024 * 1024
    private const val MAX_RULES = 50_000

    fun merge(manual: List<String>, filePath: String?): List<String> {
        val entries = manual.flatMap { it.lines() }.toMutableList()
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            require(file.isFile && file.length() <= MAX_BYTES) { "TXT absent ou supérieur à 1 Mio" }
            val bytes = file.inputStream().use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val size = stream.read(buffer)
                    if (size == -1) break
                    require(output.size() + size <= MAX_BYTES) { "TXT supérieur à 1 Mio" }
                    output.write(buffer, 0, size)
                }
                output.toByteArray()
            }
            val text = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
            entries.addAll(text.lines())
        }

        require(entries.sumOf { it.length.toLong() } <= MAX_BYTES) { "Liste supérieure à 1 Mio" }
        val rules = sortedSetOf<String>()
        entries.forEachIndexed { index, entry ->
            val rule = entry.trim { it <= ' ' }
            require(rule.all { it in ' '..'~' && it !in "\"'\\`" }) {
                "Entrée ${index + 1} : caractère invalide"
            }
            if (rule.isNotEmpty()) rules.add(rule)
        }
        require(rules.size <= MAX_RULES) { "Maximum 50000 règles" }
        return rules.toList()
    }
}
