import com.android.apksig.ApkVerifier;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

class VerifyApk {
    private static final String OFFICIAL = "Lapp/morphe/extension/youtube/patches/PlaylistSearchPatch;";
    private static final String DIALOG = "Lfr/nesquik/playlistdate/PlaylistDateDialog;";
    private static final String DATED = "Lapp/morphe/extension/youtube/patches/utils/requests/DatedPlaylistSearch";
    private static final String PICKER = "Lfr/nesquik/playlistdate/TakeoutPicker;";
    private static final String STORE = "Lfr/nesquik/playlistdate/WatchLaterDates;";

    public static void main(String[] args) throws Exception {
        File apk = new File(args[0]);
        var signature = new ApkVerifier.Builder(apk).build().verify();
        if (!signature.isVerified()) throw new AssertionError(signature.getErrors());

        var container = DexFileFactory.loadDexContainer(apk, Opcodes.forApi(35));
        Map<String, ClassDef> classes = new HashMap<>();
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef cls : container.getEntry(entry).getDexFile().getClasses()) {
                if (classes.put(cls.getType(), cls) != null) {
                    throw new AssertionError("Duplicate class " + cls.getType());
                }
            }
        }
        int menuHooks = 0;
        int dialogHooks = 0;
        int nativeConfirmations = 0;
        int morpheConfirmations = 0;
        int morpheBodies = 0;
        int takeoutStores = 0;
        int linkedReferences = 0;
        for (ClassDef cls : classes.values()) {
            boolean module = cls.getType().startsWith("Lfr/nesquik/playlistdate/")
                    || cls.getType().startsWith(DATED);
            for (Method method : cls.getMethods()) {
                if (method.getImplementation() == null) continue;
                for (var instruction : method.getImplementation().getInstructions()) {
                    if (!(instruction instanceof ReferenceInstruction op)) continue;
                    if (op.getReference() instanceof MethodReference ref) {
                        if (cls.getType().equals(PICKER)) {
                            if (ref.getDefiningClass().startsWith("Lapp/morphe/extension/")
                                    && !ref.getDefiningClass().equals("Lapp/morphe/extension/shared/Utils;")
                                    && !ref.getDefiningClass().equals("Lapp/morphe/extension/shared/Logger;")
                                    && !ref.getDefiningClass().equals("Lapp/morphe/extension/shared/innertube/utils/AuthUtils;")) {
                                throw new AssertionError("Unexpected Morphe dependency in Takeout picker " + ref);
                            }
                            if (ref.getDefiningClass().equals(STORE) && ref.getName().equals("importDates")) {
                                takeoutStores++;
                            }
                        }
                        if (ref.getDefiningClass().equals(OFFICIAL) && ref.getName().equals("addSearchMenuItem")) {
                            menuHooks++;
                        }
                        if (ref.getDefiningClass().equals(DIALOG) && ref.getName().equals("show")) {
                            if (!cls.getType().equals(OFFICIAL) || !method.getName().equals("showSearchDialog")) {
                                throw new AssertionError("Unexpected custom dialog caller " + method);
                            }
                            dialogHooks++;
                        }
                        if (ref.getDefiningClass().equals("Lfr/nesquik/playlistdate/WatchLaterEvents;")) {
                            switch (ref.getName()) {
                                case "nativeResponse" -> {
                                    if (!method.toString().equals("Lalcm;->n(Laksp;Ljava/lang/Object;)V")) {
                                        throw new AssertionError("Unexpected native playlist confirmation caller " + method);
                                    }
                                    nativeConfirmations++;
                                }
                                case "morpheResponse" -> morpheConfirmations++;
                                case "morpheAddBody" -> morpheBodies++;
                            }
                        }
                        if (module && ref.getDefiningClass().startsWith("Lapp/morphe/extension/")) {
                            verifyMethod(classes, ref);
                            linkedReferences++;
                        }
                    } else if (module && op.getReference() instanceof FieldReference ref
                            && ref.getDefiningClass().startsWith("Lapp/morphe/extension/")) {
                        verifyField(classes, ref);
                        linkedReferences++;
                    }
                }
            }
        }
        if (menuHooks != 1 || dialogHooks != 1 || linkedReferences == 0) {
            throw new AssertionError("Menu hooks " + menuHooks + ", custom dialog hooks " + dialogHooks
                    + ", official runtime references " + linkedReferences);
        }
        if (nativeConfirmations != 1 || morpheConfirmations != 3 || morpheBodies != 2) {
            throw new AssertionError("Watch later hooks: " + nativeConfirmations + " / " + morpheConfirmations + " / " + morpheBodies);
        }
        if (takeoutStores != 1) throw new AssertionError("Unexpected Takeout store paths: " + takeoutStores);
        System.out.println("Watch later: one native completion hook and two Morphe request paths.");
        System.out.println("Takeout picker writes to the local store without invoking the playlist client.");
        System.out.println("APK signature valid. One playlist toolbar hook and one dialog redirection.");
        System.out.println(linkedReferences + " official runtime references resolved. No duplicate Dex classes.");
    }

    private static void verifyMethod(Map<String, ClassDef> classes, MethodReference ref) {
        for (ClassDef cls = classes.get(ref.getDefiningClass()); cls != null; cls = classes.get(cls.getSuperclass())) {
            if ("Ljava/lang/Enum;".equals(cls.getSuperclass())
                    && ref.getName().equals("ordinal") && ref.getReturnType().equals("I")
                    && ref.getParameterTypes().isEmpty()) return;
            for (var method : cls.getMethods()) {
                if (method.getName().equals(ref.getName())
                        && method.getReturnType().equals(ref.getReturnType())
                        && method.getParameterTypes().equals(ref.getParameterTypes())) return;
            }
        }
        throw new AssertionError("Missing method " + ref);
    }

    private static void verifyField(Map<String, ClassDef> classes, FieldReference ref) {
        for (ClassDef cls = classes.get(ref.getDefiningClass()); cls != null; cls = classes.get(cls.getSuperclass())) {
            for (var field : cls.getFields()) {
                if (field.getName().equals(ref.getName()) && field.getType().equals(ref.getType())) return;
            }
        }
        throw new AssertionError("Missing field " + ref);
    }
}
