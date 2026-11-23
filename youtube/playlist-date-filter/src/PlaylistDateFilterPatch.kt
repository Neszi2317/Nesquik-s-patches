package fr.nesquik.patch.playlistdate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
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
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val OFFICIAL = "Lapp/morphe/extension/youtube/patches/PlaylistSearchPatch;"
private const val DIALOG = "Lapp/morphe/extension/youtube/patches/utils/SearchDialog;"
private const val HELPER = "Lfr/nesquik/playlistdate/PlaylistDateDialog;"
private const val EVENTS = "Lfr/nesquik/playlistdate/WatchLaterEvents;"
private const val REQUEST_CLASSES = "Lapp/morphe/extension/youtube/patches/utils/requests/"

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
    description = "Adds publication and Watch later addition-date filters to the official Playlist search dialog. " +
        "Imports Takeout dates and records confirmed Watch later additions locally. Select official Playlist search (2.4.0-dev.9).",
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

        // The native edit handler receives a typed response after the request completes.
        // Pin the descriptor and verify the casts and fields used by the runtime reader.
        val schema = mapOf(
            "Lbfpk;" to mapOf("d" to "Ljava/lang/String;", "e" to "Lbadh;"),
            "Lbjbp;" to mapOf("d" to "I", "e" to "Ljava/lang/String;"),
            "Lbfpl;" to mapOf("d" to "I", "j" to "Lbadh;"),
            "Lbfpo;" to mapOf("b" to "I", "c" to "Ljava/lang/Object;"),
            "Lbfpp;" to mapOf("c" to "Ljava/lang/String;", "d" to "Ljava/lang/String;"),
        )
        for ((type, fields) in schema) {
            val cls = classDefByOrNull(type) ?: throw PatchException("Unexpected YouTube playlist response type $type")
            if (!fields.all { (name, descriptor) -> cls.fields.any { it.name == name && it.type == descriptor } }) {
                throw PatchException("Unexpected YouTube playlist response fields in $type")
            }
        }

        val outcome = mutableClassDefByOrNull("Lalcm;")?.methods?.singleOrNull {
            it.name == "n" && it.parameterTypes.map(CharSequence::toString) ==
                listOf("Laksp;", "Ljava/lang/Object;") && it.returnType == "V"
        } ?: throw PatchException("Unsupported YouTube playlist edit completion. Use YouTube 21.30.216.")
        val nativeInstructions = outcome.implementation!!.instructions
        val responseRegister = outcome.implementation!!.registerCount - 1
        val responseCast = nativeInstructions.first()
        val requestCast = nativeInstructions.getOrNull(6)
        if (responseCast.opcode != Opcode.CHECK_CAST ||
            ((responseCast as? ReferenceInstruction)?.reference as? TypeReference)?.type != "Lbfpl;" ||
            (responseCast as OneRegisterInstruction).registerA != responseRegister ||
            requestCast?.opcode != Opcode.CHECK_CAST ||
            ((requestCast as? ReferenceInstruction)?.reference as? TypeReference)?.type != "Lbfpk;") {
            throw PatchException("Unexpected YouTube playlist edit completion layout.")
        }
        val requestRegister = (requestCast as OneRegisterInstruction).registerA
        if (requestRegister > 15 || responseRegister > 15 || requestRegister == responseRegister) {
            throw PatchException("Unexpected YouTube playlist edit completion registers.")
        }
        outcome.addInstruction(7,
            "invoke-static { v$requestRegister, v$responseRegister }, " +
                "$EVENTS->nativeResponse(Ljava/lang/Object;Ljava/lang/Object;)V")

        // Morphe's player Save to Watch later button sends its own JSON request.
        // Also cover Morphe callers that use its generic playlist editing request.
        for (name in listOf("SaveToWatchLaterRequest", "EditPlaylistRequest")) {
            val direct = mutableClassDefBy(REQUEST_CLASSES + name + ";").methods.singleOrNull { candidate ->
                candidate.name == "sendRequest" && candidate.parameterTypes.firstOrNull() == "Ljava/lang/String;"
            } ?: throw PatchException("Unsupported Morphe Watch later response. Use 2.4.0-dev.9.")
            val directInstructions = direct.implementation!!.instructions
            val bodyIndex = directInstructions.indices.singleOrNull { index ->
                val call = directInstructions[index] as? Instruction35c
                val target = call?.reference as? MethodReference
                call?.opcode == Opcode.INVOKE_STATIC && call.registerCount == 3 &&
                    target?.definingClass == REQUEST_CLASSES + "PlaylistRoutes;" && target.name == "editPlaylistBody"
            } ?: throw PatchException("Could not locate Morphe playlist edit body in $name.")
            val body = directInstructions[bodyIndex] as Instruction35c
            val bodyArgs = "v${body.registerC}, v${body.registerD}, v${body.registerE}"
            direct.replaceInstruction(bodyIndex, "invoke-static { $bodyArgs }, " +
                "$EVENTS->morpheAddBody(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)[B")

            for (index in directInstructions.indices.reversed()) {
                val instruction = directInstructions[index]
                if (instruction.opcode != Opcode.RETURN_OBJECT) continue
                val register = (instruction as OneRegisterInstruction).registerA
                direct.replaceInstruction(index, "invoke-static/range { v$register .. v$register }, " +
                    "$EVENTS->morpheResponse(Lorg/json/JSONObject;)V")
                direct.addInstruction(index + 1, "return-object v$register")
            }
        }
    }
}
