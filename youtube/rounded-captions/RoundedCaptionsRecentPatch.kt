package patches.youtube.captions

// Adapted from Neszi2317/Rounded-Subtiles v1.0.0-dev.6 (GPL-3.0).
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val WINDOW = "Lcom/google/android/libraries/youtube/player/subtitles/ui/SubtitleWindowView;"
private const val ALIGNMENT = "Landroid/text/Layout\$Alignment;"
private const val ROUND_RECT = "Landroid/graphics/Path;->addRoundRect(Landroid/graphics/RectF;[FLandroid/graphics/Path\$Direction;)V"

@Suppress("unused")
val roundedCaptions2146522 = roundedCaptionsPatch("21.46.522", "Lathc;")

@Suppress("unused")
val roundedCaptions2147483 = roundedCaptionsPatch("21.47.483", "Latdz;")

private fun roundedCaptionsPatch(version: String, captionView: String) = bytecodePatch(
    name = "Rounded YouTube Captions ($version)",
    description = "Uses the native rounded subtitle background and centers caption text.",
    default = true,
) {
    compatibleWith(Compatibility(
        name = "YouTube",
        packageName = "com.google.android.youtube",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF0033,
        targets = listOf(AppTarget(version = version, isExperimental = true)),
    ))

    execute {
        if (packageMetadata.versionName != version) return@execute

        val layout = mutableClassDefBy(captionView).methods.single {
            it.name == "onLayout" && it.parameterTypes.map(CharSequence::toString) == listOf("Z", "I", "I", "I", "I")
        }
        val layoutInstructions = layout.implementation!!.instructions
        val roundedFlagIndex = layoutInstructions.indices.singleOrNull { index ->
            val instruction = layoutInstructions[index]
            val branch = layoutInstructions.getOrNull(index + 1)
            instruction.opcode == Opcode.IGET_BOOLEAN && instruction.references("$captionView->b:Z") &&
                branch?.opcode == Opcode.IF_EQZ &&
                (instruction as TwoRegisterInstruction).registerA == (branch as OneRegisterInstruction).registerA
        } ?: throw PatchException("Could not find the rounded subtitle background switch for $version.")
        if (layoutInstructions.drop(roundedFlagIndex + 1).count { it.references(ROUND_RECT) } != 1) {
            throw PatchException("Could not verify the native rounded subtitle path for $version.")
        }

        val measure = mutableClassDefBy(WINDOW).methods.single {
            it.name == "onMeasure" && it.parameterTypes.map(CharSequence::toString) == listOf("I", "I")
        }
        val measureInstructions = measure.implementation!!.instructions
        val alignmentIndices = measureInstructions.indices.filter { index ->
            val instruction = measureInstructions[index]
            val assignment = measureInstructions.getOrNull(index + 1)
            instruction.opcode == Opcode.SGET_OBJECT && instruction.references("$ALIGNMENT->ALIGN_NORMAL:$ALIGNMENT") &&
                assignment?.opcode == Opcode.IPUT_OBJECT && assignment.references("$captionView->a:$ALIGNMENT") &&
                (instruction as OneRegisterInstruction).registerA == (assignment as TwoRegisterInstruction).registerA
        }
        if (alignmentIndices.size != 2) throw PatchException("Expected two subtitle alignment assignments for $version.")

        val flag = (layoutInstructions[roundedFlagIndex] as TwoRegisterInstruction).registerA
        layout.replaceInstruction(roundedFlagIndex, "const/4 v$flag, 0x1")
        for (index in alignmentIndices) {
            val register = (measureInstructions[index] as OneRegisterInstruction).registerA
            measure.replaceInstruction(index, "sget-object v$register, $ALIGNMENT->ALIGN_CENTER:$ALIGNMENT")
        }
    }
}

private fun Instruction.references(descriptor: String) =
    (this as? ReferenceInstruction)?.reference?.toString() == descriptor
