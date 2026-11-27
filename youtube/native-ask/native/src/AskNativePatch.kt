package app.neszi.asknative

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

private const val HELPER = "Lapp/neszi/asknative/AskNative;"
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
private object NativeCommand : Fingerprint(
    accessFlags=listOf(AccessFlags.PUBLIC,AccessFlags.FINAL), returnType="V", parameters=listOf("L"),
    filters=listOf(
        opcode(Opcode.NEW_INSTANCE), opcode(Opcode.INVOKE_DIRECT,location=MatchAfterImmediately()),
        newInstance(type="Ljava/util/HashMap;",location=MatchAfterWithin(5)),
        methodCall(opcode=Opcode.INVOKE_DIRECT,smali="Ljava/util/HashMap;-><init>(Ljava/util/Map;)V",location=MatchAfterWithin(5)),
        string("command_status_callback",location=MatchAfterImmediately()),
        methodCall(opcode=Opcode.INVOKE_INTERFACE,parameters=listOf("L","Ljava/util/Map;"),returnType="V")
    )
)
@Suppress("unused")
val askNativeAction = bytecodePatch(
    name="ZZ Ask native action button",
    description="Original Gemini icon captured from native eligible button; phone icon only, tablet Poser une question; portrait action bar and experimental fullscreen quick actions with separately captured native side-panel presentation. Requires official Ask flyout menu.",
    default=false
) {
    compatibleWith(Compatibility(name="YouTube",packageName="com.google.android.youtube",apkFileType=ApkFileType.APK,
        appIconColor=0xFF0033,targets=listOf(AppTarget(version="21.20.405"),AppTarget(version="21.30.216",isExperimental=true),AppTarget(version="21.42.241",isExperimental=true),AppTarget(version="21.44.528",isExperimental=true),AppTarget(version="21.47.483",isExperimental=true))))
    extendWith("extensions/ask-native.dex")
    // Finalize runs after all official patches execute. No cross-bundle dependencies.
    finalize {
        val official = runCatching { mutableClassDefBy(OFFICIAL) }.getOrElse {
            throw PatchException("Select official Ask flyout menu together with this add-on.")
        }
        if (official.methods.none { it.name == "openAskPanel" }) throw PatchException("Official Ask opening bridge missing")
        // Official addMenuItem is called at feed/history sites only. Hook the resolved
        // native presentation methods themselves so watch-page callers are covered too.
        val menuTargets = linkedMapOf<String, MethodReference>()
        classDefForEach { cls ->
            for (method in cls.methods) {
                val ins = method.implementation?.instructions?.toList() ?: continue
                for (i in 0 until ins.size - 3) {
                    val ref = (ins[i] as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.definingClass != OFFICIAL || ref.name != "addMenuItem") continue
                    if (ins[i+1].opcode != Opcode.MOVE_RESULT_OBJECT || ins[i+2].opcode != Opcode.CHECK_CAST)
                        throw PatchException("Unsupported official menu bridge shape")
                    val menuType = ((ins[i+2] as ReferenceInstruction).reference as TypeReference).type
                    val target = (ins[i+3] as? ReferenceInstruction)?.reference as? MethodReference
                        ?: throw PatchException("Native menu presentation target missing")
                    if (target.returnType != "V" || target.parameterTypes.firstOrNull().toString() != menuType ||
                        ins[i+3].opcode !in setOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE))
                        throw PatchException("Unsupported native menu presentation signature")
                    menuTargets[target.toString()] = target
                }
            }
        }
        if(menuTargets.size != 2) throw PatchException("Expected popup and bottom-sheet native targets; got ${menuTargets.size}")
        for (target in menuTargets.values) {
            val method = mutableClassDefBy(target.definingClass).methods.single {
                it.name == target.name && it.parameterTypes == target.parameterTypes && it.returnType == target.returnType
            }
            if(method.implementation == null || AccessFlags.STATIC.isSet(method.accessFlags))
                throw PatchException("Native menu target has no instance implementation")
            method.addInstructions(0,"""
                invoke-static/range { p0 .. p1 }, $HELPER->onNativeMenuOpening(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object p1
                check-cast p1, ${target.parameterTypes.first()}
            """)
            java.util.logging.Logger.getLogger("AskNative").info("0.8.1 native watch menu entry: $target")
        }
        val rowCreated=official.methods.single { it.name=="onMenuRowCreated" }
        rowCreated.addInstruction(0,"invoke-static/range { p0 .. p1 }, $HELPER->onNativeRowCreated(Ljava/lang/Object;Ljava/lang/Object;)V")
        val popup=official.methods.single { it.name=="onPopupMenuShown" }
        popup.addInstruction(0,"invoke-static/range { p0 .. p0 }, $HELPER->onNativePopupShown(Ljava/lang/Object;)V")
        val menuTap=official.methods.single { it.name=="onMenuItemTapped" && it.parameterTypes==listOf("Ljava/lang/Object;") }
        if(menuTap.implementation!!.registerCount<2)throw PatchException("No scratch register for native menu handler")
        menuTap.addInstructions(0,"""
            invoke-static/range { p0 .. p0 }, $HELPER->onNativeMenuTap(Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :original_menu
            return v0
            :original_menu
            nop
        """)
        // Minimal is also constructed locally after the watch root is parsed. Fix every
        // return of that exact official converter, including its DEFAULT/no-op branches.
        val layoutClass=mutableClassDefByOrNull("Lapp/morphe/extension/youtube/patches/RestoreOldVideoActionBarPatch;")
        val layout=layoutClass?.methods?.singleOrNull { it.name=="setVideoActionBarLayout" && it.parameterTypes==listOf("[B") && it.returnType=="[B" }
        if(layout!=null) {
            val exits=layout.implementation!!.instructions.mapIndexedNotNull { i, instruction -> i.takeIf { instruction.opcode==Opcode.RETURN_OBJECT } }
            for(exit in exits.reversed()) {
                val reg=layout.getInstruction<OneRegisterInstruction>(exit).registerA
                layout.replaceInstruction(exit,"invoke-static/range { v$reg .. v$reg }, $HELPER->afterActionBarLayout([B)[B")
                layout.addInstructions(exit+1,"""
                    move-result-object v$reg
                    return-object v$reg
                """)
            }
            layout.addInstruction(0,"invoke-static/range { p0 .. p0 }, $HELPER->beforeActionBarLayout([B)V")
            if(exits.isEmpty())throw PatchException("Native action layout converter has no return")
            java.util.logging.Logger.getLogger("AskNative").info("0.8.2 post-layout hooks: ${exits.size}")
        }
        val show = official.methods.single { it.name == "showAskPanel" }
        val instructions = show.implementation!!.instructions.toList()
        val before = instructions.indexOfFirst {
            it.opcode == Opcode.SPUT_BOOLEAN && (it as? ReferenceInstruction)?.reference.toString().contains("->isExecutingOwnCommand:")
        }
        if(before < 0) throw PatchException("Unsupported official Ask show method; refusing unsafe summary override")
        show.addInstruction(before,"invoke-static {}, $HELPER->beforeShow()V")
        // Capture String at method ENTRY: R8 reuses p0 as Panel after build().
        show.addInstruction(0,"invoke-static/range { p0 .. p0 }, $HELPER->beginShow(Ljava/lang/String;)V")

        val panelExecute=show.implementation!!.instructions.indexOfFirst {
            it.opcode==Opcode.INVOKE_INTERFACE && (it as? ReferenceInstruction)?.reference.toString().contains("->patch_executeCommand([B)V")
        }
        if(panelExecute<0) throw PatchException("Official panel command executor call missing")
        val panelBytes=show.getInstruction<FiveRegisterInstruction>(panelExecute).registerD
        show.addInstructions(panelExecute,"""
            invoke-static/range { v$panelBytes .. v$panelBytes }, $HELPER->adaptPanelCommand([B)[B
            move-result-object v$panelBytes
        """)

        WatchResponse.let {
            it.clearMatch()
            val index=it.instructionMatches[5].index
            val single=(it.method.getInstruction<ReferenceInstruction>(index).reference as TypeReference).type
            val register=it.method.getInstruction<OneRegisterInstruction>(index).registerA
            val root=it.method.implementation!!.instructions.take(index).last {
                it.opcode == Opcode.INSTANCE_OF
            }.let { (it as ReferenceInstruction).reference as TypeReference }.type
            val base=generateSequence(single) { type -> classDefBy(type).superclass }.first { type ->
                classDefBy(type).methods.any { m -> m.name == "parseFrom" && m.returnType == type &&
                    AccessFlags.STATIC.isSet(m.accessFlags) }
            }
            val helperClass=mutableClassDefBy(HELPER)
            val oldClassify=helperClass.methods.single { m -> m.name == "messageKind" }
            val classify=ImmutableMethod(HELPER,oldClassify.name,oldClassify.parameters,oldClassify.returnType,oldClassify.accessFlags,oldClassify.annotations,oldClassify.hiddenApiRestrictions,MutableMethodImplementation(2)).toMutable()
            helperClass.methods.remove(oldClassify)
            helperClass.methods.add(classify)
            classify.addInstructions(0,"""
                instance-of v0, p0, $root
                if-eqz v0, :check_single
                const/4 v0, 0x1
                return v0
                :check_single
                instance-of v0, p0, $single
                if-eqz v0, :other
                const/4 v0, 0x2
                return v0
                :other
                const/4 v0, 0x0
                return v0
            """)
            val parsers=mutableClassDefBy(base).methods.filter { m ->
                m.name in setOf("parseFrom", "parsePartialFrom") && m.returnType == base &&
                AccessFlags.STATIC.isSet(m.accessFlags) && m.implementation != null
            }
            var returns=0
            for (parser in parsers) {
                val exits=parser.implementation!!.instructions.mapIndexedNotNull { i, instruction ->
                    i.takeIf { instruction.opcode == Opcode.RETURN_OBJECT }
                }
                for (exit in exits.reversed()) {
                    val reg=parser.getInstruction<OneRegisterInstruction>(exit).registerA
                    // Replace the RETURN location itself: branches targeting that return must also run the hook.
                    parser.replaceInstruction(exit,"invoke-static/range { v$reg .. v$reg }, $HELPER->transformParsed(Ljava/lang/Object;)Ljava/lang/Object;")
                    parser.addInstructions(exit+1,"""
                        move-result-object v$reg
                        check-cast v$reg, $base
                        return-object v$reg
                    """)
                    returns++
                }
            }
            if(returns==0) throw PatchException("No decoded protobuf return hook found; refusing inactive patch")
            java.util.logging.Logger.getLogger("AskNative").info("0.5 decoded-root hooks: $returns in ${parsers.size} methods; root=$root single=$single")
            // The old local related-results copy is OBSERVATION ONLY, never the insertion point.
            it.method.addInstruction(index+1,"invoke-static/range { v$register .. v$register }, $HELPER->observeWatch(Ljava/lang/Object;)V")
        }
        NativeCommand.let {
            it.clearMatch()
            val index=it.instructionMatches.last().index
            val call=it.method.getInstruction<FiveRegisterInstruction>(index)
            val executor=call.registerC
            val command=call.registerD
            val type=(it.method.getInstruction<ReferenceInstruction>(index).reference as MethodReference).parameterTypes.first()
            // Replaces only own command, preserves Map and native callback registers.
            it.method.addInstructions(index,"""
                invoke-static { v$command, v$executor }, $HELPER->route(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$command
                check-cast v$command, $type
            """)
        }
    }
}
