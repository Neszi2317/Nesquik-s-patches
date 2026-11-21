package app.neszi.askdescription

import app.morphe.patcher.*
import app.morphe.patcher.InstructionLocation.*
import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*

private const val HELPER = "Lapp/neszi/askdescription/DescriptionNative;"
private const val OFFICIAL = "Lapp/morphe/extension/youtube/patches/ask/AskFlyoutMenuPatch;"
private object WatchResponse : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"), returnType = "Ljava/util/List;",
    filters = listOf(
        literal(49399797L), opcode(Opcode.SGET_OBJECT),
        fieldAccess(opcode=Opcode.IGET_OBJECT,location=MatchAfterImmediately()),
        literal(51779735L),
        fieldAccess(opcode=Opcode.IGET_OBJECT,type="Ljava/lang/Object;",location=MatchAfterWithin(5)),
        opcode(Opcode.CHECK_CAST,location=MatchAfterImmediately()), literal(46659098L)
    )
)

@Suppress("unused")
val nativeDescription = bytecodePatch(
    name="Force native Gemini summary button (Description)",
    description="Experimental native footer persistence for the SAME video and screen configuration after YouTube has supplied it once. No custom UI. Requires official Ask flyout menu; does not enable server eligibility for unseen videos.",
    default=false
) {
    compatibleWith(Compatibility(name="YouTube",packageName="com.google.android.youtube",apkFileType=ApkFileType.APK,
        appIconColor=0xFF0033,targets=listOf(AppTarget(version="21.30.216",isExperimental=true))))
    extendWith("extensions/description-native.dex")
    finalize {
        val access=mutableClassDefByOrNull("Lapp/morphe/extension/youtube/patches/ask/AskFlyoutMenuPatch$"+"MessageAccess;")
            ?: throw PatchException("Select official Ask flyout menu; native message bridge is required.")
        if(access.methods.none { it.name=="patch_parseSameType" })throw PatchException("Unsupported official protobuf bridge")
        val footerExtensions=mutableListOf<String>()
        classDefForEach { cls ->
            if(cls.methods.any { m -> m.name=="<clinit>" && m.implementation?.instructions?.any {
                (it as? WideLiteralInstruction)?.wideLiteral==260791572L
            }==true }) footerExtensions.add(cls.type)
        }
        if(footerExtensions.size!=1)throw PatchException("Native footer extension is ambiguous or missing")
        val binders=mutableListOf<MethodReference>()
        classDefForEach { cls -> for(m in cls.methods) {
            val ins=m.implementation?.instructions?.toList()?:continue
            if(m.returnType!="V" || m.parameterTypes.size!=3 || AccessFlags.STATIC.isSet(m.accessFlags))continue
            if(ins.any { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string=="engagement-panel-timeline-view-consolidated" } &&
                ins.any { ((it as? ReferenceInstruction)?.reference as? FieldReference)?.definingClass==footerExtensions.single() })binders.add(m)
        }}
        if(binders.size!=1)throw PatchException("Expected one native engagement panel binder; got ${binders.size}")
        val ref=binders.single()
        val binder=mutableClassDefBy(ref.definingClass).methods.single { it.name==ref.name && it.parameterTypes==ref.parameterTypes }
        binder.addInstructions(0,"""
            invoke-static/range { p1 .. p1 }, $HELPER->beforeBind(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p1
            check-cast p1, ${ref.parameterTypes.first()}
        """)
        WatchResponse.let {
            it.clearMatch()
            val index=it.instructionMatches[5].index
            val single=(it.method.getInstruction<ReferenceInstruction>(index).reference as TypeReference).type
            val root=it.method.implementation!!.instructions.take(index).last { it.opcode==Opcode.INSTANCE_OF }
                .let { (it as ReferenceInstruction).reference as TypeReference }.type
            val base=generateSequence(single) { type -> classDefBy(type).superclass }.first { type ->
                classDefBy(type).methods.any { m -> m.name=="parseFrom" && m.returnType==type && AccessFlags.STATIC.isSet(m.accessFlags) }
            }
            val helper=mutableClassDefBy(HELPER)
            val old=helper.methods.single { m -> m.name=="isWatchRoot" }
            val replacement=ImmutableMethod(HELPER,old.name,old.parameters,old.returnType,old.accessFlags,old.annotations,old.hiddenApiRestrictions,MutableMethodImplementation(2)).toMutable()
            helper.methods.remove(old); helper.methods.add(replacement)
            replacement.addInstructions(0,"""
                instance-of v0, p0, $root
                return v0
            """)
            var returns=0
            for(parser in mutableClassDefBy(base).methods.filter { m -> m.name in setOf("parseFrom","parsePartialFrom") && m.returnType==base && AccessFlags.STATIC.isSet(m.accessFlags) && m.implementation!=null }) {
                val exits=parser.implementation!!.instructions.mapIndexedNotNull { i,instruction -> i.takeIf { instruction.opcode==Opcode.RETURN_OBJECT } }
                for(exit in exits.reversed()) {
                    val register=parser.getInstruction<OneRegisterInstruction>(exit).registerA
                    parser.replaceInstruction(exit,"invoke-static/range { v$register .. v$register }, $HELPER->observe(Ljava/lang/Object;)V")
                    parser.addInstruction(exit+1,"return-object v$register")
                    returns++
                }
            }
            if(returns==0)throw PatchException("No watch parser return found")
            java.util.logging.Logger.getLogger("AskDescription").info("Native footer binder=$ref; root=$root; observation returns=$returns")
        }
    }
}
