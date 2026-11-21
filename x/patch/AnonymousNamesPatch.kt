package app.crimera.patches.newx.misc.anonymous

import com.nesquik.patches.x.pikoScreen
import app.crimera.bytecode.*
import app.crimera.bytecode.Target
import app.crimera.patches.common.requireExactlyOne
import app.crimera.patches.common.semantic.fieldForFlattenedFormatterLabel
import app.crimera.patches.newx.models.requireSingle
import app.crimera.patches.newx.utils.unsupportedOnMount
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.patch.*
import app.morphe.patcher.util.*
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.reference.*

private const val O="Ljava/lang/Object;"
private const val S="Ljava/lang/String;"
private const val N="Lapp/morphe/extension/newx/anonymous/Names;"
private const val W="Lapp/morphe/extension/newx/anonymous/PresentationUser;"
private const val COMPOSER="Landroidx/compose/runtime/Composer;"
private val INVOKES=setOf(Opcode.INVOKE_INTERFACE,Opcode.INVOKE_INTERFACE_RANGE)
private val STATICS=setOf(Opcode.INVOKE_STATIC,Opcode.INVOKE_STATIC_RANGE)
private val POST_SCOPES=listOf("Lcom/x/ui/common/post/","Lcom/x/urt/items/post/","Lcom/x/postdetail/")

@Suppress("unused")
val anonymousNamesPatch=bytecodePatch(
 name="NewX: Anonymous Names",
 description="Anonymisation visuelle stable des auteurs de publications. Profils et recherche de comptes préservés.",default=false
){
 unsupportedOnMount(false)
 compatibleWith(Compatibility(packageName="com.twitter.android",name="NewX",apkFileType=ApkFileType.APKM,targets=listOf(AppTarget(version="12.43.0-prod.01"))))
 extendWith("extensions/anonymous.mpe")
 pikoScreen("newx.content.anonymous_names", "piko_newx_anonymous_names", 410, "Lapp/morphe/extension/newx/anonymous/AnonymousNamesFragment;")
 execute { installAnonymousNames() }
}

private fun Method.refs():List<MethodReference> = implementation?.instructions?.toList().orEmpty().mapNotNull{it.getReference<MethodReference>()}
private fun Method.calls(ref:MethodReference)=refs().count{it.toString()==ref.toString()}
private fun newMethod(owner:String,name:String,params:List<String>,ret:String,stat:Boolean=true,locals:Int=4):MutableMethod{
 val words=params.sumOf{if(it=="J"||it=="D")2 else 1}+(if(stat)0 else 1)
 return MutableMethod(ImmutableMethod(owner,name,params.map{ImmutableMethodParameter(it,emptySet(),null)},ret,AccessFlags.PUBLIC.value or (if(stat)AccessFlags.STATIC.value else 0),emptySet(),emptySet(),ImmutableMethodImplementation(words+locals,emptyList(),emptyList(),emptyList())))
}
context(c:BytecodePatchContext)
private fun installAnonymousNames(){
 val minimal=Fingerprint(definingClass="Lcom/x/models/",name="toString",returnType=S,parameters=emptyList(),filters=listOf(string("MinimalUser(id="))).requireSingle("Anonymous minimal user").originalMethod
 fun field(label:String)=minimal.fieldForFlattenedFormatterLabel(label){c.classDefByOrNull(it)}
 val rawNameField=field(", name=");val rawHandleField=field(", screenName=");val idField=field("MinimalUser(id=")
 fun getter(f:FieldReference):Method = requireExactlyOne("Anonymous minimal getter for $f",c.mutableClassDefBy(minimal.definingClass).methods.filter{m->val ins=m.implementation?.instructions?.toList().orEmpty();m.parameterTypes.isEmpty()&&m.returnType==f.type&&ins.size==2&&ins[0].getReference<FieldReference>()?.toString()==f.toString()&&ins[1].opcode==Opcode.RETURN_OBJECT})
 val nameGetter=getter(rawNameField);val handleGetter=getter(rawHandleField);val idGetter=getter(idField)
 val userInterfaces=c.mutableClassDefBy(minimal.definingClass).interfaces.mapNotNull{c.classDefByOrNull(it)}
 val user=requireExactlyOne("Anonymous user presentation interface",userInterfaces.filter{cl->cl.methods.any{it.name==nameGetter.name}&&cl.methods.any{it.name==handleGetter.name}&&cl.methods.any{it.name==idGetter.name}})
 fun interfaceGetter(g:Method)=requireExactlyOne("Anonymous interface getter $g",user.methods.filter{it.name==g.name&&it.parameterTypes.isEmpty()&&it.returnType==g.returnType})
 val rawName=interfaceGetter(nameGetter);val rawHandle=interfaceGetter(handleGetter);val identifier=interfaceGetter(idGetter)
 val formattedHandle=requireExactlyOne("Anonymous formatted handle",user.methods.filter{it.returnType==S&&it.parameterTypes.isEmpty()&&it.calls(rawHandle)==1&&it.implementation?.instructions?.any{i->i.getReference<StringReference>()?.string=="@"}==true})
 val displayName=requireExactlyOne("Anonymous fallback display name",user.methods.filter{it.returnType==S&&it.parameterTypes.isEmpty()&&it.calls(rawName)==1&&it.calls(formattedHandle)==1})
 val idToString=requireExactlyOne("Anonymous numeric identifier formatter",c.mutableClassDefBy(idField.type).methods.filter{it.name=="toString"&&it.parameterTypes.isEmpty()&&it.returnType==S})
 println("Anonymous user contract: ${user.type}; id=$identifier; name=$displayName; handle=$formattedHandle")
 val names=c.mutableClassDefBy(N)
 fun replace(name:String,emit:Block.(Int)->Unit){val old=requireExactlyOne("Anonymous bridge $name",names.methods.filter{it.name==name});val m=newMethod(N,name,old.parameterTypes.map{it.toString()},old.returnType);m.insertHook(0,relocateBranchTargets=false){emit(m.p0Register)};names.methods.remove(old);names.methods.add(m)}
 replace("userId"){p->instanceOf(0,p,user.type);ifEqz(0,Target.Local("nil"));checkCast(p,user.type);invokeInterface(methodReference(identifier.toString()),p);moveResult(0,idField.type);ifEqz(0,Target.Local("nil"));invokeVirtual(methodReference(idToString.toString()),0);moveResult(0,S);returnObject(0);label("nil");constInt(0,0);returnObject(0)}
 for((name,getter,kind)in listOf(Triple("display",displayName,0),Triple("handle",formattedHandle,2))){replace(name){p->checkCast(p,user.type);invokeInterface(methodReference(getter.toString()),p);moveResult(0,S);constInt(1,kind);invokeStatic(methodReference("$N->render($S${O}I)$S"),0,p,1);moveResult(0,S);returnObject(0)}}
 // A wrapper exists ONLY in the presentation call. All X contracts delegate to the original.
 val wrapper=c.mutableClassDefBy(W);wrapper.interfaces.add(user.type)
 val original=ImmutableFieldReference(W,"original",O)
 val methods=user.methods.filter{!AccessFlags.STATIC.isSet(it.accessFlags)}.toList()
 if(user.interfaces.isNotEmpty()||methods.any{it.parameterTypes.isNotEmpty()||it.returnType=="V"})throw PatchException("Anonymous unsupported user interface shape")
 val kinds=mapOf(rawName.toString() to 0,displayName.toString() to 0,rawHandle.toString() to 1,formattedHandle.toString() to 2)
 for(target in methods){
  val method=newMethod(W,target.name,emptyList(),target.returnType,stat=false)
  method.insertHook(0,relocateBranchTargets=false){
   iget(0,method.p0Register,original);checkCast(0,user.type);invokeInterface(methodReference(target.toString()),0);moveResult(1,target.returnType)
   val kind=kinds[target.toString()]
   if(kind!=null){constInt(2,kind);invokeStatic(methodReference("$N->render($S${O}I)$S"),1,0,2);moveResult(1,S)}
   when(target.returnType){"Z","I","B","S","C","F"->returnValue(1);"J","D"->returnWide(1);else->returnObject(1)}
  };wrapper.methods.add(method)
 }
 // Identify the attribution renderer from BOTH semantic getters and a timestamp parameter.
 val renderer=Fingerprint(definingClass="Lcom/x/ui/common/user/",returnType="V",custom={m,_->AccessFlags.STATIC.isSet(m.accessFlags)&&m.parameterTypes.firstOrNull()==user.type&&m.parameterTypes.contains(COMPOSER)&&m.parameterTypes.contains("Lkotlin/time/Instant;")&&m.calls(displayName)==1&&m.calls(formattedHandle)==1}).requireSingle("Anonymous attribution renderer").originalMethod
 val params=renderer.parameterTypes.map{it.toString()}
 val renderBridge=newMethod(N,"renderAttribution",params,"V")
 renderBridge.insertHook(0,relocateBranchTargets=false){val p=renderBridge.p0Register;invokeStatic(methodReference("$N->wrap($O)$O"),p);moveResult(p,O);checkCast(p,user.type);invokeStatic(methodReference(renderer.toString()),*(p until p+renderBridge.numberOfParameterRegisters).toList().toIntArray());returnVoid()}
 names.methods.add(renderBridge)
 // Scope is a semantic UI boundary, not a guessed obfuscated owner. Original shared renderer
 // remains intact for profiles/account search/DM. Its restart callbacks retain the wrapper.
 val allClasses=mutableListOf<com.android.tools.smali.dexlib2.iface.ClassDef>();c.classDefForEach{allClasses.add(it)}
 val postRow = Fingerprint(
     definingClass = "Lcom/x/urt/items/post/",
     name = "toString",
     filters = listOf(string("AvailablePost(entryId=")),
 ).requireSingle("Anonymous available post").originalClassDef.type
 val allMethods = allClasses.flatMap { it.methods }
 val postWrappers = allMethods.filter { method ->
     method.parameterTypes.contains(postRow) && method.parameterTypes.contains(COMPOSER)
 }
 val callers = allMethods.filter { method ->
     method.calls(renderer) > 0 && (
         POST_SCOPES.any(method.definingClass::startsWith) ||
         (method.parameterTypes.contains(user.type) &&
             method.parameterTypes.contains("Lkotlin/time/Instant;") &&
             method.parameterTypes.contains(COMPOSER) &&
             postWrappers.any { it.definingClass == method.definingClass && it.calls(method) > 0 })
     )
 }
 if(callers.size!=2||callers.sumOf{it.calls(renderer)}!=2)throw PatchException("Anonymous expected exactly 2 post attribution call sites, found ${callers.size}: $callers")
 fun replaceCall(m:MutableMethod,index:Int,ref:MethodReference){
  val ins=m.implementation!!.instructions[index]
  val args=when(ins){is FiveRegisterInstruction->listOf(ins.registerC,ins.registerD,ins.registerE,ins.registerF,ins.registerG).take(ins.registerCount);is RegisterRangeInstruction->(ins.startRegister until ins.startRegister+ins.registerCount).toList();else->throw PatchException("Unsupported invoke")}
  // Emit through the typed layer into a temporary body, then replace exactly one invoke.
  // Inserting/removing by total instruction-count delta is unsafe: dexlib can materialize
  // a switch-payload alignment nop elsewhere and shift that delta by one.
  val temporary=newMethod(N,"emitOnly",emptyList(),"V",locals=m.implementation!!.registerCount)
  temporary.insertHook(0,relocateBranchTargets=false){invokeStatic(methodReference(ref.toString()),*args.toIntArray())}
  val replacement=requireExactlyOne("Anonymous single invoke replacement",temporary.implementation!!.instructions)
  if(replacement.codeUnits!=ins.codeUnits)throw PatchException("Anonymous invoke width changed")
  m.implementation!!.replaceInstruction(index,replacement)
 }
 val mutableOwners=mutableMapOf<String,app.morphe.patcher.util.proxy.mutableTypes.MutableClass>()
 fun owner(t:String)=mutableOwners.getOrPut(t){c.mutableClassDefBy(t)}

 for(caller in callers){val m=requireExactlyOne("Anonymous caller mutable",owner(caller.definingClass).methods.filter{it.toString()==caller.toString()});val indices=m.implementation!!.instructions.withIndex().filter{it.value.opcode in STATICS&&it.value.getReference<MethodReference>()?.toString()==renderer.toString()}.map{it.index};for(i in indices.sortedDescending())replaceCall(m,i,renderBridge);println("Anonymous attribution caller: $m")}
 // Expanded post/quote paths emit name and handle directly. Require the paired getter contract,
 // UI namespace and Composer usage; do not hook logging, share actions or model serializers.
 val direct=allClasses.filter{cl->cl.type.startsWith("Lcom/x/urt/items/post/")||cl.type.startsWith("Lcom/x/postdetail/")}.flatMap{it.methods}.filter{m->m.calls(displayName)>0&&m.calls(formattedHandle)>0&&m.refs().any{it.parameterTypes.contains(COMPOSER)}}.toList()
 if(direct.size!=2)throw PatchException("Anonymous expected 2 direct post renderers, found ${direct.size}: $direct")
 var directCount=0
 for(d in direct){val m=requireExactlyOne("Anonymous direct mutable",owner(d.definingClass).methods.filter{it.toString()==d.toString()});val slots=m.implementation!!.instructions.withIndex().filter{it.value.opcode in INVOKES&&it.value.getReference<MethodReference>()?.toString() in setOf(displayName.toString(),formattedHandle.toString())}.map{it.index};for(i in slots.sortedDescending()){val ref=m.implementation!!.instructions[i].getReference<MethodReference>()!!;val name=if(ref.toString()==displayName.toString())"display" else "handle";replaceCall(m,i,ImmutableMethodReference(N,name,listOf(O),S));directCount++};println("Anonymous direct renderer: $m")}
 // Repost attribution is a separate social-context renderer. Follow its semantic actor field
 // into the display getter, so the "X reposted" label does not disclose the reposter.
 val repostModel=Fingerprint(definingClass="Lcom/x/models/",name="toString",returnType=S,parameters=emptyList(),filters=listOf(string("Repost(repostAuthor="))).requireSingle("Anonymous repost context").originalMethod
 val repostAuthor=repostModel.fieldForFlattenedFormatterLabel("Repost(repostAuthor="){c.classDefByOrNull(it)}
 val social=Fingerprint(definingClass="Lcom/x/ui/common/",returnType="V",custom={m,_->m.parameterTypes.contains(COMPOSER)&&m.calls(displayName)==1&&m.implementation?.instructions?.any{it.getReference<FieldReference>()?.toString()==repostAuthor.toString()}==true}).requireSingle("Anonymous repost attribution").method
 val code=social.implementation!!.instructions.toList()
 val slot=requireExactlyOne("Anonymous repost actor-to-name flow",code.withIndex().filter{(i,ins)->
  val read=ins as? TwoRegisterInstruction
  val call=code.getOrNull(i+1) as? FiveRegisterInstruction
  ins.opcode==Opcode.IGET_OBJECT&&ins.getReference<FieldReference>()?.toString()==repostAuthor.toString()&&call?.registerCount==1&&call.registerC==read?.registerA&&code[i+1].getReference<MethodReference>()?.toString()==displayName.toString()
 }.map{it.index+1})
 replaceCall(social,slot,ImmutableMethodReference(N,"display",listOf(O),S))
 println("Anonymous repost renderer: $social")
 println("Anonymous Names: ${callers.size} attribution calls, $directCount direct reads, 1 repost label, ${methods.size} delegating methods")
}
