package app.crimera.patches.newx.misc.profileimages
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
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*
import com.android.tools.smali.dexlib2.immutable.*

private const val O="Ljava/lang/Object;"
private const val S="Ljava/lang/String;"
private const val R="Lapp/morphe/extension/newx/profileimages/ProfileImages;"
private const val COMPOSER="Landroidx/compose/runtime/Composer;"
private fun Method.code()=implementation?.instructions?.toList().orEmpty()
private fun Method.refs()=code().mapNotNull{it.getReference<MethodReference>()}
private fun stat(m:Method)=AccessFlags.STATIC.isSet(m.accessFlags)
private fun newMethod(name:String,params:List<String>,ret:String,locals:Int=10):MutableMethod=MutableMethod(ImmutableMethod(R,name,params.map{ImmutableMethodParameter(it,emptySet(),null)},ret,AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,emptySet(),emptySet(),ImmutableMethodImplementation(locals+params.sumOf{if(it=="J"||it=="D")2 else 1},emptyList(),emptyList(),emptyList())))
@Suppress("unused")
val profileImagesPatch=bytecodePatch(name="NewX: Hide Profile Pictures & Banners",description="Local black avatars and profile banners, pre-draw guards for Glide/Coil. Device validation required.",default=false){
 unsupportedOnMount(false)
 compatibleWith(Compatibility(packageName="com.twitter.android",name="NewX",apkFileType=ApkFileType.APKM,targets=listOf(AppTarget(version="12.43.0-prod.01"))))
 extendWith("extensions/profile-images.mpe")
 pikoScreen("newx.content.profile_images", "piko_newx_profile_images", 420, "Lapp/morphe/extension/newx/profileimages/ProfileImagesFragment;")
 execute { install() }
}
context(c:BytecodePatchContext)
private fun install(){
 val all=mutableListOf<ClassDef>();c.classDefForEach{all.add(it)}
 fun mutable(m:Method)=requireExactlyOne("ProfileImages mutable $m",c.mutableClassDefBy(m.definingClass).methods.filter{it.toString()==m.toString()})
 fun formatter(prefix:String,scope:String)=Fingerprint(definingClass=scope,name="toString",returnType=S,parameters=emptyList(),filters=listOf(string(prefix))).requireSingle("ProfileImages $prefix").originalMethod
 fun field(m:Method,label:String)=m.fieldForFlattenedFormatterLabel(label){c.classDefByOrNull(it)}
 fun cls(t:String)=c.classDefByOrNull(t)?:throw PatchException("ProfileImages missing $t")
 val requestFmt=formatter("ImageRequest(context=","Lcoil3/request/")
 val data=field(requestFmt,", data=")
 val memKey=field(requestFmt,", listener=null, memoryCacheKey=")
 val diskKey=field(requestFmt,", diskCacheKey=")
 val placeholder=field(requestFmt,", placeholderMemoryCacheKey=")
 val coilCtor=requireExactlyOne("ProfileImages Coil request constructor",cls(requestFmt.definingClass).methods.filter{it.name=="<init>"&&it.code().count{i->i.getReference<FieldReference>()?.toString()==data.toString()&&i.opcode==Opcode.IPUT_OBJECT}==1})
 fun paramWrite(f:FieldReference):Int {val i=requireExactlyOne("ProfileImages constructor parameter write $f",coilCtor.code().filter{it.opcode==Opcode.IPUT_OBJECT&&it.getReference<FieldReference>()?.toString()==f.toString()}) as TwoRegisterInstruction;val words=coilCtor.parameterTypes.sumOf{if(it=="J"||it=="D")2 else 1}+1;val p0=coilCtor.implementation!!.registerCount-words;if(i.registerB!=p0||i.registerA<=p0)throw PatchException("Unexpected constructor flow $f");return i.registerA}
 val dataP=paramWrite(data);val memP=paramWrite(memKey);val diskP=paramWrite(diskKey);val placeholderP=paramWrite(placeholder)
 // Entry arguments are not reassigned before these direct stores in this proven constructor.
 val init=coilCtor.code().firstOrNull()?.getReference<MethodReference>()
 if(init?.toString()!="Ljava/lang/Object;-><init>()V")throw PatchException("Unexpected Coil constructor prologue")
 mutable(coilCtor).insertHook(1,relocateBranchTargets=false){invokeStatic(methodReference("$R->substitute($O)$O"),dataP);moveResult(dataP,O)
  for(p in listOf(memP,diskP)){invokeStatic(methodReference("$R->cacheKey($S$O)$S"),p,dataP);moveResult(p,S)}
  invokeStatic(methodReference("$R->clearPlaceholder($O$O)$O"),placeholderP,dataP);moveResult(placeholderP,O);checkCast(placeholderP,placeholder.type)
 }
 val glideLoad=requireExactlyOne("ProfileImages Glide model loader",all.filter{it.type.startsWith("Lcom/bumptech/glide/")}.flatMap{it.methods}.filter{m->m.parameterTypes==listOf(O)&&m.returnType==m.definingClass&&m.code().any{it.opcode==Opcode.IPUT_OBJECT&&it.getReference<FieldReference>()?.type==O}&&m.refs().any{it.toString()==m.toString()}})
 val modelField=requireExactlyOne("ProfileImages Glide model assignment",glideLoad.code().filter{it.opcode==Opcode.IPUT_OBJECT&&it.getReference<FieldReference>()?.type==O}).getReference<FieldReference>()!!
 val gl=mutable(glideLoad);gl.insertHook(0,relocateBranchTargets=false){invokeStatic(methodReference("$R->substitute($O)$O"),gl.p0Register+1);moveResult(gl.p0Register+1,O)}
 val glidePainter=requireExactlyOne("ProfileImages Glide painter",all.filter{cl->cl.type.startsWith("Lcom/x/ui/common/glide/")&&cl.fields.any{it.type==glideLoad.definingClass}&&cl.superclass?.startsWith("Landroidx/compose/ui/graphics/painter/")==true})
 val painterBase=glidePainter.superclass!!
 val glideRequest=requireExactlyOne("ProfileImages painter request field",glidePainter.fields.filter{it.type==glideLoad.definingClass})
 val glideDraw=requireExactlyOne("ProfileImages Glide onDraw",glidePainter.methods.filter{it.returnType=="V"&&it.parameterTypes.size==1&&it.parameterTypes[0].startsWith("Landroidx/compose/ui/graphics/drawscope/")})
 val scope=glideDraw.parameterTypes[0].toString()
 val getSize=requireExactlyOne("ProfileImages draw size",glideDraw.refs().filter{it.definingClass==scope&&it.parameterTypes.isEmpty()&&it.returnType=="J"}.distinctBy{it.toString()})
 val scopeSize=requireExactlyOne("ProfileImages size default method",cls(scope).methods.filter{it.toString()==getSize.toString()})
 val getContext=requireExactlyOne("ProfileImages draw context",scopeSize.refs().filter{it.definingClass==scope&&it.parameterTypes.isEmpty()&&it.returnType.startsWith("L")})
 val nativeCanvas=requireExactlyOne("ProfileImages Android Canvas implementation",all.filter{cl->cl.type.startsWith("Landroidx/compose/ui/graphics/")&&cl.fields.any{it.type=="Landroid/graphics/Canvas;"}&&cl.methods.any{m->m.refs().any{it.definingClass=="Landroid/graphics/Canvas;"&&it.name=="drawRect"}}})
 val nativeField=requireExactlyOne("ProfileImages native canvas field",nativeCanvas.fields.filter{it.type=="Landroid/graphics/Canvas;"})
 val canvasInterface=requireExactlyOne("ProfileImages Canvas interface",nativeCanvas.interfaces)
 val getCanvas=requireExactlyOne("ProfileImages context canvas getter",cls(getContext.returnType).methods.filter{it.parameterTypes.isEmpty()&&it.returnType==canvasInterface})
 val runtime=c.mutableClassDefBy(R)
 val imageFmt=formatter("MediaContentImage(mediaId=","Lcom/x/models/")
 val imageUrl=field(imageFmt,", imageUrl=")
 val modelUrl=newMethod("modelUrl",listOf(O),S)
 modelUrl.insertHook(0,relocateBranchTargets=false){val p=modelUrl.p0Register;instanceOf(0,p,imageFmt.definingClass);ifEqz(0,Target.Local("other"));checkCast(p,imageFmt.definingClass);iget(0,p,imageUrl);returnObject(0);label("other");constInt(0,0);returnObject(0)}
 runtime.methods.removeAll{it.name=="modelUrl"};runtime.methods.add(modelUrl)
 val guard=newMethod("guard",listOf(O,O),"Z")
 guard.insertHook(0,relocateBranchTargets=false){val p=guard.p0Register
  invokeStatic(methodReference("$R->neutralKind($O)I"),p);moveResult(4,"I");ifEqz(4,Target.Local("normal"))
  move(0,p+1,O);checkCast(0,scope);invokeInterface(methodReference(getSize.toString()),0);moveResult(2,"J")
  invokeInterface(methodReference(getContext.toString()),0);moveResult(0,O);invokeVirtual(methodReference(getCanvas.toString()),0);moveResult(0,O)
  instanceOf(1,0,nativeCanvas.type);ifEqz(1,Target.Local("hidden"));checkCast(0,nativeCanvas.type);iget(1,0,nativeField)
  invokeStatic(methodReference("$R->draw(Landroid/graphics/Canvas;JI)V"),1,2,3,4)
  label("hidden");constInt(0,1);returnValue(0);label("normal");constInt(0,0);returnValue(0)
 }
 runtime.methods.removeAll{it.name=="guard"};runtime.methods.add(guard)
 // Both guards execute before the original draw including placeholders/crossfades.
 val gd=mutable(glideDraw);if(gd.p0Register<1)throw PatchException("No draw scratch register")
 gd.insertHook(0,relocateBranchTargets=false){iget(0,gd.p0Register,glideRequest);iget(0,0,modelField);invokeStatic(methodReference("$R->guard($O$O)Z"),0,gd.p0Register+1);moveResult(0,"Z");ifEqz(0,Target.Local("original"));returnVoid();label("original")}
 val inputFmt=formatter("Input(imageLoader=","Lcoil3/compose/");val inputRequest=field(inputFmt,", request=")
 if(inputRequest.type!=requestFmt.definingClass)throw PatchException("Coil request contract mismatch")
 val coilPainter=requireExactlyOne("ProfileImages Coil painter",all.filter{it.type.startsWith("Lcoil3/compose/")&&it.superclass==painterBase&&it.fields.any{f->f.type==inputFmt.definingClass}})
 val inputField=requireExactlyOne("ProfileImages Coil painter input",coilPainter.fields.filter{it.type==inputFmt.definingClass})
 val cd=mutable(requireExactlyOne("ProfileImages Coil onDraw",coilPainter.methods.filter{it.returnType=="V"&&it.parameterTypes==listOf(scope)}))
 if(cd.p0Register<1)throw PatchException("No Coil draw scratch register")
 cd.insertHook(0,relocateBranchTargets=false){iget(0,cd.p0Register,inputField);ifEqz(0,Target.Local("original"));iget(0,0,inputRequest);iget(0,0,data);invokeStatic(methodReference("$R->guard($O$O)Z"),0,cd.p0Register+1);moveResult(0,"Z");ifEqz(0,Target.Local("original"));returnVoid();label("original")}
 // Semantic avatar UI boundary: core user renderer + size contract + shape + Glide painter.
 val avatar=mutable(requireExactlyOne("ProfileImages common avatar renderer",all.filter{it.type.startsWith("Lcom/x/ui/common/user/")}.flatMap{it.methods}.filter{m->stat(m)&&m.returnType=="V"&&m.parameterTypes.firstOrNull()==S&&m.parameterTypes.contains(COMPOSER)&&m.parameterTypes.any{it.startsWith("Landroidx/compose/foundation/shape/")}&&m.refs().any{it.returnType==glidePainter.type}}))
 avatar.insertHook(0,relocateBranchTargets=false){invokeStatic(methodReference("$R->avatarUrl($S)$S"),avatar.p0Register);moveResult(avatar.p0Register,S)}
 // Profile banner reads are changed ONLY in Compose UI, never in user storage/serializers.
 val profile=formatter("ProfileUser(id=","Lcom/x/models/")
 val banner=field(profile,", profileBackgroundPhotoUrl=");val photo=field(profile,", profileImageUrl=")
 val bannerUi=all.filter{it.type.startsWith("Lcom/x/profile/")}.flatMap{it.methods}.filter{m->m.parameterTypes.contains(COMPOSER)&&m.code().any{it.opcode==Opcode.IGET_OBJECT&&it.getReference<FieldReference>()?.toString()==banner.toString()}}
 if(bannerUi.size!=4)throw PatchException("ProfileImages expected four profile banner renderers, found $bannerUi")
 fun transformReads(method:Method,f:FieldReference,bridge:String){val m=mutable(method);val indices=m.implementation!!.instructions.withIndex().filter{it.value.opcode==Opcode.IGET_OBJECT&&it.value.getReference<FieldReference>()?.toString()==f.toString()}.map{it.index}
  for(i in indices.sortedDescending()){val r=(m.implementation!!.instructions[i] as OneRegisterInstruction).registerA;m.insertHook(i+1,relocateBranchTargets=false){invokeStatic(methodReference("$R->$bridge($S)$S"),r);moveResult(r,S)}}
 }
 for(m in bannerUi)transformReads(m,banner,"bannerUrl")
 // The dedicated profile viewer strips the avatar size suffix and creates MediaContentImage.
 val viewer=requireExactlyOne("ProfileImages dedicated profile viewer",all.filter{it.type.startsWith("Lcom/x/profile/header/")}.flatMap{it.methods}.filter{m->m.parameterTypes==listOf(S)&&m.returnType=="V"&&m.code().any{it.getReference<StringReference>()?.string=="_normal."}&&m.refs().any{it.name=="<init>"&&it.definingClass==imageFmt.definingClass}})
 val viewerCalls=all.flatMap{it.methods}.filter{m->m.refs().any{it.toString()==viewer.toString()}}
 if(viewerCalls.size!=2||viewerCalls.any{it.definingClass!=viewer.definingClass})throw PatchException("Unexpected profile viewer callers: $viewerCalls")
 val avatarClick=requireExactlyOne("ProfileImages avatar viewer source",viewerCalls.filter{m->m.code().any{it.opcode==Opcode.IGET_OBJECT&&it.getReference<FieldReference>()?.toString()==photo.toString()}})
 val bannerClick=requireExactlyOne("ProfileImages banner viewer source",viewerCalls.filter{m->m.code().any{it.opcode==Opcode.IGET_OBJECT&&it.getReference<FieldReference>()?.toString()==banner.toString()}})
 transformReads(avatarClick,photo,"avatarViewerUrl");transformReads(bannerClick,banner,"bannerViewerUrl")
 val vm=mutable(viewer);if(vm.p0Register<1)throw PatchException("No viewer scratch")
 vm.insertHook(0,relocateBranchTargets=false){invokeStatic(methodReference("$R->blockViewer($S)Z"),vm.p0Register+1);moveResult(0,"Z");ifEqz(0,Target.Local("normal"));returnVoid();label("normal")}
 println("ProfileImages: 4 banner renderers, 2 viewer arguments, dedicated viewer gate=$viewer; media URL=$imageUrl")
 println("ProfileImages: Glide load=$glideLoad; model=$modelField; painter=$glideDraw")
 println("ProfileImages: Coil ctor=$coilCtor; input=$inputField; painter=$cd")
 println("ProfileImages: avatar=$avatar; canvas=$getCanvas / $nativeField")
}
