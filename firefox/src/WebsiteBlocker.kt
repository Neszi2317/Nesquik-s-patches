// SPDX-License-Identifier: MPL-2.0
package local.nesquikos.firefoxblocker

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.filePathOption
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringsOption

@Suppress("unused")
val websiteBlockerPatch = rawResourcePatch(
    name = "Firefox - Local Website Blocker",
    description = "Blocage local HTTP(S) par domaine ou chemin, configuré dans Morphe avant de patcher.",
) {
    compatibleWith(
        Compatibility(
            packageName = "org.mozilla.firefox",
            name = "Firefox",
            apkFileType = ApkFileType.APK,
            targets = listOf(AppTarget("159.0"), AppTarget("160.0"), AppTarget("160.0.1")),
        )
    )

    val domains by stringsOption(
        key = "blockedDomains",
        default = emptyList(),
        title = "Domaines bloqués",
        description = "Ajoutez les domaines ou URL un par un. La liste est fusionnée avec le TXT.",
        required = false,
    )
    val txt by filePathOption(
        key = "domainsFile",
        title = "Importer un TXT",
        description = "Fichier UTF-8, un domaine ou une URL par ligne. Aucun fichier requis après installation.",
        required = false,
        allowedExtensions = listOf("txt"),
    )

    execute {
        val rules = DomainRules.merge(domains.orEmpty(), txt)
        OmniEditor.apply(get("assets/omni.ja"), rules)
        println("Local Website Blocker: ${rules.size} règle(s) intégrée(s).")
    }
}
