package fr.nesquik.patches.hidden

import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.*

private const val O="Ljava/lang/Object;"
private const val S="Ljava/lang/String;"
private const val L="Ljava/util/List;"
private const val M="Lfr/nesquik/hidden/Menu;"
private const val E="Lcom/x/models/x5;"
private const val R="Lapp/morphe/extension/crimera/settings/SettingsRegistry;"
private const val T="Lapp/morphe/extension/newx/timeline/NewXTimelineFilter;"
private const val STATE = "Lcom/x/urt/items/post/options/j0;"
private const val PRESENTER = "Lcom/x/urt/items/post/options/v;"
private const val EVENT = "Lcom/x/urt/items/post/options/o;"
private const val RENDERER = "Landroidx/compose/material/d0;"
private const val FILTER_REQUIRED = "Select Piko's NewX: Filter posts by keyword with NewX: Permanently Hide Posts."

private fun menuSite(matches: List<Int>, name: String): Int = matches.singleOrNull()
    ?: throw PatchException("Expected one NewX post-menu $name site for X 12.43.0-prod.01, found ${matches.size}.")

private fun installMenuHooks() {
    val renderer = mutableClassDefBy(RENDERER).methods.single {
        it.name == "invoke" && it.parameterTypes.map(CharSequence::toString) == listOf(O, O, O)
    }
    val renderCode = renderer.implementation!!.instructions
    // Work on the rendered options, after both the stock menu and Piko have supplied their rows.
    val listRead = menuSite(renderCode.indices.filter { index ->
        renderCode[index].opcode == Opcode.IGET_OBJECT &&
            renderCode[index].getReference<FieldReference>()?.toString() == "$STATE->c:$L" &&
            renderCode.getOrNull(index + 2)?.getReference<MethodReference>()?.toString() ==
            "Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;"
    }, "options list")
    val list = (renderCode[listRead] as TwoRegisterInstruction).registerA
    val lookup = menuSite(renderCode.indices.filter { index ->
        renderCode[index].getReference<FieldReference>()?.toString() ==
            "$STATE->d:Lkotlin/collections/builders/MapBuilder;" &&
            renderCode.getOrNull(index + 1)?.getReference<MethodReference>()?.toString() ==
            "Lkotlin/collections/builders/MapBuilder;->get($O)$O" &&
            renderCode.getOrNull(index + 2)?.opcode == Opcode.MOVE_RESULT_OBJECT
    }, "label lookup")
    val action = (renderCode[lookup + 1] as FiveRegisterInstruction).registerD
    val label = (renderCode[lookup + 2] as OneRegisterInstruction).registerA
    val rowStart = menuSite(renderCode.indices.filter { index ->
        val mask = renderCode[index] as? NarrowLiteralInstruction
        val actionMove = renderCode.getOrNull(index + 3) as? TwoRegisterInstruction
        val row = renderCode.subList(index + 1, minOf(renderCode.size, index + 33))
            .firstOrNull { it.getReference<MethodReference>()?.toString() == ROW_CONSTRUCTOR }
        renderCode[index].opcode == Opcode.CONST_16 && mask?.narrowLiteral == 120 &&
            (mask as? OneRegisterInstruction)?.registerA == 28 && actionMove != null &&
            actionMove.opcode == Opcode.MOVE_OBJECT_FROM16 && actionMove.registerA == 23 &&
            actionMove.registerB == action && (row as? RegisterRangeInstruction)?.startRegister == 22
    }, "icon row")
    // The mask is initialized immediately before building the row. Restore it before continuing.
    renderer.addInstructionsWithLabels(rowStart + 1, """
        invoke-static/range {v$action .. v$action}, $M->usesIcon($O)Z
        move-result v28
        if-eqz v28, :restore_mask
        sget-object v24, Lcom/x/icons/a;->Y3:Lcom/x/icons/b;
        :restore_mask
        const/16 v28, 120
    """.trimIndent())
    renderer.addInstructions(lookup + 3, """
        invoke-static {v$action,v$label}, $M->label($O$O)$O
        move-result-object v$label
    """.trimIndent())
    renderer.addInstructions(listRead + 1, """
        invoke-static/range {v$list .. v$list}, $M->add($L)$L
        move-result-object v$list
    """.trimIndent())

    val event = mutableClassDefBy(EVENT).methods.single { it.name == "invoke" }
    val eventCode = event.implementation!!.instructions
    val presenter = menuSite(eventCode.indices.filter { index ->
        eventCode[index].getReference<FieldReference>()?.toString() == "$EVENT->f:$PRESENTER" &&
            eventCode.getOrNull(index + 1)?.getReference<FieldReference>()?.toString() ==
            "$PRESENTER->k:Landroid/content/Context;"
    }, "presenter load")
    val presenterRegister = (eventCode[presenter] as TwoRegisterInstruction).registerA
    val dispatch = menuSite(eventCode.indices.filter { index ->
        eventCode[index].getReference<MethodReference>()?.toString() ==
            "Lcom/x/urt/items/post/options/e0;->a()$E" &&
            eventCode.getOrNull(index + 1)?.opcode == Opcode.MOVE_RESULT_OBJECT &&
            eventCode.getOrNull(index + 2)?.getReference<FieldReference>()?.toString() ==
            "Lcom/x/urt/items/post/options/e;->a:[I"
    }, "confirmed action") + 2
    val selected = (eventCode[dispatch - 1] as OneRegisterInstruction).registerA
    // v14 is dead here in this APK. X writes the ordinal there immediately after loading its switch table.
    val ordinal = menuSite(eventCode.indices.filter { index ->
        eventCode[index].getReference<MethodReference>()?.toString() == "Ljava/lang/Enum;->ordinal()I" &&
            (eventCode[index] as? FiveRegisterInstruction)?.registerC == selected &&
            (eventCode.getOrNull(index + 1) as? OneRegisterInstruction)?.let {
                it.opcode == Opcode.MOVE_RESULT && it.registerA == 14
            } == true
    }, "action ordinal")
    if (ordinal < dispatch) throw PatchException("NewX post-menu action dispatch changed.")
    event.addInstructionsWithLabels(dispatch, """
        invoke-static {v$presenterRegister,v$selected}, $M->handle($O$O)Z
        move-result v14
        if-eqz v14, :original
        sget-object v14, Lkotlin/Unit;->a:Lkotlin/Unit;
        return-object v14
    """.trimIndent(), ExternalLabel("original", eventCode[dispatch]))
}

private const val ROW_CONSTRUCTOR =
    "Lcom/x/ui/common/sheets/a;-><init>(Ljava/lang/Object;Lcom/x/icons/b;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;I)V"

private val strings=resourcePatch(default=false){
    execute {
        document("res/values/strings.xml").use { doc ->
            for ((key,value) in mapOf("title" to "Permanently Hidden Posts","summary" to "Gérer les publications masquées sur cet appareil")) {
                val el=doc.createElement("string")
                el.setAttribute("name","nesquik_permanent_posts_"+key)
                el.textContent=value
                doc.documentElement.appendChild(el)
            }
        }
    }
}
@Suppress("unused")
val permanentlyHidePosts=bytecodePatch(name="NewX: Permanently Hide Posts",
    description="Masque individuellement les posts depuis leur menu. Nécessite le filtre par mots-clés de Piko.",
    default=false){
    compatibleWith(Compatibility(name="NewX",packageName="com.twitter.android",apkFileType=ApkFileType.APKM,
        targets=listOf(AppTarget(version="12.43.0-prod.01"))))
    availability { installer, _ ->
        if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.DISABLED
    }
    dependsOn(strings)
    extendWith("extensions/nesquik-hidden.mpe")
    finalize {
        if (classDefByOrNull(R) == null || classDefByOrNull(T) == null) throw PatchException(FILTER_REQUIRED)
        val registry = mutableClassDefBy(R).methods.single { it.name == "load" && it.parameterTypes.isEmpty() }
        val body = registry.implementation ?: throw PatchException("Missing Piko settings registry implementation.")
        val code = body.instructions
        val guard = code.getOrNull(2) as? BuilderOffsetInstruction
        if (guard == null || guard.opcode != Opcode.IF_NEZ || body.registerCount < 6 ||
            code.firstOrNull()?.getReference<MethodReference>()?.toString() != "$R->isLoaded()Z"
        ) throw PatchException("Unsupported Piko settings registry layout.")
        val end = guard.target.location.index
        if (code.take(end).none { it.getReference<StringReference>()?.string == "newx.content.post_filtering" }) {
            throw PatchException(FILTER_REQUIRED)
        }
        registry.addInstructions(end,"""
            const-string v0, "newx.content"
            const-string v1, "newx.content.nesquik_permanent_posts"
            const-string v2, "nesquik_permanent_posts_title"
            const-string v3, "nesquik_permanent_posts_summary"
            const-string v4, "ic_vector_filter"
            const/16 v5, 0x1b8
            invoke-static/range {v0 .. v5}, $R->registerCustomScreen(${S.repeat(5)}I)V
            const-string v0, "newx.content.nesquik_permanent_posts"
            const-string v1, "Lfr/nesquik/hidden/Settings;"
            invoke-static {v0,v1}, $R->configureCustomScreen($S$S)V
        """.trimIndent())
        val enum=mutableClassDefBy(E)
        val init=enum.methods.single{it.name=="<clinit>"}
        val enumCode=init.implementation!!.instructions
        require(enumCode.last().opcode==Opcode.RETURN_VOID && enumCode[1].getReference<StringReference>()?.string=="None")
        init.addInstructions(enumCode.lastIndex,"""
            new-instance v0, $E
            const-string v1, "NesquikPermanentHideLocal"
            const/4 v2, 0x0
            invoke-direct {v0,v1,v2}, $E-><init>(${S}I)V
            sput-object v0, $M->action:$O
        """.trimIndent())
        installMenuHooks()
        // Validate and expose only fields and methods referenced by the compiled extension.
        for(type in listOf("Projection","Menu","Store")){
            for(method in mutableClassDefBy("Lfr/nesquik/hidden/$type;").methods){
                for(ins in method.implementation?.instructions.orEmpty()){
                    val f=ins.getReference<FieldReference>()
                    if(f!=null && f.definingClass.startsWith("Lcom/x/")){
                        val field=mutableClassDefBy(f.definingClass).fields.single{it.toString()==f.toString()}
                        field.accessFlags=(field.accessFlags and 6.inv()) or 1
                    }
                    val ref=ins.getReference<MethodReference>() ?: continue
                    if(ref.definingClass==T || ref.definingClass.startsWith("Landroidx/compose/runtime/") || ref.definingClass.startsWith("Lcom/x/")){
                        val target=mutableClassDefBy(ref.definingClass).methods.single{it.toString()==ref.toString()}
                        if(ref.definingClass==T)target.accessFlags=(target.accessFlags and 6.inv()) or AccessFlags.PUBLIC.value
                    }
                }
            }
        }
        val presenter=classDefBy(PRESENTER)
        require(presenter.fields.count{it.type=="Lcom/x/models/timelines/items/k1;"}==1)
        val surfaces=listOf("Lcom/x/urt/ui/b;","Lcom/x/postdetailsheet/r;","Lcom/x/postdetailsheet/k;",
            "Lcom/x/video/tab/g9;","Lcom/x/video/tab/k7;","Lcom/x/video/tab/z7;","Lcom/x/urt/ui/t0;",
            "Lcom/x/urt/ui/module/s;","Lcom/google/android/gms/internal/mlkit_vision_common/d9;")
        for(type in surfaces){
            var sites=0
            for(method in mutableClassDefBy(type).methods){
                val instructions=method.implementation?.instructions?.toList() ?: continue
                for((index,ins) in instructions.withIndex().reversed()){
                    if(ins.opcode!=Opcode.IGET_OBJECT || ins.getReference<FieldReference>()?.toString()!="Lcom/x/urt/e2;->b:Lkotlinx/collections/immutable/b;")continue
                    val reg=(ins as TwoRegisterInstruction).registerA
                    method.addInstructions(index+1,"""
                        invoke-static/range {v$reg .. v$reg}, Lfr/nesquik/hidden/Projection;->render($O)$O
                        move-result-object v$reg
                        check-cast v$reg, Lkotlinx/collections/immutable/b;
                    """.trimIndent())
                    sites++
                }
            }
            if(sites==0)throw PatchException("Missing UI list read: $type")
            println("Permanent hide: $type: $sites list reads")
        }
    }
}
