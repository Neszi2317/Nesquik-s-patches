import com.android.apksig.ApkVerifier;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.reandroid.apk.ApkModule;
import java.io.File;
import java.util.*;
import java.util.zip.ZipFile;

class AuditPort {
    private static final String NEWX = "Lapp/morphe/extension/newx/";
    private static final String BLUR = NEWX + "mediablur/BlurRuntime;->";
    private static final String NAMES = NEWX + "anonymous/Names;->";
    private final Map<String, ClassDef> classes = new HashMap<>();
    private final Map<String, Method> methods = new HashMap<>();
    private final Map<String, List<String>> callers = new TreeMap<>();

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    private AuditPort(File apk) throws Exception {
        var dex = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames()) {
            for (ClassDef type : dex.getEntry(entry).getDexFile().getClasses()) {
                require(classes.put(type.getType(), type) == null, "Duplicate DEX class: " + type.getType());
                for (Method method : type.getMethods()) {
                    require(methods.put(method.toString(), method) == null, "Duplicate DEX method: " + method);
                }
            }
        }
        System.out.printf("%d DEX, %d classes uniques%n", dex.getDexEntryNames().size(), classes.size());
        for (Method method : methods.values()) {
            if (method.getImplementation() == null) continue;
            for (var instruction : method.getImplementation().getInstructions()) {
                if (!(instruction instanceof ReferenceInstruction ref) ||
                    !(ref.getReference() instanceof MethodReference target)) continue;
                String owner = target.getDefiningClass();
                if (!owner.startsWith(NEWX)) continue;
                String descriptor = target.toString();
                callers.computeIfAbsent(descriptor, ignored -> new ArrayList<>()).add(method.toString());
                if (isCustom(owner) && (instruction.getOpcode() == Opcode.INVOKE_STATIC ||
                    instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE)) {
                    require(methods.containsKey(descriptor), "Appel statique sans méthode: " + descriptor);
                }
            }
        }
    }

    private static boolean isCustom(String owner) {
        if (owner.startsWith(NEWX + "postfilter/Advanced") || owner.startsWith(NEWX + "advanced/")) return true;
        return List.of("anonymous/", "hidesensitive/", "profileimages/", "mediablur/").stream()
            .anyMatch(group -> owner.startsWith(NEWX + group));
    }

    private void callCount(String prefix, String from, int expected) {
        List<String> matches = callers.entrySet().stream().filter(e -> e.getKey().startsWith(prefix))
            .flatMap(e -> e.getValue().stream()).filter(caller -> caller.startsWith(from)).sorted().toList();
        require(matches.size() == expected, "Appels " + prefix + " depuis " + from + " : " + matches);
        System.out.printf("%s %s%n", prefix, matches);
    }

    private void hasCall(String prefix, String from) {
        require(callers.entrySet().stream().anyMatch(e -> e.getKey().startsWith(prefix)
                && e.getValue().stream().anyMatch(c -> c.startsWith(from))),
            "Appel manquant: " + prefix + " depuis " + from);
    }

    private void check() {
        String timeline = NEWX + "timeline/NewXTimelineFilter;->filterTimelineItems(";
        callCount(NEWX + "hidesensitive/SensitivePostFilter;->filter(", timeline, 1);
        callCount(NEWX + "postfilter/AdvancedFilter;->filter(", timeline, 1);
        callCount(NEWX + "postfilter/AdvancedFilter;->filterSuggestions(", "Lcom/x/search/query/", 3);
        callCount(NAMES + "renderAttribution(", "Lcom/x/", 2);
        callCount(NAMES + "display(", "Lcom/x/", 3);
        callCount(NAMES + "handle(", "Lcom/x/", 2);
        hasCall(NEWX + "profileimages/ProfileImages;->substitute(", "Lcoil3/request/");
        hasCall(NEWX + "profileimages/ProfileImages;->substitute(", "Lcom/bumptech/glide/");

        callCount(BLUR + "scope(", "Lcom/x/", 3);
        callCount(BLUR + "media(", "Lcom/x/ui/common/media/", 3);
        callCount(BLUR + "surface(", "Lcom/google/android/gms/internal/mlkit_vision_text_common/sd;", 1);
        callCount(BLUR + "headerVisible(", "Lcom/x/sensitivemedia/api/c;->pikoBlurOriginal_b(", 1);
        callCount(BLUR + "click(", "Lcom/x/sensitivemedia/api/c;->pikoBlurOriginal_b(", 1);
        callCount(BLUR + "icon(", "Lcom/x/sensitivemedia/api/c;->pikoBlurHeaderIcon(", 1);
        callCount(BLUR + "label(", "Lcom/x/sensitivemedia/api/c;->pikoBlurHeaderIcon(", 1);
        callCount(BLUR + "click(", "Lcom/x/urt/items/post/grok/", 0);

        for (String detail : List.of(
            "Landroidx/compose/runtime/i;->c(Landroidx/compose/runtime/h2;Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;I)V",
            "Landroidx/compose/runtime/i;->s(Ljava/lang/Object;)Landroidx/compose/runtime/w1;",
            "Landroidx/compose/runtime/o0;->m(Landroidx/compose/runtime/g2;)Ljava/lang/Object;",
            "Landroidx/compose/runtime/g0;-><init>(Lkotlin/jvm/functions/Function0;B)V",
            "Landroidx/compose/runtime/g0;->a(Ljava/lang/Object;)Landroidx/compose/runtime/h2;",
            "Landroidx/compose/ui/draw/j;->b(Landroidx/compose/ui/Modifier;FFLandroidx/compose/ui/graphics/c1;)Landroidx/compose/ui/Modifier;",
            "Landroidx/compose/ui/input/pointer/z;-><init>()V",
            "Landroidx/compose/ui/Modifier;->p0(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;",
            "Lcom/x/models/c5;->a()Ljava/lang/String;",
            "Lcom/x/models/v4;->a()Ljava/lang/String;",
            "Lcom/x/models/y4;->a()Ljava/lang/String;",
            "Lcom/x/navigation/fm;->s()J",
            "Lcom/x/icons/b;-><init>(IZ)V")) {
            require(methods.containsKey(detail), "ABI absente: " + detail);
        }
        for (String field : List.of(
            "Lcom/x/postdetail/n;->b:Lcom/x/navigation/fm;",
            "Lcom/x/postdetailsheet/c;->f:Lcom/x/postdetail/n;",
            "Lcom/x/urt/items/post/e6;->c:Lcom/x/models/h6;",
            "Lcom/x/models/h6;->a:J",
            "Landroidx/compose/ui/graphics/n0;->b:Landroidx/compose/ui/graphics/c1;",
            "Landroidx/compose/ui/input/pointer/z;->a:Lkotlin/jvm/functions/Function1;",
            "Landroidx/compose/ui/q;->a:Landroidx/compose/ui/q;",
            "Lkotlin/Unit;->a:Lkotlin/Unit;")) {
            ClassDef owner = classes.get(field.substring(0, field.indexOf("->")));
            boolean present = false;
            if (owner != null) for (var f : owner.getFields()) present |= f.toString().equals(field);
            require(present, "Champ ABI absent: " + field);
        }
    }

    private void checkPikoSettings() {
        String registry = "Lapp/morphe/extension/crimera/settings/SettingsRegistry;";
        Method load = methods.get(registry + "->load()V");
        require(load != null, "Registre des réglages Piko absent");
        Map<String, Integer> strings = new HashMap<>();
        int guards = 0;
        for (var instruction : load.getImplementation().getInstructions()) {
            if (!(instruction instanceof ReferenceInstruction reference)) continue;
            if (reference.getReference() instanceof StringReference text)
                strings.merge(text.getString(), 1, Integer::sum);
            if (reference.getReference().toString().equals(registry + "->isLoaded()Z"))
                guards++;
        }
        require(guards == 1, "Le catalogue Piko doit avoir un seul garde");
        for (String id : List.of("hide_all_sensitive_posts", "advanced_global_filter",
            "anonymous_names", "profile_images", "media_blur")) {
            require(strings.getOrDefault("newx.content." + id, 0) == 2,
                "Réglage non enregistré et configuré une seule fois : " + id);
        }
        require(strings.getOrDefault("newx.content.post_filtering", 0) == 2,
            "Le réglage officiel du filtre de mots-clés doit rester dans le registre");
        System.out.println("Catalogue Piko : un garde, cinq réglages personnels et le filtre officiel.");
    }

    public static void main(String[] args) throws Exception {
        File apk = new File(args[0]);
        var signing = new ApkVerifier.Builder(apk).build().verify();
        require(signing.isVerified(), "Signature APK invalide: " + signing.getAllErrors());
        System.out.println("Signature APK vérifiée");
        try (var zip = new ZipFile(apk)) {
            require(zip.getEntry("assets/piko-advanced-filter.json") != null, "Liste initiale absente");
        }
        var table = ApkModule.loadApkFile(apk).getTableBlock();
        String button = table.getResource(2131963716).getName();
        require(button.equals("post_option_ask_grok"), "Ressource inattendue pour le bouton Grok: " + button);
        System.out.println("Description du bouton Grok natif = " + button);
        for (String drawable : List.of("ic_piko_blur_off", "ic_piko_blur_on"))
            require(table.getResource("com.twitter.android", "drawable", drawable) != null, "Icône absente: " + drawable);
        for (String key : List.of("hide_all_sensitive_posts", "advanced_filter", "anonymous_names",
            "profile_images", "media_blur")) {
            for (String suffix : List.of("_title", "_summary")) {
                String name = "piko_newx_" + key + suffix;
                require(table.getResource("com.twitter.android", "string", name) != null,
                    "Libellé absent: " + name);
            }
        }
        AuditPort audit = new AuditPort(apk);
        audit.check();
        audit.checkPikoSettings();
        System.out.println("Audit statique des points d'injection et de leurs cibles réussi.");
    }
}
