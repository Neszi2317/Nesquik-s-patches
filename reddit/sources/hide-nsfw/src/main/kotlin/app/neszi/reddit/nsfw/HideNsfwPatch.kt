package app.neszi.reddit.nsfw

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.*
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.function.Supplier

private const val FILTER = "Lapp/neszi/reddit/nsfw/NsfwFilter;"
private const val SOURCES = "Lapp/neszi/reddit/nsfw/NsfwFilter\$Sources;"
private const val LINK = "Lcom/reddit/domain/model/Link;"
private object Resources

private fun method(owner: String, name: String, params: List<String>, ret: String, registers: Int, code: String, static: Boolean = false) =
    ImmutableMethod(owner, name, params.map { ImmutableMethodParameter(it, null, null) }, ret,
        AccessFlags.PUBLIC.value or (if (static) AccessFlags.STATIC.value else 0), null, null,
        MutableMethodImplementation(registers)).toMutable().apply { addInstructions(0, code) }

@Suppress("unused")
val hideNsfwPostsPatch = bytecodePatch(
    name = "Hide NSFW posts",
    description = "Removes posts marked 18+ from native feed lists. Independent, always active when included. Reddit 2026.47.0.",
    default = false
) {
    compatibleWith(Compatibility(packageName="com.reddit.frontpage", name="Reddit",
        targets=listOf(AppTarget(version="2026.47.0", isExperimental=true))))
    extendWith(Supplier { Resources::class.java.getResourceAsStream("/nsfw-extension.dex")
        ?: throw PatchException("Missing runtime DEX") })
    finalize {
        val all = mutableListOf<ClassDef>()
        classDefForEach { all.add(it) }
        val byType = all.associateBy { it.type }
        val enums = setOf("Lcom/reddit/feeds/model/IndicatorType;", "Lcom/reddit/type/CellIndicatorType;")
        for (type in enums) if (byType[type]?.fields?.none { it.name == "NSFW" } != false)
            throw PatchException("Native NSFW enum changed: $type")
        if (byType[LINK]?.methods?.none { it.name == "getOver18" && it.parameterTypes.isEmpty() && it.returnType == "Z" } != false)
            throw PatchException("Native Link.getOver18()Z missing")

        fun strings(c: ClassDef) = c.methods.filter { it.name == "toString" }.flatMap { m ->
            m.implementation?.instructions?.mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }?.toList() ?: emptyList()
        }
        fun descendant(c: ClassDef): Boolean {
            var cur: ClassDef? = c
            val seen = mutableSetOf<String>()
            while (cur != null && seen.add(cur.type)) {
                if (cur.type == "Lmtj;") return true
                cur = byType[cur.superclass]
            }
            return false
        }
        val feedNodes = all.filter { descendant(it) }.map { it.type }.toSet()
        val sectionNodes = all.filter { c -> strings(c).any { it.contains("Section(") } }.map { it.type }.toSet()
        val enumHolders = all.filter { c -> c.fields.any { it.type in enums } }.map { it.type }.toSet()
        val candidates = feedNodes + sectionNodes + enumHolders + LINK
        val containers = setOf("Ljava/util/List;", "Ljava/util/Collection;", "Ljava/lang/Iterable;", "Lkpq;", "Lulc;")
        var bridges = 0
        for (type in candidates.sorted()) {
            val original = byType[type] ?: continue
            if (original.accessFlags and AccessFlags.INTERFACE.value != 0) continue
            val fields = original.fields.filter {
                it.accessFlags and AccessFlags.STATIC.value == 0 &&
                    (it.type in candidates || it.type in enums || it.type in containers)
            }.toList()
            if (fields.isEmpty()) continue
            val c = mutableClassDefBy(type)
            if (c.methods.any { it.name == "neszi_nsfwSources" }) throw PatchException("NSFW patch already present; patch the original APKM")
            c.interfaces.add(SOURCES)
            val code = buildString {
                appendLine("const/16 v0, ${fields.size}")
                appendLine("new-array v0, v0, [Ljava/lang/Object;")
                fields.forEachIndexed { index, field ->
                    appendLine("const/16 v1, $index")
                    appendLine("iget-object v2, p0, $field")
                    appendLine("aput-object v2, v0, v1")
                }
                appendLine("return-object v0")
            }
            c.methods.add(method(type, "neszi_nsfwSources", emptyList(), "[Ljava/lang/Object;", 4, code))
            bridges++
        }
        // Old native Listing children: remove whole Link objects, keep pagination fields.
        val listing = mutableClassDefBy("Lcom/reddit/domain/model/listing/Listing;")
        val listingCtor = listing.methods.single { it.name == "<init>" && it.parameterTypes == listOf(
            "Ljava/util/List;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Ljava/util/List;") }
        listingCtor.addInstructions(0, """
            invoke-static/range { p1 .. p1 }, $FILTER->filter(Ljava/util/List;)Ljava/util/List;
            move-result-object p1
        """)
        // Modern FeedPage raw data: full constructor shared by default/copy paths.
        val page = classDefByStrings("FeedPage(data=").singleOrNull()
            ?: throw PatchException("FeedPage signature changed")
        val pageCtor = mutableClassDefBy(page.type).methods.single { it.name == "<init>" && it.parameterTypes.size == 7 && it.parameterTypes.first() == "Ljava/util/List;" }
        pageCtor.addInstructions(0, """
            invoke-static/range { p1 .. p1 }, $FILTER->filter(Ljava/util/List;)Ljava/util/List;
            move-result-object p1
        """)
        // Final native sections, including restored/updated view states. Preserve persistent-list type.
        val builder = classDefBy("Lvcf;").methods.singleOrNull { it.name == "N0" && it.parameterTypes == listOf("Ljava/lang/Iterable;") && it.returnType == "Lkpq;" }
            ?: throw PatchException("Persistent list builder missing")
        val runtime = mutableClassDefBy(FILTER)
        runtime.methods.add(method(FILTER, "filterPersistent", listOf("Lkpq;"), "Lkpq;", 2, """
            invoke-static/range { p0 .. p0 }, $FILTER->filter(Ljava/util/List;)Ljava/util/List;
            move-result-object v0
            if-ne v0, p0, :changed
            return-object p0
            :changed
            invoke-static { v0 }, $builder
            move-result-object v0
            return-object v0
        """, static=true))
        val feed = classDefByStrings("Feed(sections=").singleOrNull()
            ?: throw PatchException("Feed view state missing")
        val feedCtor = mutableClassDefBy(feed.type).methods.single { it.name == "<init>" && it.parameterTypes.size == 12 && it.parameterTypes.first() == "Lkpq;" }
        feedCtor.addInstructions(0, """
            invoke-static/range { p1 .. p1 }, $FILTER->filterPersistent(Lkpq;)Lkpq;
            move-result-object p1
        """)
        println("Hide NSFW posts: $bridges typed native bridges; Listing.children + ${page.type}.data + ${feed.type}.sections filtered; pagination keys untouched")
    }
}
