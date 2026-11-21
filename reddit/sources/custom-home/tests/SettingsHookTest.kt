import app.neszi.reddit.customhome.wrapSettingsInitializer
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

fun main() {
    val original = ImmutableMethod("Ltest/Fragment;", "initialize", emptyList(), "V",
        AccessFlags.PROTECTED.value, null, null, MutableMethodImplementation(3)).toMutable()
    // Reproduce parameter clobbering: p0 is v2 and no longer holds a fragment at return.
    original.addInstructions(0, """
        const/4 p0, 0x0
        return-void
    """)
    val before = original.implementation!!.instructions.map { it.opcode }.toList()
    val (wrapperMethod, helper) = wrapSettingsInitializer(original)
    check(helper.implementation!!.registerCount == 3)
    check(helper.implementation!!.instructions.map { it.opcode }.toList() == before)
    check(helper.accessFlags and AccessFlags.PRIVATE.value != 0)
    check(wrapperMethod.accessFlags == AccessFlags.PROTECTED.value)
    val wrapper = wrapperMethod.implementation!!
    check(wrapper.registerCount == 1)
    val instructions = wrapper.instructions.toList()
    check(instructions.map { it.opcode } == listOf(Opcode.INVOKE_DIRECT_RANGE, Opcode.INVOKE_STATIC_RANGE, Opcode.RETURN_VOID))
    for (i in instructions.take(2)) {
        check((i as RegisterRangeInstruction).startRegister == 0 && i.registerCount == 1)
    }
    check(((instructions[0] as ReferenceInstruction).reference as MethodReference).name == helper.name)
    check(((instructions[1] as ReferenceInstruction).reference as MethodReference).name == "addSettings")
    println("PASS: settings wrapper preserves this even when original initializer clobbers p0; body, access flags and call order preserved")
}
