package fr.nesquik.patch.playlistdate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val OFFICIAL = "Lapp/morphe/extension/youtube/patches/PlaylistSearchPatch;"
private const val DIALOG = "Lapp/morphe/extension/youtube/patches/utils/SearchDialog;"
private const val HELPER = "Lfr/nesquik/playlistdate/PlaylistDateDialog;"

private object PlaylistActionBar : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    filters = listOf(string("Unhandled menu item %s"), opcode(Opcode.RETURN_VOID)),
)

private val dialogArguments = listOf(
    "Landroid/content/Context;", "Ljava/lang/String;", "Ljava/util/function/Consumer;",
)

@Suppress("unused")
val playlistDateFilter = bytecodePatch(
    name = "YouTube: Playlist Search & Date Filter",
    description = "Adds an inclusive publication-date filter to the official Playlist search dialog. " +
        "Select Morphe Playlist search (patches 2.4.0-dev.9) in the same patching session.",
    default = false,
) {
    compatibleWith(Compatibility(
        name = "YouTube",
        packageName = "com.google.android.youtube",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF0033,
        targets = listOf(AppTarget(version = "21.30.216")),
    ))

    extendWith("extensions/nesquik-playlist-date.dex")

    finalize {
        val actionBar = PlaylistActionBar.method.implementation!!.instructions
        val officialIsEnabled = actionBar.any {
            val method = (it as? ReferenceInstruction)?.reference as? MethodReference
            method?.definingClass == OFFICIAL && method.name == "addSearchMenuItem"
        }
        if (!officialIsEnabled) {
            throw PatchException("Select the official Morphe Playlist search patch (2.4.0-dev.9).")
        }

        val method = mutableClassDefBy(OFFICIAL).methods.single { it.name == "showSearchDialog" }
        val instructions = method.implementation!!.instructions
        val callIndex = instructions.indices.singleOrNull { index ->
            val call = instructions[index] as? Instruction35c
            val target = call?.reference as? MethodReference
            call?.opcode == Opcode.INVOKE_STATIC && target?.definingClass == DIALOG &&
                target.name == "show" && target.parameterTypes.map(CharSequence::toString) == dialogArguments
        } ?: throw PatchException("Unsupported official Playlist search dialog. Use Morphe patches 2.4.0-dev.9.")

        // The original consumer captures the activity and playlist ID immediately before show().
        val show = instructions[callIndex] as Instruction35c
        val capture = instructions.getOrNull(callIndex - 1) as? Instruction35c
        val constructor = capture?.reference as? MethodReference
        if (capture?.opcode != Opcode.INVOKE_DIRECT || capture.registerCount != 3 ||
            capture.registerC != show.registerE || capture.registerD != show.registerC ||
            constructor?.name != "<init>" || constructor.parameterTypes.map(CharSequence::toString) !=
                listOf("Landroid/app/Activity;", "Ljava/lang/String;")
        ) {
            throw PatchException("Unsupported Playlist search callback. Use Morphe patches 2.4.0-dev.9.")
        }
        val args = "v${show.registerC}, v${show.registerD}, v${show.registerE}, v${capture.registerE}"
        method.replaceInstruction(callIndex,
            "invoke-static { $args }, $HELPER->show(" +
                "Landroid/app/Activity;Ljava/lang/String;Ljava/util/function/Consumer;Ljava/lang/String;)V"
        )
    }
}
