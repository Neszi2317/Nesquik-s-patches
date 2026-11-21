package app.morphe.patches.youtube.layout.universalcaptiontranslation

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// Reconstructed from the supplied 0.3.0 classes. No obfuscated class names.
internal object TranslationTracksFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;", parameters = emptyList(), strings = listOf("&tlang=")
)
internal object CaptionMenuFingerprint : Fingerprint(
    classFingerprint = TranslationTracksFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;", parameters = emptyList(),
    strings = listOf("AUTO_TRANSLATE_CAPTIONS_OPTION")
)
