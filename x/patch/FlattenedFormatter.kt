package app.crimera.patches.common.semantic

import app.crimera.patches.common.requireExactlyOne
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.*
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*

/** Proves label/value association across straight-line R8 StringBuilder helpers.
 * Unlike adjacency scans, carries a trailing label into the next helper invocation.
 * Rejects branches, exception handlers, ambiguous calls and unsupported formatting.
 */
fun Method.fieldForFlattenedFormatterLabel(label:String,classByType:(String)->ClassDef?):FieldReference {
    class Builder(val values:MutableList<Any> = mutableListOf())
    val unknown=Any();val receiver=Any()
    fun evaluate(method:Method,args:List<Any>,depth:Int):Any {
        val impl=method.implementation?:throw PatchException("No formatter body: $method")
        if(depth>8||impl.instructions.count()>512||impl.tryBlocks.isNotEmpty()||impl.instructions.any{it is OffsetInstruction})
            throw PatchException("Formatter must be bounded and straight-line: $method")
        val regs=mutableMapOf<Int,Any>();var p=method.p0Register;var a=0
        if(!AccessFlags.STATIC.isSet(method.accessFlags))regs[p++]=args.getOrElse(a++){unknown}
        for(type in method.parameterTypes){regs[p]=args.getOrElse(a){unknown};val w=if(type.toString() in setOf("J","D"))2 else 1;p+=w;a+=w}
        var pending:Any=unknown
        for(ins in impl.instructions){
            val r=(ins as? OneRegisterInstruction)?.registerA
            val field=ins.getReference<FieldReference>();val str=ins.getReference<StringReference>();val ref=ins.getReference<MethodReference>()
            when {
                ins.opcode==Opcode.NEW_INSTANCE -> {if(r!=null)regs[r]=if(ins.getReference<TypeReference>()?.type=="Ljava/lang/StringBuilder;")Builder() else unknown}
                str!=null -> {if(r!=null)regs[r]=str.string}
                field!=null&&ins.opcode in setOf(Opcode.IGET_OBJECT,Opcode.IGET,Opcode.IGET_BOOLEAN,Opcode.IGET_WIDE,Opcode.IGET_BYTE,Opcode.IGET_SHORT,Opcode.IGET_CHAR) -> {
                    val read=ins as TwoRegisterInstruction;regs[read.registerA]=if(regs[read.registerB]===receiver)field else unknown
                }
                ins.opcode in setOf(Opcode.MOVE_OBJECT,Opcode.MOVE_OBJECT_FROM16,Opcode.MOVE_OBJECT_16,Opcode.MOVE,Opcode.MOVE_FROM16,Opcode.MOVE_16,Opcode.MOVE_WIDE,Opcode.MOVE_WIDE_FROM16,Opcode.MOVE_WIDE_16)->{val move=ins as TwoRegisterInstruction;regs[move.registerA]=regs[move.registerB]?:unknown}
                ins.opcode in setOf(Opcode.MOVE_RESULT,Opcode.MOVE_RESULT_OBJECT,Opcode.MOVE_RESULT_WIDE)->{regs[r!!]=pending;pending=unknown}
                ref!=null -> {
                    val values=ins.registersUsed.map{regs[it]?:unknown};pending=unknown
                    if(ref.definingClass=="Ljava/lang/StringBuilder;"){
                        val builder=values.firstOrNull() as? Builder?:throw PatchException("Unknown formatter builder: $method / $ref")
                        when(ref.name){"<init>"->{if(ref.parameterTypes==listOf("Ljava/lang/String;"))builder.values.add(values[1])}
                            "append"->{builder.values.add(values.getOrElse(1){unknown});pending=builder}
                            "toString"->pending=builder
                            else->throw PatchException("Unsupported builder operation: $ref")}
                    }else if(ins.opcode in setOf(Opcode.INVOKE_STATIC,Opcode.INVOKE_STATIC_RANGE)&&
                        (ref.returnType=="Ljava/lang/StringBuilder;"||ref.parameterTypes.contains("Ljava/lang/StringBuilder;")||ref.returnType=="Ljava/lang/String;"&&values.any{it is String}&&values.any{it is FieldReference})){
                        val helper=requireExactlyOne("formatter helper $ref",classByType(ref.definingClass)?.methods?.filter{it.toString()==ref.toString()}.orEmpty())
                        pending=evaluate(helper,values,depth+1)
                    }else if(values.any{it is Builder})throw PatchException("Unproven formatter consumer: $ref")
                }
                ins.opcode==Opcode.RETURN_OBJECT -> return regs[r]?:unknown
                ins.opcode==Opcode.RETURN_VOID -> return unknown
                ins.opcode==Opcode.NOP || ins.opcode==Opcode.CHECK_CAST -> Unit
                ins.opcode.setsRegister() -> {if(r!=null)regs[r]=unknown}
                else->throw PatchException("Unsupported formatter opcode: ${ins.opcode} in $method")
            }
        }
        return unknown
    }
    val tokens=(evaluate(this,listOf(receiver),0) as? Builder)?.values?:throw PatchException("No proven formatted result: $this")
    val candidates=tokens.indices.filter{tokens[it]==label}.mapNotNull{tokens.getOrNull(it+1) as? FieldReference}.filter{it.definingClass==definingClass}
    return requireExactlyOne("flattened formatter field '$label' in $this",candidates.distinctBy{it.toString()})
}
