// SPDX-License-Identifier: MPL-2.0
package fr.nesquik.patches.sbrowser

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.*
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.Locale
import java.net.InetAddress
import java.net.URI

private const val helper = "Lfr/nesquik/sbrowser/blocker/WebsiteFilter;"
private const val terrace = "Lcom/sec/terrace/browser/net/TerraceUrlFilter;"
private const val bridge = "Lcom/sec/terrace/browser/browsing_data/TinUrlFilterBridge;"
private const val delegate = "Lcom/sec/terrace/browser/net/TerraceUrlFilter\$TerraceUrlFilterDelegate;"
private val subreddit = Regex("/r/[a-z0-9_]{1,64}")

// GURL serializes IPv6 using the first longest zero run and lowercase hexadecimal.
private fun ipv6Host(literal: String): String {
    require('%' !in literal) { "Zone IPv6 non prise en charge : $literal" }
    val raw = InetAddress.getByName(literal).address // bracketed numeric literal only
    val bytes = if (raw.size == 4) ByteArray(10) + byteArrayOf(-1, -1) + raw else raw
    val groups = bytes.asList().chunked(2).map { ((it[0].toInt() and 255) shl 8) or (it[1].toInt() and 255) }
    var bestStart = 0
    var bestLength = 1
    var run = 0
    groups.forEachIndexed { i, value ->
        run = if (value == 0) run + 1 else 0
        if (run > bestLength) { bestStart = i - run + 1; bestLength = run }
    }
    fun List<Int>.hex() = joinToString(":") { it.toString(16) }
    if (bestLength == 1) return "[${groups.hex()}]"
    return "[${groups.take(bestStart).hex()}::${groups.drop(bestStart + bestLength).hex()}]"
}

private fun normalize(token: String): String {
    var value = token.replace(Regex("^https?://", RegexOption.IGNORE_CASE), "")
    if (value.startsWith("/r/", true)) value = "reddit.com$value"
    val slash = value.indexOf('/')
    val prefix = (if (slash < 0) value else value.substring(0, slash)).lowercase(Locale.ROOT)
    var host = prefix.removePrefix("www.").removePrefix("*.").removeSuffix(".")
    require(host.isNotEmpty() && !host.contains('@')) { "Domaine invalide : $token" }
    val parsed = URI("http://$host/").parseServerAuthority()
    require(parsed.rawAuthority == host && parsed.host == host && parsed.port == -1) {
        "Domaine ou port non reconnu : $token"
    }
    if (host.startsWith("[")) host = ipv6Host(host)
    val path = if (slash < 0) "" else value.substring(slash).removeSuffix("/").lowercase(Locale.ROOT)
    if (host.endsWith(".reddit.com") && subreddit.matches(path)) host = "reddit.com"
    return host + path
}

@Suppress("unused")
val samsungWebsiteBlocker = bytecodePatch(
    name = "Samsung Internet - Local Website Blocker",
    description = "Blocage local HTTP(S) par domaine ou chemin, configuré avant la compilation.",
    default = false,
) {
    compatibleWith(Compatibility(name = "Samsung Internet", packageName = "com.sec.android.app.sbrowser",
        apkFileType = ApkFileType.APK, targets = listOf(AppTarget("31.0.2.41"))))
    extendWith("extensions/nesquik-sbrowser-blocker.mpe")
    val domains by stringsOption(key = "blockedDomains", default = emptyList(), required = false,
        title = "Domaines bloqués", description = "Domaines, chemins ou /r/subreddit. Inclut les sous-domaines.")
    val txt by filePathOption(key = "domainsFile", required = false, allowedExtensions = listOf("txt"),
        title = "Importer un TXT", description = "Liste UTF-8 fusionnée avec les entrées manuelles.")
    execute {
        val rules = sortedSetOf<String>()
        DomainRules.merge(domains.orEmpty(), txt).forEach { line ->
            for (token in line.split(Regex("\\s+"))) {
                if (token.startsWith("#")) break
                if (token.isNotEmpty()) rules.add(normalize(token))
            }
        }
        require(rules.size <= 50_000) { "Maximum 50 000 règles" }
        if (rules.isEmpty()) return@execute
        val isBlocked = mutableClassDefBy(terrace).methods.single {
            it.name == "isBlockedUrl" && it.returnType == "Z" && it.parameterTypes == listOf("Ljava/lang/String;")
        }
        val original = isBlocked.implementation!!
        check(isBlocked.accessFlags and 8 != 0 && original.registerCount == 2 &&
            original.instructions.first().opcode == Opcode.IF_NEZ &&
            original.instructions.any { it.getReference<MethodReference>()?.toString() == "$delegate->isBlockedUrl(Ljava/lang/String;)Z" })
        val supported = mutableClassDefBy(bridge).methods.single {
            it.name == "isUrlFilterSupported" && it.returnType == "Z" && it.parameterTypes.isEmpty()
        }
        val code = supported.implementation!!.instructions
        check(code.size == 3 && code[0].getReference<MethodReference>()?.toString() == "$terrace->isUrlFilterSupported()Z" &&
            code[1].opcode == Opcode.MOVE_RESULT && code[2].opcode == Opcode.RETURN)
        supported.addInstructions(2, """
            invoke-static {v0}, $helper->rememberSupport(Z)Z
            move-result v0
        """.trimIndent())
        isBlocked.addInstructionsWithLabels(0, """
            invoke-static {p0}, $helper->blocked(Ljava/lang/String;)Z
            move-result v0
            if-nez v0, :local_result
            invoke-static {}, $helper->stockEnabled()Z
            move-result v0
            if-nez v0, :stock_filter
            :local_result
            return v0
            :stock_filter
            nop
        """.trimIndent())

        // The first parameter is scratch space for a literal. The second holds the rules.
        val populate = mutableClassDefBy(helper).methods.single { it.name == "populate" }
        check(populate.parameterTypes == listOf("Ljava/lang/String;", helper))
        val chunks = mutableListOf<String>()
        val chunk = StringBuilder()
        for (rule in rules) {
            if (chunk.length + rule.length > 12000 && chunk.isNotEmpty()) {
                chunks.add(chunk.toString())
                chunk.clear()
            }
            if (chunk.isNotEmpty()) chunk.append("\\n")
            chunk.append(rule)
        }
        if (chunk.isNotEmpty()) chunks.add(chunk.toString())
        populate.addInstructions(0, chunks.joinToString("\n") {
            "const-string/jumbo p0, \"$it\"\ninvoke-virtual {p1, p0}, $helper->addChunk(Ljava/lang/String;)V"
        })
        println("Samsung Internet Local Website Blocker : ${rules.size} règle(s) intégrée(s)")
    }
}
