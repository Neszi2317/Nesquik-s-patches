package app.crimera.patches.newx.timeline

import com.nesquik.patches.x.*
import app.crimera.bytecode.Target
import app.crimera.bytecode.insertHook
import app.crimera.bytecode.methodReference
import app.crimera.patches.common.requireExactlyOne
import app.crimera.patches.newx.models.*
import app.crimera.patches.common.semantic.fieldForFlattenedFormatterLabel
import app.crimera.patches.newx.utils.HIDE_SENSITIVE_COMPATIBILITY
import app.crimera.patches.newx.utils.Constants.TIMELINE_FILTER_DESCRIPTOR
import app.crimera.patches.newx.utils.unsupportedOnMount
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patcher.util.numberOfParameterRegisters
import app.morphe.patcher.util.p0Register
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.cloneMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val SENSITIVE = "Lapp/morphe/extension/newx/hidesensitive/SensitivePostFilter;"
private const val OBJECT = "Ljava/lang/Object;"

@Suppress("unused")
val newXHideAllSensitivePostsPatch = bytecodePatch(
    name = "NewX: Hide all sensitive posts",
    description = "Removes sensitive posts and their containers before rendering, including quoted and reposted media.",
) {
    unsupportedOnMount()
    compatibleWith(HIDE_SENSITIVE_COMPATIBILITY)
    extendWith("extensions/hide-sensitive.mpe")
    requirePikoSettingsResources()
    finalize {
        requirePikoTimeline()
        installSensitiveBridges()
        registerSensitiveToggle()
        val enabled = sensitiveBridge("isEnabled")
        enabled.insertHook(0, relocateBranchTargets = false) {
            constString(0, "newx.content.hide_all_sensitive_posts")
            constInt(1, 1)
            invokeStatic(methodReference("Lapp/morphe/extension/crimera/settings/SettingsRegistry;->getBooleanOrDefault(Ljava/lang/String;Z)Z"), 0, 1)
            moveResult(0, "Z")
            returnValue(0)
        }
        val filter = mutableClassDefBy(TIMELINE_FILTER_DESCRIPTOR).requireBridge("filterTimelineItems", OBJECT, OBJECT)
        filter.insertHook(0, relocateBranchTargets = false) {
            invokeStatic(methodReference("$SENSITIVE->filter($OBJECT)$OBJECT"), filter.p0Register)
            moveResult(filter.p0Register, OBJECT)
        }
    }
}

/** Replace a compile-time bridge with a body having explicit, verified scratch registers. */
context(context: BytecodePatchContext)
private fun sensitiveBridge(name: String): MutableMethod {
    val owner = context.mutableClassDefBy(SENSITIVE)
    val old = requireExactlyOne("HideSensitive bridge $name", owner.methods.filter { it.name == name })
    val method = old.cloneMutable(additionalRegisters = old.numberOfParameterRegisters + 4)
    owner.methods.remove(old)
    owner.methods.add(method)
    val code = method.implementation ?: throw PatchException("HideSensitive bridge has no body: $name")
    while (code.instructions.isNotEmpty()) code.removeInstruction(code.instructions.lastIndex)
    return method
}

context(_: BytecodePatchContext)
private fun model(label: String) = Fingerprint(
    definingClass = "Lcom/x/models/", name = "toString", returnType = "Ljava/lang/String;",
    parameters = emptyList(), filters = listOf(string(label)),
).requireSingle("HideSensitive model $label")

context(context: BytecodePatchContext)
private fun installSensitiveBridges() {
    val models = resolvedNewXPostMediaModels()
    val posts = models.postModels
    val timeline = resolvedNewXTimelineModels()
    val runtime = context.mutableClassDefBy(SENSITIVE)
    val shared = context.mutableClassDefBy(TIMELINE_FILTER_DESCRIPTOR)
    val forwarding = setOf("isTimelinePost", "isTimelineModule", "isTimelineModuleItem",
        "isModuleItemDispensable", "getModuleItem", "copyModuleItem", "getModuleInnerContent",
        "getModuleHeader", "getModuleFooter", "getModuleDisplayType", "getModuleSortIndex",
        "getModuleEntryId", "getModuleClientEventInfo", "getPostId", "isVerticalConversation",
        "getVerticalConversationPostIds", "copyVerticalConversation", "copyModule", "immutableList")
    forwarding.forEach { name ->
        val stub = requireExactlyOne("HideSensitive forwarding stub $name", runtime.methods.filter { it.name == name })
        val target = requireExactlyOne("Piko timeline bridge $name", shared.methods.filter {
            it.name == name && it.parameterTypes == stub.parameterTypes && it.returnType == stub.returnType
        })
        if (!AccessFlags.STATIC.isSet(target.accessFlags)) throw PatchException("Expected static bridge $target")
        target.accessFlags = (target.accessFlags and AccessFlags.PRIVATE.value.inv() and AccessFlags.PROTECTED.value.inv()) or AccessFlags.PUBLIC.value
        val method = sensitiveBridge(name)
        method.insertHook(0, relocateBranchTargets = false) {
            val args = (method.p0Register until method.p0Register + method.numberOfParameterRegisters).toList().toIntArray()
            invokeStatic(target, *args)
            moveResult(0, method.returnType)
            when (method.returnType) {
                "J", "D" -> returnWide(0)
                "Z", "I" -> returnValue(0)
                else -> returnObject(0)
            }
        }
    }
    fun accessor(field: FieldReference) = context.mutableClassDefBy(field.definingClass)
        .resolveFieldAccessor(field, "HideSensitive ${field.name}")
    fun getter(name: String, field: FieldReference) {
        val read = accessor(field)
        val method = sensitiveBridge(name)
        method.insertHook(0, relocateBranchTargets = false) {
            move(0, method.p0Register, OBJECT)
            checkCast(0, field.definingClass)
            readModelAccessor(read, 0)
            if (field.type == "Z") returnValue(0) else returnObject(0)
        }
        println("HideSensitive $name = $field")
    }
    val contextual = model("ContextualPost(canonicalPost=")
    val canonical = model("CanonicalPost(id=")
    val repost = model("RePostedPost(canonicalPost=")
    if (contextual.originalClassDef.type != posts.contextualPostDescriptor ||
        canonical.originalClassDef.type != posts.canonicalPostDescriptor ||
        repost.originalClassDef.type != posts.contextualRepostedPostField.type)
        throw PatchException("HideSensitive model resolution disagrees with Piko")
    getter("getCanonical", posts.contextualCanonicalPostField)
    getter("getQuote", contextual.fieldForToStringLabel(", quotedPost="))
    getter("getRepost", posts.contextualRepostedPostField)
    getter("getRepostCanonical", posts.repostedCanonicalPostField)
    getter("getRepostQuote", repost.fieldForToStringLabel(", quotedPost="))
    getter("getInterstitial", models.contextualTweetInterstitialField)
    getter("getUserSensitiveFlag", canonical.fieldForToStringLabel(", hasUserFlaggedSensitiveMedia="))
    getter("getMedia", models.canonicalPostMediaField)
    val sensitive = canonical.originalMethod.fieldForFlattenedFormatterLabel(", isPossiblySensitive=") {
        context.classDefByOrNull(it)
    }
    if (sensitive.type != "Z") throw PatchException("isPossiblySensitive must be boolean: $sensitive")
    getter("isPossiblySensitive", sensitive)

    val isContextual = sensitiveBridge("isContextual")
    isContextual.insertHook(0, relocateBranchTargets = false) {
        instanceOf(0, isContextual.p0Register, posts.contextualPostDescriptor)
        returnValue(0)
    }
    val isSensitivePost = sensitiveBridge("isSensitivePost")
    val resultRead = accessor(timeline.postResultField)
    isSensitivePost.insertHook(0, relocateBranchTargets = false) {
        move(0, isSensitivePost.p0Register, OBJECT)
        checkCast(0, timeline.postDescriptor)
        readModelAccessor(resultRead, 0)
        constInt(1, 0)
        invokeStatic(methodReference("$SENSITIVE->sensitiveResult(${OBJECT}I)Z"), 0, 1)
        moveResult(0, "Z")
        returnValue(0)
    }
    val visibility = model("MediaVisibilityResults(blurImageInterstitial=")
    if (visibility.originalClassDef.type != models.contextualMediaVisibilityResultsField.type)
        throw PatchException("HideSensitive visibility type mismatch")
    val visibilityRead = accessor(models.contextualMediaVisibilityResultsField)
    val blurRead = accessor(visibility.fieldForToStringLabel("MediaVisibilityResults(blurImageInterstitial="))
    val blur = sensitiveBridge("getBlurInterstitial")
    blur.insertHook(0, relocateBranchTargets = false) {
        move(0, blur.p0Register, OBJECT)
        checkCast(0, posts.contextualPostDescriptor)
        readModelAccessor(visibilityRead, 0)
        ifEqz(0, Target.Local("no_blur"))
        readModelAccessor(blurRead, 0)
        label("no_blur")
        returnObject(0)
    }
    val warning = model("SensitiveMediaWarning(adultContent=")
    val warningType = warning.originalClassDef.type
    val warningFields = warning.classDef.fields.filter { !AccessFlags.STATIC.isSet(it.accessFlags) && it.type == "Z" }
    if (warningFields.size < 3) throw PatchException("Incomplete SensitiveMediaWarning flags: $warningFields")
    val reads = warningFields.map { accessor(it) }
    val warningMethod = sensitiveBridge("warningIsSensitive")
    warningMethod.insertHook(0, relocateBranchTargets = false) {
        move(0, warningMethod.p0Register, OBJECT)
        checkCast(0, warningType)
        reads.forEach { read ->
            readModelAccessor(read, 0, 1)
            ifNez(1, Target.Local("sensitive"))
        }
        constInt(1, 0)
        returnValue(1)
        label("sensitive")
        constInt(1, 1)
        returnValue(1)
    }
    val mediaReads = listOf("MediaContentImage(mediaId=", "MediaContentGif(mediaId=", "MediaContentVideo(mediaId=").map { label ->
        val media = model(label)
        // The unique field of the semantically resolved warning type avoids formatter lowering.
        val field = media.classDef.requireSingleInstanceField(warningType, "sensitive media warning")
        media.originalClassDef.type to accessor(field)
    }
    val mediaWarning = sensitiveBridge("getMediaWarning")
    mediaWarning.insertHook(0, relocateBranchTargets = false) {
        mediaReads.forEachIndexed { index, (type, read) ->
            instanceOf(0, mediaWarning.p0Register, type)
            ifEqz(0, Target.Local("next_media_$index"))
            move(0, mediaWarning.p0Register, OBJECT)
            checkCast(0, type)
            readModelAccessor(read, 0)
            returnObject(0)
            label("next_media_$index")
        }
        constInt(0, 0)
        returnObject(0)
    }
}
