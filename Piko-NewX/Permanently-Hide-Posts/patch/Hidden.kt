package fr.nesquik.patches.hidden

import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.*

private const val O="Ljava/lang/Object;"
private const val S="Ljava/lang/String;"
private const val L="Ljava/util/List;"
private const val M="Lfr/nesquik/hidden/Menu;"
private const val E="Lcom/x/models/x5;"
private const val R="Lapp/morphe/extension/crimera/settings/SettingsRegistry;"
private const val T="Lapp/morphe/extension/newx/timeline/NewXTimelineFilter;"
private const val prereq="Select Piko 3.12.0-dev.5: Browse tweet object and Filter posts by keyword."

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
    description="Masque individuellement les posts depuis leur menu. Nécessite les composants Piko Browse tweet object et Filter posts by keyword.",
    default=false){
    compatibleWith(Compatibility(name="NewX",packageName="com.twitter.android",apkFileType=ApkFileType.APKM,
        targets=listOf(AppTarget(version="12.43.0-prod.01"))))
    availability { installer, _ ->
        if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.DISABLED
    }
    dependsOn(strings)
    extendWith("extensions/nesquik-hidden.mpe")
    finalize {
        if(classDefByOrNull(R)==null || classDefByOrNull(T)==null)throw PatchException(prereq)
        val registry=mutableClassDefBy(R).methods.single{it.name=="load"}
        val code=registry.implementation!!.instructions
        val guard=code.getOrNull(2) as? BuilderOffsetInstruction ?: throw PatchException(prereq)
        val end=guard.target.location.index
        val labels=code.take(end).mapNotNull{it.getReference<StringReference>()?.string}
        if(guard.opcode!=Opcode.IF_NEZ || "newx.content.browse_tweet_object" !in labels ||
            "newx.content.post_filtering" !in labels)throw PatchException(prereq)
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
        val handlers=mutableClassDefBy("Lapp/morphe/extension/newx/misc/NewXObjectBrowserHandler;").methods
        fun handler(name:String)=handlers.single{it.name==name}
        handler("addOption").addInstructions(0,"invoke-static {p0}, $M->add($L)$L\nmove-result-object p0")
        handler("labelFor").addInstructions(0,"invoke-static {p0,p1}, $M->label($O$O)$O\nmove-result-object p1")
        handler("usesIcon").addInstructions(0,"invoke-static {p0}, $M->icon($O)$O\nmove-result-object p0")
        val click=handler("handleOptionAction")
        require(click.implementation!!.registerCount>=3)
        click.addInstructionsWithLabels(0,"""
            invoke-static {p0,p1}, $M->handle($O$O)Z
            move-result v0
            if-eqz v0, :original
            return v0
        """.trimIndent(),ExternalLabel("original",click.implementation!!.instructions.first()))
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
        val presenter=mutableClassDefBy("Lcom/x/urt/items/post/options/v;")
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
