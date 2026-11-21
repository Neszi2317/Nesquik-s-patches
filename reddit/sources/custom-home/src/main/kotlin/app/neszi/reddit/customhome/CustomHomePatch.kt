package app.neszi.reddit.customhome

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*
import java.util.function.Supplier

private const val EXT = "Lapp/neszi/reddit/customhome/CustomHome;"
private const val HOME = "Lcom/reddit/feeds/home/impl/ui/HomeFeedScreen;"
private const val NAV = "Lcom/reddit/navstack/v;"
private object ExtensionResource

@Suppress("unused")
val customHomePatch = bytecodePatch(
    name = "Custom Home & Disable Main Feed",
    description = "Runtime Home subreddit and removal of the Home navigation button. Reddit 2026.47.0; Android testing required.",
    default = false
) {
    compatibleWith(Compatibility(packageName = "com.reddit.frontpage", name = "Reddit", targets = listOf(AppTarget(version = "2026.47.0", isExperimental = true))))
    extendWith(Supplier { ExtensionResource::class.java.getResourceAsStream("/custom-home-extension.dex")
        ?: throw PatchException("Missing custom-home-extension.dex") })

    // All official patches execute first, regardless of alphabetical order or source bundle.
    // Do not overwrite classes from the main Morphe extension.
    finalize {
        val settings = mutableClassDefByOrNull("Lapp/morphe/extension/reddit/settings/preference/RedditPreferenceFragment;")
            ?: throw PatchException("Select an official Reddit patch that includes Morphe Settings (e.g. Hide navigation buttons) in this same patching run.")
        val settingsInit = settings.methods.singleOrNull { it.name == "initialize" && it.parameterTypes.isEmpty() }
            ?: throw PatchException("Unsupported Morphe settings fragment")
        val app = mutableClassDefBy("Lcom/reddit/frontpage/FrontpageApplication;")
        val appInit = app.methods.single { it.name == "onCreate" && it.parameterTypes.isEmpty() }
        appInit.addInstructions(0, "invoke-static/range { p0 .. p0 }, $EXT->init(Landroid/content/Context;)V")
        val mainInit = mutableClassDefBy("Lcom/reddit/launch/main/MainActivity;").methods.single {
            it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;")
        }
        mainInit.addInstructions(0, "invoke-static/range { p0 .. p0 }, $EXT->beginActivity(Landroid/content/Context;)V")
        val homeCtor = mutableClassDefBy("Lcom/reddit/feedslegacy/switcher/impl/homepager/compose/HomePagerScreen;").methods.single {
            it.name == "<init>" && it.parameterTypes == listOf("Landroid/os/Bundle;")
        }
        homeCtor.addInstructions(0, """
            invoke-static/range { p1 .. p1 }, $EXT->prepareHomeArgs(Landroid/os/Bundle;)Landroid/os/Bundle;
            move-result-object p1
        """)
        if (settings.methods.any { it.name == "neszi_customHomeOriginalInitialize" })
            throw PatchException("Custom Home already applied; use the original Reddit APKM")
        val (settingsWrapper, settingsOriginalBody) = wrapSettingsInitializer(settingsInit)
        settings.methods.remove(settingsInit)
        settings.methods.add(settingsOriginalBody)
        settings.methods.add(settingsWrapper)

        // Verify the native community screen's public Bundle constructor exists.
        val community = classDefBy("Lcom/reddit/screens/listing/compose/SubredditFeedScreen;")
        if (community.methods.none { it.name == "<init>" && it.parameterTypes == listOf("Landroid/os/Bundle;") })
            throw PatchException("Native subreddit Bundle constructor missing")
        if (classDefBy(NAV).fields.none { it.name == "c" && it.type == "Landroid/os/Bundle;" })
            throw PatchException("Unsupported screen arguments field")

        // Resolve by semantic strings and an exact Home constructor/factory reference.
        val factoryClasses = classDefByStrings("homeTabScreenFactory")
        val factoryCandidates = factoryClasses.flatMap { cls -> cls.methods.filter { m ->
            m.returnType == "Ljava/lang/Object;" && m.implementation?.instructions?.any {
                val r = (it as? ReferenceInstruction)?.reference as? MethodReference
                r?.returnType == HOME
            } == true
        } }
        val factory = factoryCandidates.singleOrNull() ?: throw PatchException("Expected one Home screen factory, found ${factoryCandidates.size}")
        val factoryMethod = mutableClassDefBy(factory.definingClass).methods.single { it.name == factory.name && it.parameterTypes == factory.parameterTypes }
        val factoryInstructions = factoryMethod.implementation!!.instructions.toList()
        val analytics = factoryInstructions.withIndex().single { (_, i) ->
            val r = (i as? ReferenceInstruction)?.reference as? MethodReference
            r?.name == "G0" && r.parameterTypes == listOf("Lcom/reddit/analytics/deeplink/DeepLinkAnalytics;")
        }
        val screenReg = (analytics.value as FiveRegisterInstruction).registerC
        require(screenReg < 16)
        factoryMethod.addInstructions(analytics.index + 1, """
            invoke-static { v$screenReg }, $EXT->replaceFactoryResult(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v$screenReg
        """)

        // Native modern NavEntry constructor; covers navigation and decoded restored entries.
        val entryClass = classDefByStrings("NavEntry(").singleOrNull { c ->
            c.fields.any { it.type == NAV } && c.methods.any { it.name == "<init>" && it.parameterTypes.firstOrNull() == NAV }
        } ?: throw PatchException("NavEntry signature changed")
        val entryCtor = mutableClassDefBy(entryClass.type).methods.single {
            it.name == "<init>" && it.parameterTypes == listOf(NAV, "Z", "La500;", "Ljava/lang/String;")
        }
        entryCtor.addInstructions(0, """
            invoke-static/range { p1 .. p1 }, $EXT->replaceEntry($NAV)$NAV
            move-result-object p1
        """)

        // Same native list-building boundary as Morphe Hide navigation buttons.
        val nav = mutableClassDefBy("Lcom/reddit/launch/bottomnav/BottomNavScreen;")
        val listMethods = nav.methods.filter { m -> m.implementation?.instructions?.any {
            val r = (it as? ReferenceInstruction)?.reference as? FieldReference
            r?.type == "Lcom/reddit/launch/bottomnav/BottomNavTab;" && it.opcode == Opcode.IGET_OBJECT
        } == true && m.implementation!!.instructions.any {
            val r = (it as? ReferenceInstruction)?.reference as? MethodReference
            r?.definingClass == "Ljava/util/Iterator;" && r.name == "hasNext"
        } }
        val listMethod = listMethods.singleOrNull { it.parameterTypes == listOf("Lkpq;", "Lcom/reddit/launch/usecase/BottomNavTabListUseCaseParams;") && it.returnType == "Lkpq;" }
            ?: throw PatchException("2026.47.0 bottom navigation list signature changed")
        val insns = listMethod.implementation!!.instructions.toList()
        val enumLoad = insns.withIndex().first { (_, i) ->
            val r = (i as? ReferenceInstruction)?.reference as? FieldReference
            i.opcode == Opcode.IGET_OBJECT && r?.type == "Lcom/reddit/launch/bottomnav/BottomNavTab;"
        }
        val enumRegister = (enumLoad.value as TwoRegisterInstruction).registerA
        // v3 is null immediately before this boundary, and remains live as a null callback.
        // Assert the exact target method, restore null before continuing; no register expansion.
        require(insns.take(enumLoad.index).any { i -> i.opcode == Opcode.CONST_4 && (i as? OneRegisterInstruction)?.registerA == 3 && (i as? NarrowLiteralInstruction)?.narrowLiteral == 0 })
        require(enumRegister != 3 && enumRegister < 16)
        val loop = insns.first { i ->
            val r = (i as? ReferenceInstruction)?.reference as? MethodReference
            r?.definingClass == "Ljava/util/Iterator;" && r.name == "hasNext"
        }
        listMethod.addInstructionsWithLabels(enumLoad.index + 1, """
            invoke-static { v$enumRegister }, $EXT->hideHome(Ljava/lang/Enum;)Z
            move-result v3
            if-nez v3, :neszi_skip_home
            const/4 v3, 0x0
        """, ExternalLabel("neszi_skip_home", loop))
        println("Custom Home: settings + app init + ${factory.definingClass} factory + ${entryClass.type} NavEntry + native tab list injected")
    }
}
