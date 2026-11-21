/* Unofficial custom patch. Structural hooks adapted from Morphe Channel search / AuthHook.
 * GPL-3.0; see NOTICE. */
package app.nesquikos.patches
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*
private const val EXT="Lapp/nesquikos/playlist/PlaylistSearch;"
val playlistSearchPatch=bytecodePatch(name="YouTube – Search in Playlists",description="Experimental playlist title/channel search with authenticated browse and pagination. Device validation required.") {
 compatibleWith("com.google.android.youtube"("21.16.256"))
 extendWith("playlist-extension.dex")
 execute {
  val root=Fingerprint(returnType="Landroid/view/View;",parameters=listOf("Landroid/view/LayoutInflater;","Landroid/view/ViewGroup;","Landroid/os/Bundle;"),strings=listOf("Browse Fragment was given a navigation endpoint without browse data."))
  val m=root.method; val ins=m.implementation!!.instructions.toList()
  val error=ins.indexOfFirst{((it as? ReferenceInstruction)?.reference as? StringReference)?.string=="Browse Fragment was given a navigation endpoint without browse data."}
  val ei=(0 until error).last{val f=(ins[it] as? ReferenceInstruction)?.reference as? FieldReference;ins[it].opcode==Opcode.IGET_OBJECT && f?.definingClass==m.definingClass}
  val endpoint=(ins[ei] as ReferenceInstruction).reference as FieldReference
  val extension=(ins.drop(ei+1).first{it.opcode==Opcode.SGET_OBJECT} as ReferenceInstruction).reference as FieldReference
  val id=Fingerprint(accessFlags=listOf(AccessFlags.PUBLIC,AccessFlags.STATIC),returnType="Ljava/lang/String;",parameters=listOf(endpoint.type),filters=listOf(fieldAccess(opcode=Opcode.SGET_OBJECT,reference=extension))).method
  val bridge=Fingerprint(definingClass="Lapp/nesquikos/playlist/NativeBridge;",name="browseId").method
  bridge.removeInstructions(0,bridge.implementation!!.instructions.size)
  bridge.addInstructions(0,"""
   check-cast p0, ${m.definingClass}
   iget-object p0, p0, $endpoint
   invoke-static {p0}, $id
   move-result-object p0
   return-object p0
  """)
  ins.indices.reversed().filter{ins[it].opcode==Opcode.RETURN_OBJECT}.forEach{i->val r=(ins[i] as OneRegisterInstruction).registerA;m.addInstructions(i,"invoke-static/range {v$r .. v$r}, $EXT->onRoot(Landroid/view/View;)V")}
  m.addInstructions(0,"invoke-static/range {p0 .. p0}, $EXT->onFragment(Ljava/lang/Object;)V")
  val setter=Fingerprint(definingClass=m.definingClass,returnType="V",parameters=listOf(endpoint.type),filters=listOf(fieldAccess(opcode=Opcode.IPUT_OBJECT,reference=endpoint))).method
  val si=setter.implementation!!.instructions.toList()
  si.indices.reversed().filter{si[it].opcode==Opcode.RETURN_VOID}.forEach{setter.addInstructions(it,"invoke-static/range {p0 .. p0}, $EXT->changed(Ljava/lang/Object;)V")}
  val request=Fingerprint(accessFlags=listOf(AccessFlags.PUBLIC,AccessFlags.STATIC),returnType="Lorg/chromium/net/UrlRequest",filters=listOf(
   methodCall(opcode=Opcode.INVOKE_VIRTUAL,name="newUrlRequestBuilder",parameters=listOf("Ljava/lang/String;","Lorg/chromium/net/UrlRequest\$Callback;","Ljava/util/concurrent/Executor;")),
   methodCall(opcode=Opcode.INVOKE_INTERFACE,smali="Ljava/util/Map;->entrySet()Ljava/util/Set;"),methodCall(opcode=Opcode.INVOKE_VIRTUAL,name="setPriority")))
  val b=request.instructionMatches[0];val map=request.instructionMatches[1];val ur=(b.instruction as FiveRegisterInstruction).registerD;val mr=(map.instruction as FiveRegisterInstruction).registerC
  require(ur<16 && mr<16)
  request.method.addInstructions(b.index,"invoke-static {v$ur,v$mr}, Lapp/nesquikos/playlist/PlaylistSession;->capture(Ljava/lang/String;Ljava/util/Map;)V")
  val account=Fingerprint(name="toString",strings=listOf(", getPageId=",", isIncognito="));val ai=account.method.implementation!!.instructions.toList()
  fun field(label:String,op:Opcode):FieldReference{val i=ai.indexOfFirst{((it as? ReferenceInstruction)?.reference as? StringReference)?.string==label};require(i>=0);return(ai.drop(i+1).take(4).first{it.opcode==op} as ReferenceInstruction).reference as FieldReference}
  val pf=field(", getPageId=",Opcode.IGET_OBJECT);val inf=field(", isIncognito=",Opcode.IGET_BOOLEAN);var count=0
  account.classDef.methods.filter{it.parameterTypes.isEmpty() && it.implementation!=null}.forEach{g->
   val gi=g.implementation!!.instructions.toList()
   if(gi.size<=24 && (g.returnType=="Z"||g.returnType=="Ljava/lang/String;"))gi.indices.reversed().forEach{i->
    val f=(gi[i] as? ReferenceInstruction)?.reference as? FieldReference
    val cb=if(f==pf && gi[i].opcode==Opcode.IGET_OBJECT)"page(Ljava/lang/String;)V" else if(f==inf && gi[i].opcode==Opcode.IGET_BOOLEAN)"incognito(Z)V" else null
    if(cb!=null){val r=(gi[i] as TwoRegisterInstruction).registerA;g.addInstructions(i+1,"invoke-static/range {v$r .. v$r}, Lapp/nesquikos/playlist/PlaylistSession;->$cb");count++}
   }
  }
  require(count>=3)
  println("Playlist root: $m; endpoint: $endpoint; browseId: $id; session: ${request.method}; identity hooks: $count")
 }
}
