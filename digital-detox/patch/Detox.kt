package fr.nesquik.patches.detox

import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.w3c.dom.Element

private const val ext = "fr.nesquik.detox."
private const val android = "http://schemas.android.com/apk/res/android"
private const val pkg = "com.urbandroid.ddc"
private const val ui = "Lfr/nesquik/detox/Ui;"
private const val material = "Lcom/google/android/material/dialog/MaterialAlertDialogBuilder;"

private fun Element.child(name: String, vararg attrs: Pair<String, String>): Element {
    val child = ownerDocument.createElement(name)
    for ((key, value) in attrs) child.setAttributeNS(android,"android:" + key,value)
    appendChild(child)
    return child
}
private val syncResources = resourcePatch(default = false) {
    execute {
        document("AndroidManifest.xml").use { xml ->
            val manifest = xml.documentElement
            val app = manifest.getElementsByTagName("application").item(0) as Element
            app.child("activity","name" to ext+"Devices","exported" to "false",
                "theme" to "@android:style/Theme.DeviceDefault.Settings")
            app.child("service","name" to ext+"Delivery","permission" to "android.permission.BIND_JOB_SERVICE","exported" to "true")
            val filter = app.child("receiver","name" to ext+"Push","exported" to "true").child("intent-filter")
            for (action in listOf("MESSAGE","NEW_ENDPOINT","UNREGISTERED","REGISTRATION_FAILED"))
                filter.child("action","name" to "org.unifiedpush.android.connector."+action)
            val queries = manifest.getElementsByTagName("queries").item(0) as? Element ?: manifest.child("queries")
            queries.child("package","name" to "io.heckel.ntfy")
        }
        for ((folder, title) in listOf("values" to "Device synchronization", "values-fr" to "Synchronisation des appareils")) {
            document("res/$folder/strings.xml").use { xml ->
                val label=xml.createElement("string")
                label.setAttribute("name","nesquik_detox_sync_title")
                label.textContent=title
                xml.documentElement.appendChild(label)
            }
        }
        for (directory in listOf("xml", "xml-v26")) {
            document("res/$directory/settings_main.xml").use { xml ->
                val root=xml.documentElement
                val prefs=root.getElementsByTagName("Preference")
                val template=(0 until prefs.length).map { prefs.item(it) as Element }
                    .single { it.getAttributeNS(android,"key")=="category_integration" }
                val option = xml.createElement(ext + "DevicesPreference")
                for (i in 0 until template.attributes.length) {
                    val attr = template.attributes.item(i)
                    option.setAttributeNS(attr.namespaceURI, attr.nodeName, attr.nodeValue)
                }
                option.setAttributeNS(android,"android:key","nesquik_detox_sync")
                option.setAttributeNS(android,"android:title","@string/nesquik_detox_sync_title")
                root.insertBefore(option,template.nextSibling)
            }
        }
    }
}

@Suppress("unused")
val crossDeviceDetoxSync = bytecodePatch(name="Cross-Device Detox Sync",
    description="Synchronize manually confirmed Digital Detox sessions with paired devices.", default=false) {
    compatibleWith(Compatibility(name="Digital Detox",packageName=pkg,apkFileType=ApkFileType.APK,targets=listOf(AppTarget(version="11.42"))))
    dependsOn(syncResources)
    extendWith("extensions/nesquik-detox.mpe")
    execute {
        val adapter=mutableClassDefBy("Lcom/urbandroid/ddc/adapter/ChallengeAdapter;")
        val launch=adapter.methods.single { method ->
            method.implementation?.instructions?.any { it.getReference<StringReference>()?.string=="Start challenge - NEW" }==true
        }
        val code=launch.implementation!!.instructions
        val listenerIndex=code.indices.single { code[it].getReference<MethodReference>()?.toString()==
            "$material->setNegativeButton(ILandroid/content/DialogInterface\$OnClickListener;)$material" }
        val show=code.indices.single { it>listenerIndex && code[it].getReference<MethodReference>()?.name=="show" }
        val builder=(code[show] as FiveRegisterInstruction).registerC
        launch.addInstructions(show,"invoke-static {v$builder}, $ui->launchOption($material)V")

        val positive=code.take(listenerIndex).last { it.getReference<MethodReference>()?.name=="setPositiveButton" } as FiveRegisterInstruction
        val listener=code.take(listenerIndex).last {
            val ref=it.getReference<MethodReference>()
            ref?.name=="<init>" && (it as? FiveRegisterInstruction)?.registerC==positive.registerE &&
                ref.definingClass.startsWith(adapter.type.dropLast(1)+"\$")
        }.getReference<MethodReference>()!!.definingClass
        val click=mutableClassDefBy(listener).methods.single { it.name=="onClick" }
        val ops=click.implementation!!.instructions
        val starts=ops.indices.filter {
            val ref=ops[it].getReference<MethodReference>()
            ref?.definingClass=="Lcom/urbandroid/ddc/service/ChallengeService;" && ref.name in listOf("start","startCustom")
        }
        if(starts.size!=2) throw PatchException("Expected both native manual launch paths.")
        for(i in starts.reversed()){
            val ctx=(ops[i] as FiveRegisterInstruction).registerC
            click.addInstructions(i+1,"invoke-static {v$ctx}, $ui->confirmed(Landroid/content/Context;)V")
        }
        click.addInstructions(0,"invoke-static {p1}, $ui->begin(Landroid/content/DialogInterface;)V")
        val resume=mutableClassDefBy("Lcom/urbandroid/ddc/activity/MainActivity;").methods.single { it.name=="onResume" }
        val r=resume.implementation!!.instructions
        val superCall=r.indices.single { r[it].opcode==Opcode.INVOKE_SUPER && r[it].getReference<MethodReference>()?.name=="onResume" }
        resume.addInstructions(superCall+1,"invoke-static/range {p0 .. p0}, $ui->resume(Landroid/app/Activity;)V")
    }
}
