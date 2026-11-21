package app.morphe.patches.youtube.layout.universalcaptiontranslation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.*
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

// Reconstructed from 0.3.0 bytecode, with one semantic change in 0.4.0:
// Exhausting a non-empty server list now proceeds to the fallback catalogue.
private fun Method.code() = implementation?.instructions?.toList() ?: emptyList()
private fun Method.hasString(s: String) = code().any {
    ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == s
}
private inline fun <reified T> Method.refs(): List<T> = code()
    .mapNotNull { (it as? ReferenceInstruction)?.reference as? T }.distinctBy { it.toString() }
private fun <T> List<T>.one(role: String): T = singleOrNull()
    ?: throw PatchException("Caption ABI mismatch: $role ($size matches)")

val universalCaptionTranslationPatch = bytecodePatch(
    name = "Universal auto-translated captions",
    description = "Completes partial native caption translation lists. Server availability varies by video.",
    default = false
) {
    compatibleWith("com.google.android.youtube")
    execute {
        val generatorMatch = TranslationTracksFingerprint.matchAllOrNull()?.one("translation generator")
            ?: throw PatchException("Translation generator not found")
        fun MutableClass.checkUnpatched() {
            if (methods.any { it.name.startsWith("morphe_") &&
                    (it.name.contains("Caption") || it.name.contains("Translation")) })
                throw PatchException("Caption fallback already applied. Use a clean APK and disable the older patch.")
        }
        generatorMatch.classDef.checkUnpatched()
        val menuMatch = CaptionMenuFingerprint.matchAllOrNull()?.one("caption menu")
            ?: throw PatchException("Caption menu not found")
        val generator = generatorMatch.method
        val menu = menuMatch.method
        val owner = menuMatch.classDef
        if (owner.type != generator.definingClass) throw PatchException("Caption managers differ")
        owner.checkUnpatched()
        val display = generator.refs<FieldReference>().filter { it.type == "Ljava/lang/CharSequence;" }.one("display label")
        val builderType = display.definingClass
        val builder = classDefBy(builderType)
        val build = builder.methods.filter { it.hasString("Missing required properties:") }.one("builder build")
        val trackType = build.returnType
        val factory = generator.refs<MethodReference>().filter {
            it.definingClass == trackType && it.parameterTypes.isEmpty() && it.returnType == builderType
        }.one("track factory")
        val translatedId = generator.refs<MethodReference>().filter {
            it.definingClass == trackType && it.parameterTypes.map { p -> p.toString() } ==
                listOf("Ljava/lang/String;", "Ljava/lang/String;") && it.returnType == "Ljava/lang/String;"
        }.one("translated vssId")
        val copy = builder.methods.filter { it.name == "<init>" &&
            it.parameterTypes.map { p -> p.toString() } == listOf(trackType) }.one("track-to-builder copy constructor")
        fun setter(role: String) = builder.methods.filter { it.hasString("Null $role") }.one("$role setter")
        fun trackField(role: String): FieldReference {
            val dest = setter(role).refs<FieldReference>().filter { it.definingClass == builderType }.one("$role builder field")
            val (read, _) = copy.code().zipWithNext().filter { (r, w) ->
                r.opcode == Opcode.IGET_OBJECT && w.opcode == Opcode.IPUT_OBJECT &&
                    (w as ReferenceInstruction).reference.toString() == dest.toString() &&
                    (r as TwoRegisterInstruction).registerA == (w as TwoRegisterInstruction).registerA
            }.one("$role copy dataflow")
            val field = (read as ReferenceInstruction).reference as FieldReference
            if (field.definingClass != trackType || field.type != "Ljava/lang/String;")
                throw PatchException("Invalid $role field")
            return field
        }
        val language = trackField("languageCode")
        val url = trackField("url")
        val videoId = trackField("videoId")
        val vssId = trackField("vssId")
        val kind = trackField("kind")
        val offline = menu.refs<FieldReference>().filter { it.definingClass == owner.type && it.type == "Z" }.one("offline guard")
        if (build.refs<FieldReference>().count { it.type == "Z" } != 2) throw PatchException("Unexpected boolean builder ABI")
        val setLanguage = setter("languageCode")
        val setUrl = setter("url")
        val setVideo = setter("videoId")
        val setVss = setter("vssId")
        val setId = setter("id")
        val targetCode = generator.refs<FieldReference>().filter {
            it.type == "Ljava/lang/String;" && it.definingClass != trackType && it.definingClass != owner.type
        }.one("target language code")
        val instructions = generator.code()
        val castIndex = instructions.indexOfFirst { it.opcode == Opcode.CHECK_CAST &&
            ((it as? ReferenceInstruction)?.reference as? TypeReference)?.type == targetCode.definingClass }
        if (castIndex < 0) throw PatchException("Target-language loop not found")
        val targetList = (instructions.take(castIndex).lastOrNull { it.opcode == Opcode.IGET_OBJECT }
            as? ReferenceInstruction)?.reference as? FieldReference
            ?: throw PatchException("Target-language collection not found")
        val metadata = generator.refs<FieldReference>().filter {
            it.definingClass == owner.type && it.type == targetList.definingClass
        }.one("caption metadata")
        val menuText = menu.refs<FieldReference>().filter { it.definingClass == owner.type && it.type == "Ljava/lang/String;" }
        if (menuText.size != 3) throw PatchException("Caption menu text ABI changed")
        val menuVideo = menuText[1]
        val menuTitle = menuText[2]
        fun helper(name: String, params: List<String>, registers: Int, body: String): MutableMethod =
            ImmutableMethod(owner.type, name, params.map { ImmutableMethodParameter(it, null, null) }, "V",
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value, null, null,
                MutableMethodImplementation(registers)).toMutable().apply { addInstructions(0, body.trimIndent()) }
        fun wrap(method: MutableMethod, originalName: String, after: String) {
            val original = ImmutableMethod(owner.type, originalName, emptyList(), "Ljava/util/List;",
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value, null, null, method.implementation).toMutable()
            val wrapper = ImmutableMethod(owner.type, method.name, method.parameters, method.returnType,
                method.accessFlags, method.annotations, method.hiddenApiRestrictions, MutableMethodImplementation(2)).toMutable()
            wrapper.addInstructions(0, """
                invoke-direct {p0}, ${owner.type}->$originalName()Ljava/util/List;
                move-result-object v0
                invoke-direct {p0, v0}, $after
                return-object v0
            """.trimIndent())
            owner.methods.remove(method)
            owner.methods.add(original)
            owner.methods.add(wrapper)
        }
        val helperName = "morphe_addCaptionTarget"
        val nativeMenu = "${owner.type}->morphe_originalCaptionMenu()Ljava/util/List;"
        val targetCall = "${owner.type}->$helperName(Ljava/util/List;Ljava/util/List;Ljava/lang/String;)V"
        val allCall = "${owner.type}->morphe_completeTranslations(Ljava/util/List;)V"
        val menuCall = "${owner.type}->morphe_completeCaptionMenu(Ljava/util/List;)V"
        val targetHelper = helper(helperName, listOf("Ljava/util/List;", "Ljava/util/List;", "Ljava/lang/String;"), 12, """
            invoke-virtual {p3}, Ljava/lang/String;->isEmpty()Z
            move-result v1
            if-nez v1, :done
            invoke-interface {p2}, Ljava/util/List;->iterator()Ljava/util/Iterator;
            move-result-object v0
            :existing
            invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
            move-result v1
            if-eqz v1, :sources
            invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, ${trackType}
            iget-object v4, v2, ${language}
            invoke-virtual {p3, v4}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
            move-result v1
            if-nez v1, :done
            goto :existing
            :sources
            const/4 v3, 0x0
            const-string v5, "-"
            invoke-virtual {p3, v5}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v6
            invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;
            move-result-object v0
            :scan
            invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
            move-result v1
            if-eqz v1, :build
            invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, ${trackType}
            iget-object v4, v2, ${language}
            invoke-virtual {p3, v4}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
            move-result v1
            if-nez v1, :done
            invoke-virtual {v4, v6}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
            move-result v1
            if-nez v1, :done
            iget-object v4, v2, ${url}
            invoke-virtual {v4}, Ljava/lang/String;->isEmpty()Z
            move-result v1
            if-nez v1, :scan
            const-string v5, "?"
            invoke-virtual {v4, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
            move-result v1
            if-eqz v1, :scan
            const-string v5, "tlang="
            invoke-virtual {v4, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
            move-result v1
            if-nez v1, :scan
            iget-object v4, v2, ${vssId}
            const-string v5, "t"
            invoke-virtual {v4, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
            move-result v1
            if-nez v1, :scan
            if-eqz v3, :choose
            iget-object v4, v3, ${kind}
            const-string v5, "asr"
            invoke-virtual {v5, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v1
            if-eqz v1, :scan
            iget-object v4, v2, ${kind}
            invoke-virtual {v5, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v1
            if-nez v1, :scan
            :choose
            move-object v3, v2
            goto :scan
            :build
            if-eqz v3, :done
            invoke-static {}, ${factory}
            move-result-object v0
            invoke-virtual {v0, p3}, ${setLanguage}
            const-string v2, ""
            invoke-virtual {v0, v2}, ${setId}
            iget-object v2, v3, ${videoId}
            invoke-virtual {v0, v2}, ${setVideo}
            iget-object v2, v3, ${vssId}
            invoke-static {v2, p3}, ${translatedId}
            move-result-object v2
            invoke-virtual {v0, v2}, ${setVss}
            iget-object v2, v3, ${url}
            const-string v1, "&tlang="
            invoke-virtual {v1, p3}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v1
            invoke-virtual {v2, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v2
            invoke-virtual {v0, v2}, ${setUrl}
            invoke-static {p3}, Ljava/util/Locale;->forLanguageTag(Ljava/lang/String;)Ljava/util/Locale;
            move-result-object v1
            invoke-virtual {v1}, Ljava/util/Locale;->getDisplayName()Ljava/lang/String;
            move-result-object v1
            iput-object v1, v0, ${display}
            invoke-virtual {v0}, ${build}
            move-result-object v0
            invoke-interface {p2, v0}, Ljava/util/List;->add(Ljava/lang/Object;)Z
            :done
            return-void
        """)
        val complete = helper("morphe_completeTranslations", listOf("Ljava/util/List;"), 10, """
            iget-boolean v0, p0, ${offline}
            if-nez v0, :done
            invoke-direct {p0}, ${nativeMenu}
            move-result-object v2
            invoke-interface {v2}, Ljava/util/List;->isEmpty()Z
            move-result v0
            if-nez v0, :done
            iget-object v0, p0, ${metadata}
            iget-object v0, v0, ${targetList}
            invoke-interface {v0}, Ljava/util/List;->isEmpty()Z
            move-result v1
            if-nez v1, :catalogue
            invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
            move-result-object v0
            :server_loop
            invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
            move-result v1
            if-eqz v1, :catalogue
            invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v3
            check-cast v3, ${targetCode.definingClass}
            iget-object v3, v3, ${targetCode}
            invoke-direct {p0, v2, p1, v3}, ${targetCall}
            goto :server_loop
            :catalogue
            const-string v0, "ab,aa,af,ak,sq,am,ar,hy,as,ay,az,bn,ba,eu,be,bho,bs,br,bg,my,ca,ceb,zh-Hans,zh-Hant,co,hr,cs,da,dv,nl,dz,en,eo,et,ee,fo,fj,fil,fi,fr,gaa,gl,lg,ka,de,el,gn,gu,ht,ha,haw,iw,hi,hmn,hu,is,ig,id,iu,ga,it,ja,jv,kl,kn,kk,kha,km,rw,ko,kri,ku,ky,lo,la,lv,ln,lt,lua,luo,lb,mk,mg,ms,ml,mt,gv,mi,mr,mn,mfe,ne,new,nso,no,ny,oc,or,om,os,pam,ps,fa,pl,pt,pt-PT,pa,qu,ro,rn,ru,sm,sg,sa,gd,sr,crs,sn,sd,si,sk,sl,so,st,es,su,sw,ss,sv,tg,ta,tt,te,th,bo,ti,to,ts,tn,tum,tr,tk,uk,ur,ug,uz,ve,vi,war,cy,fy,wo,xh,yi,yo,zu"
            const-string v1, ","
            invoke-virtual {v0, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;
            move-result-object v0
            const/4 v1, 0x0
            array-length v4, v0
            :catalogue_loop
            if-ge v1, v4, :done
            aget-object v3, v0, v1
            invoke-direct {p0, v2, p1, v3}, ${targetCall}
            add-int/lit8 v1, v1, 0x1
            goto :catalogue_loop
            :done
            return-void
        """)
        val completeMenu = helper("morphe_completeCaptionMenu", listOf("Ljava/util/List;"), 8, """
            iget-boolean v0, p0, ${offline}
            if-nez v0, :done
            invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;
            move-result-object v0
            :scan
            invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
            move-result v1
            if-eqz v1, :check
            invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, ${trackType}
            iget-object v2, v2, ${language}
            const-string v3, "AUTO_TRANSLATE_CAPTIONS_OPTION"
            invoke-virtual {v3, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v1
            if-nez v1, :done
            goto :scan
            :check
            invoke-virtual {p0}, ${generator}
            move-result-object v0
            invoke-interface {v0}, Ljava/util/List;->isEmpty()Z
            move-result v0
            if-nez v0, :done
            invoke-static {}, ${factory}
            move-result-object v0
            const-string v1, "AUTO_TRANSLATE_CAPTIONS_OPTION"
            invoke-virtual {v0, v1}, ${setLanguage}
            iget-object v1, p0, ${menuVideo}
            invoke-virtual {v0, v1}, ${setVideo}
            const-string v1, ""
            invoke-virtual {v0, v1}, ${setId}
            invoke-virtual {v0, v1}, ${setVss}
            invoke-virtual {v0, v1}, ${setUrl}
            iget-object v1, p0, ${menuTitle}
            iput-object v1, v0, ${display}
            invoke-virtual {v0}, ${build}
            move-result-object v0
            invoke-interface {p1, v0}, Ljava/util/List;->add(Ljava/lang/Object;)Z
            :done
            return-void
        """)
        wrap(menu, "morphe_originalCaptionMenu", menuCall)
        wrap(generator, "morphe_originalTranslations", allCall)
        owner.methods.addAll(listOf(targetHelper, complete, completeMenu))
    }
}
