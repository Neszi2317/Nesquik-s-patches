// SPDX-License-Identifier: MPL-2.0
package local.nesquikos.firefoxblocker

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal object OmniEditor {
    private const val STARTUP = "modules/GeckoViewStartup.sys.mjs"
    private const val MODULE = "modules/LocalNesquikosWebsiteBlocker.sys.mjs"
    private const val ANCHOR = "case \"app-startup\": {"
    private const val IMPORT = "ChromeUtils.importESModule(\"resource://gre/$MODULE\").install();"
    private const val RULES_MARKER = "/*__LWB_RULES__*/ []"

    fun apply(omni: File, rules: List<String>) {
        require(omni.isFile) { "assets/omni.ja absent" }
        val temp = Files.createTempFile(omni.parentFile.toPath(), "omni-lwb-", ".tmp")
        try {
            ZipFile(omni).use { input ->
                require(input.getEntry(MODULE) == null) {
                    "APK déjà patché par Local Website Blocker. Repartir de l’APK original."
                }
                val entry = requireNotNull(input.getEntry(STARTUP)) { "GeckoViewStartup absent" }
                val startup = input.getInputStream(entry).bufferedReader().use { it.readText() }
                require(startup.split(ANCHOR).size == 2 && "case \"content-process-ready-for-script\":" in startup) {
                    "Structure GeckoViewStartup inconnue"
                }
                val patchedStartup = startup.replace(ANCHOR, "$ANCHOR\n        $IMPORT")
                val template = checkNotNull(javaClass.getResourceAsStream("/LocalWebsiteBlocker.sys.mjs"))
                    .bufferedReader().use { it.readText() }
                check(template.split(RULES_MARKER).size == 2) { "Marqueur de règles absent ou ambigu" }
                // DomainRules excludes quotes, backslashes and non-printable characters.
                val module = template.replace(RULES_MARKER, rules.joinToString(",", "[", "]") { "\"$it\"" })

                ZipOutputStream(Files.newOutputStream(temp).buffered()).use { output ->
                    for (source in input.entries().asSequence()) {
                        require("startupCache" !in source.name) { "Cache JS embarqué inconnu" }
                        output.putNextEntry(ZipEntry(source.name).apply { time = source.time })
                        if (source.name == STARTUP) {
                            output.write(patchedStartup.toByteArray(Charsets.UTF_8))
                        } else {
                            input.getInputStream(source).use { it.copyTo(output) }
                        }
                        output.closeEntry()
                    }
                    output.putNextEntry(ZipEntry(MODULE))
                    output.write(module.toByteArray(Charsets.UTF_8))
                    output.closeEntry()
                }
            }
            ZipFile(temp.toFile()).use { check(it.getEntry(MODULE) != null) }
            Files.move(temp, omni.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temp)
        }
    }
}
