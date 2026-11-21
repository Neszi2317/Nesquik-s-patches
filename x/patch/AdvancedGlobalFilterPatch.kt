package app.crimera.patches.newx.timeline.advanced

import com.nesquik.patches.x.*
import app.crimera.bytecode.*
import app.crimera.bytecode.Target
import app.crimera.patches.common.requireExactlyOne
import app.crimera.patches.common.semantic.fieldForFlattenedFormatterLabel
import app.crimera.patches.newx.models.*
import app.crimera.patches.newx.utils.Constants.TIMELINE_FILTER_DESCRIPTOR
import app.crimera.patches.newx.utils.unsupportedOnMount
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.patch.*
import app.morphe.patcher.util.*
import app.morphe.util.cloneMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.*
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import app.morphe.extension.newx.postfilter.AdvancedMatcher
import com.google.gson.Gson
import java.io.File

private const val O="Ljava/lang/Object;"
private const val S="Ljava/lang/String;"
private const val M="Lapp/morphe/extension/newx/postfilter/AdvancedModels;"
private const val F="Lapp/morphe/extension/newx/postfilter/AdvancedFilter;"
private val keywords=stringsOption("advancedKeywords",default=emptyList(),title="Mots-clés",description="Ajout, modification et suppression. Un mot/expression par entrée. Collage multiligne accepté.")
private val file1=filePathOption("advancedFile",title="Importer TXT / CSV / JSON",allowedExtensions=listOf("txt","csv","json"))
private val file2=filePathOption("advancedFile2",title="Deuxième liste à fusionner (facultatif)",allowedExtensions=listOf("txt","csv","json"))
private val importMode=stringOption("advancedImportMode",default="merge",values=mapOf("Fusionner" to "merge","Remplacer la saisie manuelle" to "replace"),title="Mode d’importation")
private val mode=stringOption("advancedMatchMode",default="substring",values=mapOf("Sous-chaîne" to "substring","Mot entier" to "word","Regex RE2" to "regex"),title="Correspondance")
private val strict=booleanOption("advancedStrict",default=false,title="Mode strict",description="Masquer aussi les réponses/citations dont le parent est absent du cache.")
private val fold=booleanOption("advancedFoldAccents",default=true,title="Ignorer les accents (hors regex)")
private val exportFolder=folderOption("advancedExportFolder",title="Exporter la liste normalisée (facultatif)",description="Écrit piko-keywords.txt dans ce dossier pendant le patching. Le nombre final est affiché dans le journal et dans X.")

private val seeds=rawResourcePatch(default=false){execute {
    val all=mutableListOf<String>()
    if(importMode.value!="replace"||file1.file==null&&file2.file==null)keywords.value.orEmpty().forEach{all.addAll(it.lines())}
    for(file in listOfNotNull(file1.file,file2.file)){
        if(!file.isFile||file.length()>2097152)throw PatchException("Liste absente ou supérieure à 2 Mo: $file")
        // JVM patch-time parser is Gson-based; Android runtime uses org.json.
        val text=file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        when(file.extension.lowercase()){
            "json"->{val json=com.google.gson.JsonParser.parseString(text);val arr=if(json.isJsonArray)json.asJsonArray else json.asJsonObject.getAsJsonArray("keywords")
                ?:throw PatchException("JSON attendu : tableau ou {keywords:[...]}")
                arr.forEach{if(!it.isJsonPrimitive||!it.asJsonPrimitive.isString)throw PatchException("JSON: uniquement des chaînes");all.add(it.asString)}}
            "csv"->all.addAll(parseCsv(text))
            else->all.addAll(text.lines())
        }
    }
    val matcher=try{AdvancedMatcher(all,mode.value!!,fold.value!!)}catch(e:Exception){throw PatchException("Liste invalide: ${e.message}")}
    val list=matcher.rules()
    val json=Gson().toJson(mapOf("keywords" to list,"mode" to mode.value,"strict" to strict.value,"foldAccents" to fold.value,"enabled" to true))
    get("assets/piko-advanced-filter.json").apply{parentFile.mkdirs();writeText(json)}
    exportFolder.file?.let{it.mkdirs();File(it,"piko-keywords.txt").writeText(list.joinToString("\n"))}
    println("Advanced Global Filter: ${list.size} mots-clés, mode=${mode.value}, strict=${strict.value}")
}}
private fun parseCsv(s:String):List<String>{val out=mutableListOf<String>();val cell=StringBuilder();var quoted=false;var i=0
    while(i<s.length){val c=s[i++];when{c=='"'->{if(quoted&&i<s.length&&s[i]=='"'){cell.append('"');i++}else quoted=!quoted}
        !quoted&&c in ",;\r\n"->{out.add(cell.toString());cell.setLength(0)}else->cell.append(c)}}
    if(quoted)throw PatchException("CSV: guillemet non fermé");out.add(cell.toString());return out}

@Suppress("unused")
val advancedGlobalFilterPatch=bytecodePatch(name="NewX: Advanced Global Keyword Filter",description="Filtre local global avec import TXT/CSV/JSON, pseudos, citations, réponses et tendances. Configuration avant patching et dans Piko.",default=false){
    unsupportedOnMount(false);compatibleWith(Compatibility(packageName="com.twitter.android", name="NewX", apkFileType=ApkFileType.APKM, targets=listOf(AppTarget(version="12.43.0-prod.01"))))
    dependsOn(seeds)
    keywords();file1();file2();importMode();mode();strict();fold();exportFolder()
    extendWith("extensions/advanced.mpe")
    requirePikoSettingsResources()
    finalize{
        requirePikoTimeline()
        forwardTimelineBridges();installModels();installSearchHook()
        registerPikoScreen("newx.content.advanced_global_filter", "piko_newx_advanced_filter", 405, "Lapp/morphe/extension/newx/postfilter/AdvancedFilterFragment;")
        val owner=mutableClassDefBy(TIMELINE_FILTER_DESCRIPTOR)
        val method=requireExactlyOne("public combined timeline entry",owner.methods.filter{it.name=="filterTimelineItems"&&it.parameterTypes==listOf(O)})
        method.insertHook(0,relocateBranchTargets=false){invokeStatic(methodReference("$F->filter($O)$O"),method.p0Register);moveResult(method.p0Register,O)}
    }
}

context(c:BytecodePatchContext)
private fun replace(name:String,emit:Block.(Int)->Unit){
    val owner=c.mutableClassDefBy(M);val old=requireExactlyOne("Advanced bridge $name",owner.methods.filter{it.name==name})
    val words=old.numberOfParameterRegisters
    val m=old.cloneMutable(additionalRegisters=words+4-old.implementation!!.registerCount)
    owner.methods.remove(old);owner.methods.add(m);val body=m.implementation!!
    while(body.instructions.isNotEmpty())body.removeInstruction(body.instructions.lastIndex)
    m.insertHook(0,relocateBranchTargets=false){emit(m.p0Register)}
}
context(c:BytecodePatchContext)
private fun forwardTimelineBridges(){
    val names=mapOf("isPost" to "isTimelinePost","isModule" to "isTimelineModule","isWrapper" to "isTimelineModuleItem","unwrap" to "getModuleItem","dispensable" to "isModuleItemDispensable","copyWrapper" to "copyModuleItem","children" to "getModuleInnerContent","display" to "getModuleDisplayType","header" to "getModuleHeader","footer" to "getModuleFooter","sort" to "getModuleSortIndex","entry" to "getModuleEntryId","event" to "getModuleClientEventInfo","copyModule" to "copyModule","immutable" to "immutableList","vertical" to "isVerticalConversation","verticalIds" to "getVerticalConversationPostIds","copyVertical" to "copyVerticalConversation")
    val old=c.mutableClassDefBy(TIMELINE_FILTER_DESCRIPTOR)
    for((name,target)in names){val targetMethod=requireExactlyOne("Piko forwarding bridge $target",old.methods.filter{it.name==target})
        targetMethod.accessFlags=(targetMethod.accessFlags and AccessFlags.PRIVATE.value.inv() and AccessFlags.PROTECTED.value.inv()) or AccessFlags.PUBLIC.value
        val reference=methodReference(targetMethod.toString());replace(name){p->val args=(p until p+targetMethod.numberOfParameterRegisters).toList().toIntArray();invokeStatic(reference,*args);moveResult(0,targetMethod.returnType)
            when(targetMethod.returnType){"Z","I"->returnValue(0);"J"->returnWide(0);else->returnObject(0)}}}
}
context(c:BytecodePatchContext)
private fun formatter(anchor:String,scope:String="Lcom/x/models/"):Method=Fingerprint(definingClass=scope,name="toString",returnType=S,parameters=emptyList(),filters=listOf(string(anchor))).requireSingle("Advanced $anchor").originalMethod
context(c:BytecodePatchContext)
private fun field(m:Method,label:String):FieldReference{
    val f=m.fieldForFlattenedFormatterLabel(label){c.classDefByOrNull(it)};c.mutableClassDefBy(f.definingClass).requirePublicFields(listOf(f));println("Advanced field '$label': $f");return f
}
context(c:BytecodePatchContext)
private fun objectBridge(name:String,variants:List<List<FieldReference>>){
    val resultType=requireExactlyOne("advanced bridge result $name",c.mutableClassDefBy(M).methods.filter{it.name==name}).returnType
    for(path in variants){val f=path.lastOrNull()?:throw PatchException("Empty bridge path: $name")
        if(!f.type.startsWith("L")&&!f.type.startsWith("["))throw PatchException("Object bridge cannot return primitive: $name $f")
        if(resultType!=O&&resultType!=f.type)throw PatchException("Bridge return mismatch: $name needs $resultType, field=$f")
    }
    replace(name){p->for((i,path)in variants.withIndex()){val next=Target.Local("next$i");move(1,p,O)
        for(f in path){instanceOf(0,1,f.definingClass);ifEqz(0,next);checkCast(1,f.definingClass);iget(1,1,f)}
        returnObject(1);label("next$i")};constInt(0,0);returnObject(0)}
}
context(c:BytecodePatchContext)
private fun installModels(){
    val tm=resolvedNewXTimelineModels();val pm=resolvedNewXPostModels()
    val canon=formatter("CanonicalPost(id=");val contextual=formatter("ContextualPost(canonicalPost=")
    val repostClass=c.mutableClassDefBy(pm.contextualRepostedPostField.type)
    val repostFormatter=requireExactlyOne("repost formatter",repostClass.methods.filter{it.name=="toString"&&it.parameterTypes.isEmpty()})
    val canonFields=listOf(pm.contextualCanonicalPostField,pm.repostedCanonicalPostField)
    val quoteFields=listOf(field(contextual,", quotedPost="),field(repostFormatter,", quotedPost="))
    val required=listOf(tm.postResultField,pm.contextualRepostedPostField)+canonFields
    for(f in required)c.mutableClassDefBy(f.definingClass).requirePublicFields(listOf(f))
    objectBridge("result",listOf(listOf(tm.postResultField)));objectBridge("canonical",canonFields.map{listOf(it)})
    objectBridge("quote",quoteFields.map{listOf(it)});objectBridge("repost",listOf(listOf(pm.contextualRepostedPostField)))
    val id=field(canon,"CanonicalPost(id=")
    for((name,label)in mapOf("text" to ", text=","author" to ", author=","parentId" to ", repliedPostId=","quoteId" to ", quotedStatusId="))objectBridge(name,listOf(listOf(field(canon,label))))
    objectBridge("postId",listOf(listOf(id)))
    // Verify the exact identifier contract rather than reflect over obfuscated models.
    val idClass=c.mutableClassDefBy(id.type)
    val getter=requireExactlyOne("post identifier getValue",idClass.methods.filter{it.name=="toString"&&it.parameterTypes.isEmpty()&&it.returnType==S})
    replace("identifier"){p->instanceOf(0,p,id.type);ifEqz(0,Target.Local("nil"));checkCast(p,id.type);invokeVirtual(methodReference(getter.toString()),p);moveResult(0,S);returnObject(0);label("nil");constInt(0,0);returnObject(0)}
    val userMatch=Fingerprint(definingClass="Lcom/x/models/",name="toString",returnType=S,parameters=emptyList(),filters=listOf(string("MinimalUser(id="))).requireSingle("minimal user")
    val author=field(canon,", author=")
    for((name,label)in mapOf("username" to ", screenName=","name" to ", name=","bio" to ", profileDescription=")){
        val userField=field(userMatch.originalMethod,label)
        if(userField.type!=S)throw PatchException("Author $name is not a string: $userField")
        val getter=requireExactlyOne("author $name getter",c.mutableClassDefBy(userField.definingClass).methods.filter{m->
            val code=m.implementation?.instructions?.toList().orEmpty()
            m.parameterTypes.isEmpty()&&m.returnType==S&&code.size==2&&code[0].getReference<FieldReference>()?.toString()==userField.toString()&&code[1].opcode==Opcode.RETURN_OBJECT})
        val seen=mutableSetOf<String>()
        fun findInterface(t:String):MethodReference? {if(!seen.add(t))return null;val klass=c.classDefByOrNull(t)?:return null
            val found=klass.methods.filter{it.name==getter.name&&it.parameterTypes.isEmpty()&&it.returnType==S}
            if(found.isNotEmpty())return requireExactlyOne("author interface $name",found)
            val inherited=klass.interfaces.mapNotNull{findInterface(it)}.distinctBy{it.toString()}
            return if(inherited.isEmpty())null else requireExactlyOne("inherited author interface $name",inherited)}
        val ref=findInterface(author.type)?:throw PatchException("Author interface missing $name")
        replace(name){p->instanceOf(0,p,author.type);ifEqz(0,Target.Local("nil"));checkCast(p,author.type);invokeInterface(ref,p);moveResult(0,S);returnObject(0);label("nil");constInt(0,0);returnObject(0)}
        println("Advanced author $name: $ref")
    }
    val urt=formatter("UrtTimelineTrend(timelineTrend=")
    val trend=formatter("TimelineTrend(thumbnailImageUrl=")
    val trendField=field(urt,"UrtTimelineTrend(timelineTrend=")
    objectBridge("trendText",listOf(listOf(trendField,field(trend,", name="))))
    objectBridge("trendDescription",listOf(listOf(trendField,field(trend,", promotedDescription="))))
    objectBridge("suggestedUser",listOf(listOf(field(formatter("UrtTimelineUser(user="),"UrtTimelineUser(user=")),listOf(field(formatter("User(user=","Lcom/x/models/search/"),"User(user="))))
    // Extended text hooks added only when their complete semantic path is verified.
    val note=field(canon,", notePost=");val noteFmt=c.mutableClassDefBy(note.type).methods.filter{it.name=="toString"&&it.parameterTypes.isEmpty()}
    val noteMethod=requireExactlyOne("note post formatter",noteFmt)
    val noteResult=field(noteMethod,", noteTweetResult=")
    objectBridge("noteText",listOf(listOf(note,noteResult,field(formatter("NotePost(postEntityList="),", text="))))
    val card=field(canon,", legacyCard=")
    val cardFmt=formatter("LegacyCard(cardPlatform=")
    objectBridge("cardBindings",listOf(listOf(card,field(cardFmt,", bindings="))))
    objectBridge("cardUrl",listOf(listOf(card,field(cardFmt,", url="))))
    val binding=formatter("CardBinding(key=");objectBridge("bindingKey",listOf(listOf(field(binding,"CardBinding(key="))))
    objectBridge("bindingText",listOf(listOf(field(binding,", value="),field(formatter("StringValue(value=","Lcom/x/models/cards/"),"StringValue(value="))))
    val meta=field(trend,", trendMetadata=");val metaFmt=formatter("TrendMetadata(domainContext=")
    objectBridge("trendDomain",listOf(listOf(trendField,meta,field(metaFmt,"TrendMetadata(domainContext="))))
    objectBridge("trendMetadata",listOf(listOf(trendField,meta,field(metaFmt,", metaDescription="))))
    val suggestion=formatter("Suggestion(text=","Lcom/x/models/search/")
    objectBridge("suggestionText",listOf(listOf(field(suggestion,"Suggestion(text="))))
    objectBridge("suggestionDisplay",listOf(listOf(field(suggestion,", displayString="))))
    objectBridge("trendGroups",listOf(listOf(trendField,field(trend,", groupedTrends="))))
    objectBridge("groupName",listOf(listOf(field(formatter("GroupedTrend(name="),"GroupedTrend(name=")),listOf(field(formatter("GroupedTrends(name="),"GroupedTrends(name="))))
    val notification=field(formatter("UrtNotification(sortIndex="),", notification=");val nf=formatter("TimelineNotification(id=")
    objectBridge("notificationPosts",listOf(listOf(notification,field(nf,", targetPosts="))))
    objectBridge("notificationUsers",listOf(listOf(notification,field(nf,", sourceUsers="))))
    val rich=field(formatter("RichText(text=","Lcom/x/models/text/"),"RichText(text=")
    objectBridge("notificationMessage",listOf(listOf(notification,field(nf,", richMessage="),rich)))
    objectBridge("notificationContext",listOf(listOf(notification,field(nf,", contextText="),rich)))
    val nt=formatter("NotificationTweet(id=","Lcom/x/models/notification/");val nu=formatter("NotificationUser(id=","Lcom/x/models/notification/")
    objectBridge("notificationPostText",listOf(listOf(field(nt,", text="))))
    objectBridge("notificationUsername",listOf(listOf(field(nu,", screenName="))))
    objectBridge("notificationName",listOf(listOf(field(nu,", fullName="))))
    for((name,label)in mapOf("notificationPostId" to "NotificationTweet(id=","notificationOriginalId" to ", originalId=")){
        val f=field(nt,label);if(f.type!="J")throw PatchException("Expected numeric notification ID: $f")
        replace(name){p->instanceOf(0,p,f.definingClass);ifEqz(0,Target.Local("zero"));checkCast(p,f.definingClass);iget(0,p,f);returnWide(0);label("zero");constLong(0,0);returnWide(0)}
    }
    println("Advanced surfaces: shared timelines, search suggestions, trends, legacy cards, timeline notifications")
}

context(c:BytecodePatchContext)
private fun installSearchHook(){
    val state=formatter("SearchQueryState(query=","Lcom/x/search/query/")
    val f=field(state,", typeaheads=")
    if(f.type!="Ljava/util/List;")throw PatchException("Unexpected typeahead collection: $f")
    val owner=c.mutableClassDefBy(state.definingClass)
    var count=0
    for(method in owner.methods){
        val code=method.implementation?.instructions?.toList()?:continue
        val writes=code.withIndex().filter{it.value.opcode==Opcode.IPUT_OBJECT&&it.value.getReference<FieldReference>()?.toString()==f.toString()}
        if(writes.isNotEmpty()&&method.name!="<init>")throw PatchException("Unexpected typeahead mutation: $method")
        for((index,ins)in writes.asReversed()){
            val write=ins as TwoRegisterInstruction
            if(write.registerB!=method.p0Register)throw PatchException("Unproven state receiver: $method")
            method.insertHook(index,relocateBranchTargets=true){invokeStatic(methodReference("$F->filterSuggestions($O)$O"),write.registerA);moveResult(write.registerA,O);checkCast(write.registerA,f.type)}
            count++
        }
    }
    if(count==0)throw PatchException("No proven typeahead constructor write")
    println("Advanced search: $count constructor writes into $f")
}
