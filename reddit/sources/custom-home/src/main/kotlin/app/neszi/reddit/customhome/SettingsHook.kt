package app.neszi.reddit.customhome

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * DEX parameter registers may be reused by the original method's body.
 * Preserve `this` in a fresh wrapper frame rather than reading p0 at its return.
 */
fun wrapSettingsInitializer(method: MutableMethod): Pair<MutableMethod, MutableMethod> {
    if (method.parameterTypes.isNotEmpty() || method.returnType != "V" ||
        method.accessFlags and AccessFlags.STATIC.value != 0)
        throw PatchException("Unsupported settings initializer signature")
    val helper = ImmutableMethod(
        method.definingClass, "neszi_customHomeOriginalInitialize", emptyList(), "V",
        AccessFlags.PRIVATE.value, null, null,
        method.implementation ?: throw PatchException("Settings initializer has no body")
    ).toMutable()
    val wrapper = ImmutableMethod(
        method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions,
        MutableMethodImplementation(1)
    ).toMutable()
    wrapper.addInstructions(0, """
        invoke-direct/range { p0 .. p0 }, ${helper.definingClass}->${helper.name}()V
        invoke-static/range { p0 .. p0 }, Lapp/neszi/reddit/customhome/CustomHomeSettings;->addSettings(Landroid/preference/PreferenceFragment;)V
        return-void
    """)
    return wrapper to helper
}
