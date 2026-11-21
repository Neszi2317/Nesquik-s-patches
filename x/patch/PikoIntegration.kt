package com.nesquik.patches.x

import app.crimera.bytecode.Block
import app.crimera.bytecode.insertHook
import app.crimera.bytecode.methodReference
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val REGISTRY = "Lapp/morphe/extension/crimera/settings/SettingsRegistry;"
private const val TIMELINE = "Lapp/morphe/extension/newx/timeline/NewXTimelineFilter;"
private const val TEXT = "Ljava/lang/String;"
private const val CONTENT = "newx.content"
private const val LINK_ERROR = "Nesquik X requires official Piko 3.12.0-dev.5 with NewX: Filter posts by keyword selected."

private val labels = resourcePatch(default = false) {
    execute {
        for ((folder, file) in listOf("values" to "personal-strings.xml", "values-fr-rFR" to "personal-strings-fr.xml")) {
            val labels = object {}.javaClass.getResourceAsStream("/nesquik-x/$file")
                ?: throw PatchException("Missing labels in Nesquik's patches: $file")
            val target = "res/$folder/strings.xml"
            val output = get(target)
            if (!output.exists()) {
                output.parentFile.mkdirs()
                output.writeText("<resources/>")
            }
            val xml = labels.bufferedReader().use { "<resources>\n${it.readText()}\n</resources>" }
            document(xml.byteInputStream()).use { source ->
                document(target).use { destination ->
                    val resources = destination.getElementsByTagName("resources").item(0)
                    val strings = source.getElementsByTagName("string")
                    for (index in 0 until strings.length) {
                        resources.appendChild(destination.importNode(strings.item(index), true))
                    }
                }
            }
        }
    }
}

fun BytecodePatchBuilder.requirePikoSettingsResources() = dependsOn(labels)

fun BytecodePatchBuilder.pikoScreen(id: String, strings: String, order: Int, fragment: String) {
    requirePikoSettingsResources()
    finalize { registerPikoScreen(id, strings, order, fragment) }
}

context(context: BytecodePatchContext)
fun registerPikoScreen(id: String, strings: String, order: Int, fragment: String) {
    registerWithPiko {
            constString(0, CONTENT)
            constString(1, id)
            constString(2, "${strings}_title")
            constString(3, "${strings}_summary")
            constString(4, "ic_vector_filter")
            constInt(5, order)
            invokeStatic(methodReference("$REGISTRY->registerCustomScreen(${TEXT.repeat(5)}I)V"), 0, 1, 2, 3, 4, 5)
            constString(0, id)
            constString(1, fragment)
            invokeStatic(methodReference("$REGISTRY->configureCustomScreen($TEXT$TEXT)V"), 0, 1)
    }
}

context(context: BytecodePatchContext)
fun registerSensitiveToggle() = registerWithPiko {
    val id = "newx.content.hide_all_sensitive_posts"
    constString(0, CONTENT)
    constString(1, id)
    constString(2, "piko_newx_hide_all_sensitive_posts_title")
    constString(3, "piko_newx_hide_all_sensitive_posts_summary")
    constInt(4, 299)
    invokeStatic(methodReference("$REGISTRY->registerToggle(${TEXT.repeat(4)}I)V"), 0, 1, 2, 3, 4)
    constString(0, id)
    constInt(1, 1)
    constInt(2, 1)
    invokeStatic(methodReference("$REGISTRY->configureToggle(${TEXT}ZZ)V"), 0, 1, 2)
}

context(context: BytecodePatchContext)
fun requirePikoTimeline() {
    if (context.classDefByOrNull(TIMELINE) == null) throw PatchException(LINK_ERROR)
}

context(context: BytecodePatchContext)
private fun registerWithPiko(emit: Block.() -> Unit) {
    val registry = context.classDefByOrNull(REGISTRY) ?: throw PatchException(LINK_ERROR)
    val method = context.mutableClassDefBy(registry.type).methods.singleOrNull {
        it.name == "load" && it.parameterTypes.isEmpty() && it.returnType == "V"
    } ?: throw PatchException(LINK_ERROR)
    val body = method.implementation ?: throw PatchException(LINK_ERROR)
    val code = body.instructions
    val guard = code.getOrNull(2) as? BuilderOffsetInstruction ?: throw PatchException(LINK_ERROR)
    val registrationEnd = guard.target.location.index
    if (code.firstOrNull()?.getReference<MethodReference>()?.toString() != "$REGISTRY->isLoaded()Z" ||
        guard.opcode != Opcode.IF_NEZ || registrationEnd <= 3 || body.registerCount < 6 ||
        code.take(registrationEnd).none {
            it.getReference<StringReference>()?.string == "newx.content.post_filtering"
        }
    ) throw PatchException(LINK_ERROR)
    // Insert before the freeze logic. The guard's jump keeps pointing to the original target.
    method.insertHook(registrationEnd, relocateBranchTargets = false) { emit() }
}
