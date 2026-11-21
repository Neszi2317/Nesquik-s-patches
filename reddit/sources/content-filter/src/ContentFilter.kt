package local.reddit.patches

import java.io.File
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*

private const val R="Llocal/reddit/filter/RedditContentFilter;"
private const val LINK="Lcom/reddit/domain/model/Link;"
private const val LIST="Ljava/util/List;"
private fun Method.code()=implementation?.instructions?.toList().orEmpty()
private fun Instruction.ref()=(this as? ReferenceInstruction)?.reference
private fun Method.calls()=code().mapNotNull{it.ref() as? MethodReference}
private fun Method.params()=parameterTypes.map{it.toString()}
private fun <T> List<T>.one(label:String):T=singleOrNull()?:throw PatchException("Content filter: expected one $label, got $size")
private fun check(ok:Boolean,why:String){if(!ok)throw PatchException("Content filter 2026.47.0: $why")}
private fun MutableMethod.filterReturns(call:String){
    val all=code()
    for(i in all.indices.reversed())if(all[i].opcode==Opcode.RETURN_OBJECT){
        val r=(all[i] as OneRegisterInstruction).registerA
        addInstructions(i,"invoke-static/range { v$r .. v$r }, $R->$call($LIST)$LIST\nmove-result-object v$r")
    }
}

private fun readRules(entries:List<String>?,path:String?,subreddits:Boolean):String {
    val fromFile=if(path.isNullOrBlank()) "" else {
        val f=File(path)
        check(f.isFile&&f.length()<=8_000_000,"TXT file missing or larger than 8 MB")
        StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(f.readBytes())).toString()
    }
    val input=(entries.orEmpty()+fromFile).joinToString("\n").replace("\uFEFF","")
    check(input.length<=2_000_000,"list larger than 2 million characters")
    val unique=linkedMapOf<String,String>()
    for(raw in input.split(Regex("\\r\\n|\\r|\\n"))) {
        var value=raw.trim();if(value.isEmpty())continue
        if(subreddits){
            value=value.lowercase(Locale.ROOT).removePrefix("/r/").removePrefix("r/").removeSuffix("/")
            check(value.matches(Regex("[a-z0-9_]+")),"invalid subreddit name: $value")
        }
        val key=value.map{Character.toLowerCase(Character.toUpperCase(it))}.joinToString("")
        unique.putIfAbsent(key,value)
        check(unique.size<=50000,"more than 50000 rules")
    }
    return unique.values.joinToString("\n")
}
private fun smaliString(s:String)=s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")
private fun MutableMethod.embedText(text:String){
    check(implementation!!.registerCount>=2,"config registers missing")
    removeInstructions(code().size)
    addInstructions(0,buildString {
        append("new-instance v0, Ljava/lang/StringBuilder;\ninvoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V\n")
        for(part in text.chunked(8000))append("const-string v1, \"${smaliString(part)}\"\ninvoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n")
        append("invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;\nmove-result-object v0\nreturn-object v0")
    })
}

val contentFiltersPatch=bytecodePatch(
    name="Reddit - Local keyword filters",
    description="Local literal keyword lists and TXT import. Configure keyword/subreddit lists and TXT imports before patching. Filters posts (including flair), comments, search communities and typeahead suggestions. Reddit 2026.47.0.",
    default=false
){
    val blockedKeywords by stringsOption(key="blockedKeywords",default=emptyList(),title="Blocked keywords",
        description="Mots et expressions littéraux. Ajoutez les entrées avec + ; collage groupé séparé par virgules, ou import TXT ci-dessous (une règle par ligne). Casse ignorée. Les fichiers et les entrées sont fusionnés.")
    val blockedSubreddits by stringsOption(key="blockedSubreddits",default=emptyList(),title="Blocked subreddits",
        description="Noms exacts : AskReddit, r/AskReddit ou /r/AskReddit. Ajoutez avec +, séparés par virgules, ou utilisez le TXT ci-dessous. Casse ignorée.")
    val keywordsFile=filePathOption(key="keywordsFile",title="Blocked keywords — TXT",description="Fichier UTF-8 local, une expression par ligne. Fusionné avec Blocked keywords.",allowedExtensions=listOf("txt"))
    val subredditsFile=filePathOption(key="subredditsFile",title="Blocked subreddits — TXT",description="Fichier UTF-8 local, un subreddit par ligne. Fusionné avec Blocked subreddits.",allowedExtensions=listOf("txt"))
    compatibleWith(Compatibility(packageName="com.reddit.frontpage", name="Reddit", targets=listOf(AppTarget(version="2026.47.0", isExperimental=true))))
    extendWith("extensions/reddit-keywords.dex")
    execute {
        check(packageMetadata.packageName=="com.reddit.frontpage"&&packageMetadata.versionName=="2026.47.0","wrong target version")
        val keywords=readRules(blockedKeywords,keywordsFile.value,false)
        val subreddits=readRules(blockedSubreddits,subredditsFile.value,true)
        val config=mutableClassDefBy("Llocal/reddit/filter/BundledConfig;")
        config.methods.filter{it.name=="keywords"}.one("keyword defaults").embedText(keywords)
        config.methods.filter{it.name=="subreddits"}.one("subreddit defaults").embedText(subreddits)
        println("Content filters: embedded Manager configuration (local only)")
        // Cross-check exact runtime paths against semantic toString signatures and field types.
        fun validate(type:String,marker:String,fields:Map<String,String>){
            val c=classDefBy(type)
            check(c.methods.any{m->m.code().any{(it.ref() as? StringReference)?.string==marker}},"$type semantic marker missing")
            for((name,kind)in fields)check(c.fields.any{it.name==name&&it.type==kind},"$type.$name changed")
        }
        validate("Lqs40;","PostElement(linkId=",mapOf("e" to "Ljava/lang/String;","f" to "Lkpq;"))
        validate("Ly39;","ClassicPostElement(linkId=",mapOf("q" to "Ljava/lang/String;","k" to "Ljava/lang/String;","t" to "Lpw40;"))
        validate("Lpw40;","PostFlairsElement(linkId=",mapOf("i" to "Lkpq;"))
        validate("Lmu40;","PostFlairFeedPresentationModel(text=",mapOf("a" to "Ljava/lang/String;","d" to "Ljava/lang/String;"))
        check(classDefBy(LINK).methods.any{it.name=="getLinkFlairText"&&it.returnType=="Ljava/lang/String;"},"Link.getLinkFlairText missing")
        validate("Lghw;","MetadataHeaderElement(linkId=",mapOf("m" to "Ljava/lang/String;","n" to "Ljava/lang/String;"))
        validate("Lg560;","PostTitleElement(linkId=",mapOf("i" to "Ljava/lang/String;"))
        validate("Lvdd0;","RichTextElement(linkId=",mapOf("i" to "Ljava/lang/String;"))
        validate("Lvof0;","SearchFeedPostElement(feedPostElement=",mapOf("e" to "Lqs40;"))
        validate("Lopa;","Comments(changes=",mapOf("b" to "Ljava/util/Map;","c" to LIST,"g" to "Ljava/util/Map;"))
        validate("Lkk50;","PostPreviewFeedElement(linkId=",mapOf("l" to "Lghw;","A" to LINK,"n" to "Lg560;","o" to "Lw270;","p" to "Lp9m0;","q" to "Lpw40;","s" to "Ly39;"))
        validate("Lvmt;","LinearPostCardElement(linkId=",mapOf("k" to "Lghw;","j" to "Lg560;","n" to "Lumt;"))
        validate("Lmyl0;","TheaterPostCardElement(linkId=",mapOf("k" to "Lghw;","j" to "Lg560;"))
        validate("Lp9m0;","TitleWithThumbnailElement(linkId=",mapOf("i" to "Lg560;","j" to "Lw270;"))
        validate("Lm9m0;","TitleWithThumbnailCollapsedElement(linkId=",mapOf("i" to "Lg560;"))
        validate("Lb8m0;","TitleWithLinkedPreviewElement(linkId=",mapOf("i" to "Lg560;","j" to "Lw270;"))
        validate("Lw270;","PreviewTextElement(linkId=",mapOf("i" to "Ljava/lang/String;"))
        validate("Lumt;","Content(text=",mapOf("a" to "Ltmt;"))
        check(classDefBy(LINK).methods.any{it.name=="getSubreddit"&&it.returnType=="Ljava/lang/String;"},"Link.getSubreddit missing")
        validate("Lvmf0;","SearchDynamicPostElement(post=",mapOf("e" to "Lhag0;"))
        validate("Lhag0;","SearchPostInfo(id=",mapOf("s" to "Ljava/lang/String;","e" to "Ljava/lang/String;","O" to "Ljava/lang/String;","P" to "Ljava/lang/String;"))
        validate("Lskf0;","SearchDynamicCommunityElement(id=",mapOf("f" to "Lhbf0;"))
        validate("Lhbf0;","SearchCommunity(subredditId=",mapOf("k" to "Ljava/lang/String;"))
        validate("La1h0;","SearchTypeaheadSuggestion(id=",mapOf("b" to "Lv0h0;"))
        validate("Lb1h0;","SearchTypeaheadSuggestionFeedElement(identifier=",mapOf("k" to "Lv0h0;"))
        validate("Lv0h0;","Behaviors(default=",mapOf("a" to "Lu0h0;"))
        validate("Lw0h0;","CommunityNavigationBehavior(id=",mapOf("b" to "Ljava/lang/String;"))
        validate("Lmzg0;","SearchTypeaheadRowCommunityItem(id=",mapOf("b" to "Lozg0;"))
        validate("Lozg0;","SearchTypeaheadRowCommunityItemBehaviors(default=",mapOf("a" to "Lw86;"))
        validate("Lw86;","SearchCommunityNavigationBehavior(id=",mapOf("b" to "Ljava/lang/String;"))
        validate("Lztg0;","SearchTypeaheadList(id=",mapOf("b" to LIST))
        validate("Llzg0;","SearchTypeaheadRow(id=",mapOf("c" to "Ljava/util/ArrayList;"))
        // Filter the two typed child collections where native typeahead becomes feed data.
        // No mutation of server/cache models and no interception of generic List/Compose APIs.
        val typeahead=mutableClassDefBy("Lcom/reddit/search/combined/data/a;").methods.filter{
            it.calls().any{r->r.definingClass=="Lpzg0;"&&r.name=="<init>"}
        }.one("dynamic typeahead mapper")
        val reads=typeahead.code().mapIndexedNotNull { index, ins ->
            val f=ins.ref() as? FieldReference
            if(ins.opcode==Opcode.IGET_OBJECT && (f?.toString()=="Lztg0;->b:$LIST" || f?.toString()=="Llzg0;->c:Ljava/util/ArrayList;"))Triple(index,(ins as OneRegisterInstruction).registerA,f!!.type) else null
        }
        check(reads.size==2,"typeahead child list paths changed")
        for((index,reg,kind) in reads.reversed())typeahead.addInstructions(index+1,
            "invoke-static/range {v$reg .. v$reg}, $R->filterSearchItems($LIST)$LIST\nmove-result-object v$reg"+
            if(kind!=LIST)"\ncheck-cast v$reg, $kind" else "")
        val comments=classDefBy("Lcom/reddit/domain/model/Comment;")
        check(comments.methods.any{it.name=="getBody"&&it.returnType=="Ljava/lang/String;"},"Comment.getBody missing")
        check(classDefBy("Lopa;").methods.count{it.name=="<init>"&&it.params().size==10}==1,"comment projection constructor changed")
        // Same Listing.getChildren path as Santodan; no constructor mutation or cache deletion.
        mutableClassDefBy("Lcom/reddit/domain/model/listing/Listing;").methods.filter{it.name=="getChildren"&&it.params().isEmpty()&&it.returnType==LIST}.one("Listing getter").filterReturns("filterListing")
        // Preserve Link text registration from Santodan, but bounded and entirely offline.
        val ctor=mutableClassDefBy(LINK).methods.filter{m->m.name=="<init>"&&m.code().any{val f=it.ref() as? FieldReference;it.opcode==Opcode.IPUT_BOOLEAN&&f?.name=="isBlankAd"&&f.definingClass==LINK}}.one("Link constructor")
        val code=ctor.code()
        for(i in code.indices.reversed())if(code[i].opcode==Opcode.RETURN_VOID)ctor.addInstructions(i,"invoke-static/range { p0 .. p0 }, $R->rememberLink(Ljava/lang/Object;)V")
        // Refresh Link text when actually mapped, after deserialization has populated mutable fields.
        // Both static mappers keep all original instructions and coroutine behavior.
        val linkMapper=mutableClassDefBy("Lcom/reddit/feeds/impl/data/mapper/link/a;").methods.filter{
            it.name=="a"&&it.params().firstOrNull()==LINK&&it.calls().any{r->r.definingClass=="Lqs40;"&&r.name=="<init>"}
        }.one("card Link mapper")
        check(AccessFlags.STATIC.isSet(linkMapper.accessFlags),"card mapper no longer static")
        linkMapper.addInstructions(0,"invoke-static/range { p0 .. p0 }, $R->rememberLink(Ljava/lang/Object;)V")
        val classicMapper=mutableClassDefBy("Lqkr;").methods.filter{
            it.params().firstOrNull()==LINK&&it.calls().any{r->r.definingClass=="Ly39;"&&r.name=="<init>"}
        }.one("classic Link mapper")
        check(AccessFlags.STATIC.isSet(classicMapper.accessFlags),"classic mapper no longer static")
        classicMapper.addInstructions(0,"invoke-static/range { p0 .. p0 }, $R->rememberLink(Ljava/lang/Object;)V")
        validate("Lpj50;","PostPreviewComponentFragment(id=",mapOf("a" to "Ljava/lang/String;","d" to "Lnj50;"))
        validate("Lnj50;","Post(__typename=",mapOf("i" to "Lqs50;"))
        check(classDefBy("Lqs50;").fields.any{it.name=="a"&&it.type=="Lps50;"},"GraphQL subreddit wrapper changed")
        validate("Lps50;","Subreddit(type=",mapOf("c" to "Ljava/lang/String;"))
        val rawPost=mutableClassDefBy("Lpj50;").methods.filter{it.name=="<init>"&&it.params()==listOf("Ljava/lang/String;","Loj50;",LIST,"Lnj50;")}.one("GraphQL post constructor")
        for(i in rawPost.code().indices.reversed())if(rawPost.code()[i].opcode==Opcode.RETURN_VOID)
            rawPost.addInstructions(i,"invoke-static/range { p0 .. p0 }, $R->rememberGraphqlPost(Ljava/lang/Object;)V")
        // Narrow native feed converter. Null output is already explicitly skipped by callers.
        val converter=Fingerprint(strings=listOf("FeedConverter.convert("),parameters=listOf("Lmtj;"),returnType="Lulc;").method
        check(converter.implementation!!.registerCount>=3,"converter locals missing")
        converter.addInstructionsWithLabels(0,"""
            invoke-static/range { p1 .. p1 }, $R->hideFeedPost(Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :show
            const/4 v0, 0x0
            return-object v0
            :show
            nop
        """.trimIndent())
        val processor=Fingerprint(parameters=listOf("Lcom/reddit/feeds/data/FeedType;",LIST,LIST),returnType="Lkotlin/Pair;").matchAll().one("feed processor").method
        processor.addInstructions(0,"invoke-static/range { p2 .. p2 }, $R->filterFeedElements($LIST)$LIST\nmove-result-object p2")
        // Both native tree-to-view projections. Clone only the emitted view state.
        for(type in listOf("Lcom/reddit/comments/tree/a;","Lcom/reddit/comments/tree/b;")){
            val m=mutableClassDefBy(type).methods.filter{it.name=="emit"&&it.params().size==2}.one("$type emit")
            val c=m.code();val i=c.indices.filter{c[it].opcode==Opcode.CHECK_CAST&&c[it].ref().toString()=="Lopa;"}.one("comment state cast")
            val r=(c[i] as OneRegisterInstruction).registerA
            check(m.calls().any{it.name=="emit"},"not a tree view emitter")
            m.addInstructions(i+1,"invoke-static/range { v$r .. v$r }, $R->filterCommentState(Ljava/lang/Object;)Ljava/lang/Object;\nmove-result-object v$r\ncheck-cast v$r, Lopa;")
        }
        val main=mutableClassDefBy("Lcom/reddit/launch/main/MainActivity;").methods.filter{it.name=="onCreate"&&it.params()==listOf("Landroid/os/Bundle;")}.one("main onCreate")
        main.addInstructions(0,"invoke-static/range { p0 .. p0 }, $R->initialize(Landroid/content/Context;)V")
        println("Content filters: validated 2026.47.0 models; Listing, Link, converter, feed processor, 2 comment projections, startup hooked")
    }
    finalize {
        val settings=mutableClassDefByOrNull("Lapp/morphe/extension/reddit/settings/preference/RedditPreferenceFragment;")
        if(settings!=null){
            val m=settings.methods.filter{it.name=="initialize"&&it.params().isEmpty()}.one("Morphe initialize")
            // A wrapper preserves `this` and composes with Custom Home's initializer wrapper.
            val helper=ImmutableMethod(settings.type,"neszi_keywordOriginalInitialize",emptyList(),"V",AccessFlags.PRIVATE.value,null,null,m.implementation).toMutable()
            val wrapper=ImmutableMethod(settings.type,m.name,m.parameters,m.returnType,m.accessFlags,m.annotations,m.hiddenApiRestrictions,MutableMethodImplementation(1)).toMutable()
            wrapper.addInstructions(0,"""
                invoke-direct/range { p0 .. p0 }, ${settings.type}->neszi_keywordOriginalInitialize()V
                invoke-static/range { p0 .. p0 }, $R->addSettingsFragment(Landroid/preference/PreferenceFragment;)V
                return-void
            """.trimIndent())
            settings.methods.remove(m);settings.methods.add(helper);settings.methods.add(wrapper)
            println("Content filters: integrated into official Morphe settings")
        }else{
            // Same native URL dispatcher used by Morphe, narrowly match Privacy Policy at runtime.
            val m=Fingerprint(parameters=listOf("L","Landroid/app/Activity;","Landroid/net/Uri;","Landroid/os/Bundle;","Z","I"),returnType="V",strings=listOf("android.intent.action.VIEW"),custom={m,_->AccessFlags.STATIC.isSet(m.accessFlags)&&m.calls().any{it.name=="getPackageManager"}}).matchAll().one("settings URL dispatcher").method
            check(m.implementation!!.registerCount>6,"URL dispatcher lacks locals")
            m.addInstructionsWithLabels(0,"""
                invoke-static { p1, p2 }, $R->openSettings(Landroid/app/Activity;Landroid/net/Uri;)Z
                move-result v0
                if-eqz v0, :original
                return-void
                :original
                nop
            """.trimIndent())
            println("Content filters: standalone settings via Reddit Settings > Privacy Policy")
        }
    }
}
