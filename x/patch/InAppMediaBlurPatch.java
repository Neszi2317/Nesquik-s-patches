package app.crimera.patches.newx.misc.mediablur;

import com.nesquik.patches.x.PikoIntegrationKt;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public final class InAppMediaBlurPatch {
    private static final String R = "Lapp/morphe/extension/newx/mediablur/BlurRuntime;";
    private static final String COMPOSER = "Landroidx/compose/runtime/Composer;";
    private static final String MODIFIER = "Landroidx/compose/ui/Modifier;";
    private static final String CLICK = "Lkotlin/jvm/functions/Function0;";
    private static final String ICON = "Lcom/x/icons/b;";
    private static final String OBJECT = "Ljava/lang/Object;";
    private static final String TEXT = "Ljava/lang/String;";
    private static final InstructionExtensions I = InstructionExtensions.INSTANCE;

    // The wrappers deliberately share the exact ABI with BlurRuntime. Resolve all entry points
    // and validate the native branch/data flow before replacing any callback.
    private static final String DETAIL = "Lcom/x/postdetail/c;->d(Lcom/x/compose/core/l2;" +
        "Lcom/x/postdetail/n;" + MODIFIER + CLICK + COMPOSER + "II)V";
    private static final String SHEET = "Lcom/x/postdetailsheet/a;->a(Lcom/x/postdetailsheet/c;" +
        "Ldev/chrisbanes/haze/a1;Lcom/x/photoeditor/tools/collage/i;" + MODIFIER + COMPOSER + "I)V";
    private static final String HEADER = "Lcom/x/sensitivemedia/api/c;->b(Lcom/x/urt/items/post/e6;" +
        "Landroidx/compose/foundation/layout/c3;Lcom/x/urt/items/post/k6;" + CLICK + MODIFIER + COMPOSER + "I)V";
    private static final String POST_BODY = "Lcom/x/urt/items/post/g;->a(Lcom/x/urt/items/post/e6;" +
        "Landroidx/compose/foundation/layout/c3;" + MODIFIER + COMPOSER + "I)V";
    private static final String IMAGE = "Lcom/x/ui/common/media/a;->a(Lcom/x/models/v4;" +
        "Lcom/x/ui/common/media/u1;Landroidx/compose/ui/layout/r;" + MODIFIER + COMPOSER + "I)V";
    private static final String VIDEO = "Lcom/x/ui/common/media/i1;->d(Lcom/x/models/c5;" +
        "Lcom/x/models/media/r3;ZLkotlin/jvm/functions/Function8;Lkotlin/jvm/functions/Function6;" +
        MODIFIER + TEXT + "ZZLandroidx/compose/ui/layout/r;Lcom/x/ui/common/media/w1;" +
        "Lcom/x/ui/common/media/attribution/c;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function3;" +
        COMPOSER + "III)V";
    private static final String GIF = "Lcom/x/ui/common/media/h2;->a(Lcom/x/models/y4;" +
        MODIFIER + "ZZF" + COMPOSER + "II)V";
    private static final String SURFACE = "Lcom/google/android/gms/internal/mlkit_vision_text_common/sd;->a(" +
        "Landroidx/media3/common/k1;" + MODIFIER + "I" + COMPOSER + "I)V";
    private static final String DRAW_ICON = "Lcom/x/compose/core/j;->f(" + ICON + TEXT +
        MODIFIER + "JZ" + COMPOSER + "II)V";
    private static final String NATIVE_ICON = "Lcom/x/icons/a;->I4:" + ICON;
    private static final String ASK_GROK_FIELD = "Lcom/x/urt/items/post/e6;->b0:Z";
    private static final String NATIVE_CLICKABLE = "Landroidx/compose/foundation/f0;->b(" + MODIFIER +
        "Landroidx/compose/foundation/interaction/l;Landroidx/compose/foundation/t1;Z" +
        TEXT + "Landroidx/compose/ui/semantics/l;" + CLICK + "I)" + MODIFIER;

    private static final ResourcePatch RESOURCES = PatchKt.resourcePatch(null, null, false, patch -> {
        patch.execute((Function1<ResourcePatchContext, Unit>) context -> {
            File destination = context.get("res/drawable", false);
            destination.mkdirs();
            for (String icon : Arrays.asList("ic_piko_blur_off.xml", "ic_piko_blur_on.xml")) {
                try (InputStream in = InAppMediaBlurPatch.class.getClassLoader().getResourceAsStream("mediablur/" + icon)) {
                    if (in == null) throw new IOException("Missing media blur icon " + icon);
                    Files.copy(in, new File(destination, icon).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    private static final BytecodePatch PATCH = PatchKt.bytecodePatch(
            "NewX: In-App Media Blur",
            "Blurs media in replies, with a toggle in the native post header.",
            false, patch -> {
        try {
            PatchBuilder.class.getMethod("compatibleWith", kotlin.Pair[].class).invoke(patch,
                (Object) new kotlin.Pair[]{patch.invoke("com.twitter.android", new String[]{"12.43.0-prod.01"})});
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Media blur compatibility ABI", e);
        }
        patch.dependsOn(new Patch[]{RESOURCES});
        patch.extendWith(() -> {
            InputStream dex = InAppMediaBlurPatch.class.getClassLoader().getResourceAsStream("extensions/media-blur.mpe");
            if (dex == null) throw new IllegalStateException("Missing media-blur.mpe");
            return dex;
        });
        PikoIntegrationKt.pikoScreen(patch, "newx.content.media_blur", "piko_newx_media_blur", 430,
            "Lapp/morphe/extension/newx/mediablur/BlurSettingsFragment;");
        patch.execute((Function1<BytecodePatchContext, Unit>) context -> {
            install(context);
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    public static BytecodePatch getInAppMediaBlurPatch() {
        return PATCH;
    }

    private static MutableMethod method(BytecodePatchContext context, String descriptor) {
        String owner = descriptor.substring(0, descriptor.indexOf("->"));
        for (MutableMethod method : context.mutableClassDefBy(owner).getMethods()) {
            if (method.toString().equals(descriptor) && method.getImplementation() != null) return method;
        }
        throw new IllegalStateException("Media blur cannot resolve " + descriptor);
    }

    private static List<Instruction> code(Method method) {
        List<Instruction> result = new ArrayList<>();
        method.getImplementation().getInstructions().forEach(result::add);
        return result;
    }

    private static String reference(Instruction instruction) {
        return instruction instanceof ReferenceInstruction
            ? ((ReferenceInstruction) instruction).getReference().toString() : "";
    }

    private static int findUnique(List<Instruction> instructions, String target) {
        int matched = -1;
        for (int i = 0; i < instructions.size(); i++) {
            if (!reference(instructions.get(i)).equals(target)) continue;
            if (matched >= 0) throw new IllegalStateException("Ambiguous media blur hook " + target);
            matched = i;
        }
        if (matched < 0) throw new IllegalStateException("Missing media blur hook " + target);
        return matched;
    }

    private static void install(BytecodePatchContext context) {
        MutableMethod detail = method(context, DETAIL);
        MutableMethod sheet = method(context, SHEET);
        MutableMethod header = method(context, HEADER);
        MutableMethod postBody = method(context, POST_BODY);
        MutableMethod image = method(context, IMAGE);
        MutableMethod video = method(context, VIDEO);
        MutableMethod gif = method(context, GIF);
        MutableMethod surface = method(context, SURFACE);

        hookHeader(context, header);
        wrap(context, detail, 0, "Landroidx/compose/material3/i6;", 1);
        wrap(context, sheet, 1, "Lcom/x/postdetailsheet/g;", 3);
        wrap(context, header, 2, "Landroidx/compose/material3/j6;", 1);
        // This renderer is also called directly by reply rows, without the header provider.
        wrap(context, postBody, 3, "Lcom/x/ui/common/toasts/c;", 1);
        hookMedia(image, 3, 4, 5, -1);
        hookMedia(video, 5, 14, 15, 17);
        hookMedia(gif, 1, 5, 6, 7);
        I.addInstructions(surface, 0,
            "move/from16 v0, p2\nmove-object/from16 v1, p3\n" +
            "invoke-static {v0, v1}, " + R + "->surface(I" + OBJECT + ")I\nmove-result p2");
        System.out.println("Media Blur: detail and sheet scopes, native Ask Grok header, reply body, image/video/GIF renderers and Media3 surface");
    }

    private static void hookHeader(BytecodePatchContext context, MutableMethod header) {
        List<Instruction> body = code(header);
        int gate = findUnique(body, ASK_GROK_FIELD);
        int nativeIcon = findUnique(body, NATIVE_ICON);
        // The Grok branch must contain a single clickable call whose seventh argument is a
        // Function0. The icon renderer uses p0=icon, p1=description and p6=Composer.
        int click = -1;
        int draw = -1;
        for (int index = gate + 1; index < Math.min(body.size(), nativeIcon + 7); index++) {
            String ref = reference(body.get(index));
            if (ref.equals(NATIVE_CLICKABLE)) click = index;
            if (index > nativeIcon && ref.equals(DRAW_ICON)) draw = index;
        }
        if (header.getImplementation().getRegisterCount() != 75 || nativeIcon <= gate ||
            click != gate + 51 || draw != nativeIcon + 4 ||
            !reference(body.get(click - 6)).equals(CLICK) ||
            body.get(click - 6).getOpcode() != Opcode.CHECK_CAST ||
            ((OneRegisterInstruction) body.get(click - 6)).getRegisterA() != 29 ||
            ((RegisterRangeInstruction) body.get(click)).getStartRegister() != 23 ||
            ((RegisterRangeInstruction) body.get(draw)).getStartRegister() != 15 ||
            ((OneRegisterInstruction) body.get(gate)).getRegisterA() != 4 ||
            body.get(gate + 1).getOpcode() != Opcode.IF_EQZ ||
            reference(body.get(gate + 5)).indexOf(COMPOSER) < 0) {
            throw new IllegalStateException("Native Ask Grok header register flow changed");
        }

        MutableMethod renderer = method(context, DRAW_ICON);
        String name = "pikoBlurHeaderIcon";
        String owner = header.getDefiningClass();
        MutableMethod bridge = newStatic(owner, name, renderer, 2);
        I.addInstructions(bridge, 0, "move-object v0, p0\nmove-object v1, p6\n" +
            "invoke-static {v0, v1}, " + R + "->icon(" + OBJECT + OBJECT + ")" + OBJECT + "\n" +
            "move-result-object p0\ncheck-cast p0, " + ICON + "\n" +
            "move-object v0, p1\ninvoke-static {v0, v1}, " + R + "->label(" + TEXT + OBJECT + ")" + TEXT + "\n" +
            "move-result-object p1\ninvoke-static/range {p0 .. p8}, " + DRAW_ICON + "\nreturn-void");
        context.mutableClassDefBy(owner).getMethods().add(bridge);
        replaceCall(header, draw, body.get(draw), new ImmutableMethodReference(
            owner, name, renderer.getParameterTypes(), renderer.getReturnType()));

        // v5 is dead after the callback has been moved to v29 and is next overwritten by the
        // padding literal. v15 is the Compose runtime in this branch up to the vector draw.
        I.addInstructions(header, click - 5, "move-object/from16 v5, v29\n" +
            "invoke-static {v5, v15}, " + R + "->click(" + OBJECT + OBJECT + ")" + OBJECT + "\n" +
            "move-result-object v29\ncheck-cast v29, " + CLICK);
        I.addInstructions(header, gate + 1, "invoke-static {v4, v15}, " + R +
            "->headerVisible(Z" + OBJECT + ")Z\nmove-result v4");
    }

    private static void hookMedia(MutableMethod method, int modifier, int composer, int changed, int defaults) {
        List<? extends CharSequence> parameters = method.getParameterTypes();
        if (!parameters.get(modifier).toString().equals(MODIFIER) ||
            !parameters.get(composer).toString().equals(COMPOSER) ||
            method.getImplementation().getRegisterCount() < parameters.size() + 4) {
            throw new IllegalStateException("Invalid media renderer shape: " + method);
        }
        int modifierChangeMask = 7 << (1 + 3 * modifier);
        String instructions = "move-object/from16 v0, p0\nmove-object/from16 v1, p" + modifier +
            "\nmove-object/from16 v2, p" + composer + "\ninvoke-static {v0, v1, v2}, " +
            R + "->media(" + OBJECT + OBJECT + OBJECT + ")" + OBJECT + "\nmove-result-object v0\n" +
            "if-eqz v0, :piko_blur_original\ncheck-cast v0, " + MODIFIER + "\nmove-object/16 p" +
            modifier + ", v0\nconst v3, " + ~modifierChangeMask + "\nand-int p" + changed +
            ", p" + changed + ", v3\nor-int/lit8 p" + changed + ", p" + changed + ", 0x1\n";
        if (defaults >= 0) {
            instructions += "and-int/lit8 p" + defaults + ", p" + defaults + ", " + ~(1 << modifier) + "\n";
        }
        I.addInstructions(method, 0, instructions + ":piko_blur_original\nnop");
    }

    private static MutableMethod newStatic(String owner, String name, Method prototype, int locals) {
        List<ImmutableMethodParameter> parameters = new ArrayList<>();
        int words = 0;
        for (CharSequence type : prototype.getParameterTypes()) {
            parameters.add(new ImmutableMethodParameter(type.toString(), Collections.emptySet(), null));
            words += type.toString().equals("J") || type.toString().equals("D") ? 2 : 1;
        }
        int flags = AccessFlags.PUBLIC.getValue() | AccessFlags.STATIC.getValue();
        return new MutableMethod(new ImmutableMethod(owner, name, parameters, prototype.getReturnType(),
            flags, Collections.emptySet(), Collections.emptySet(),
            new ImmutableMethodImplementation(words + locals, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList())));
    }

    private static void replaceCall(MutableMethod method, int index, Instruction instruction,
            ImmutableMethodReference target) {
        if (instruction instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction range = (RegisterRangeInstruction) instruction;
            I.replaceInstruction(method, index, new BuilderInstruction3rc(instruction.getOpcode(),
                range.getStartRegister(), range.getRegisterCount(), target));
        } else {
            FiveRegisterInstruction regs = (FiveRegisterInstruction) instruction;
            I.replaceInstruction(method, index, new BuilderInstruction35c(instruction.getOpcode(),
                regs.getRegisterCount(), regs.getRegisterC(), regs.getRegisterD(), regs.getRegisterE(),
                regs.getRegisterF(), regs.getRegisterG(), target));
        }
    }

    private static void wrap(BytecodePatchContext context, MutableMethod original, int kind,
            String callbackOwner, int expectedRestarts) {
        String owner = original.getDefiningClass();
        String descriptor = original.toString();
        String originalName = original.getName();
        String delegate = "pikoBlurOriginal_" + originalName;
        if (!AccessFlags.STATIC.isSet(original.getAccessFlags()) || !original.getReturnType().equals("V") ||
            code(original).stream().noneMatch(instruction -> instruction.getOpcode() == Opcode.NEW_INSTANCE &&
                reference(instruction).equals(callbackOwner))) {
            throw new IllegalStateException("Unrecognized restart callback for " + descriptor);
        }
        ImmutableMethodReference destination = new ImmutableMethodReference(
            owner, delegate, original.getParameterTypes(), "V");
        int redirected = 0;
        for (MutableMethod callback : context.mutableClassDefBy(callbackOwner).getMethods()) {
            if (callback.getImplementation() == null || !callback.getName().equals("invoke")) continue;
            List<Instruction> body = code(callback);
            for (int index = 0; index < body.size(); index++) {
                Instruction instruction = body.get(index);
                if (!reference(instruction).equals(descriptor)) continue;
                replaceCall(callback, index, instruction, destination);
                redirected++;
            }
        }
        if (redirected != expectedRestarts) {
            throw new IllegalStateException("Media blur expected " + expectedRestarts +
                " restart calls for " + descriptor + " but found " + redirected);
        }

        MutableClass clazz = context.mutableClassDefBy(owner);
        List<? extends CharSequence> types = original.getParameterTypes();
        MutableMethod wrapper = newStatic(owner, originalName, original, 4);
        StringBuilder body = new StringBuilder("const/16 v0, ").append(types.size())
            .append("\nnew-array v0, v0, [Ljava/lang/Object;\n");
        for (int parameter = 0; parameter < types.size(); parameter++) {
            body.append("const/16 v1, ").append(parameter).append('\n');
            String type = types.get(parameter).toString();
            if (type.equals("I")) {
                body.append("move/from16 v2, p").append(parameter)
                    .append("\ninvoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;\nmove-result-object v2\n");
            } else if (type.startsWith("L") || type.startsWith("[")) {
                body.append("move-object/from16 v2, p").append(parameter).append('\n');
            } else {
                throw new IllegalStateException("Unexpected scope parameter " + type + " in " + descriptor);
            }
            body.append("aput-object v2, v0, v1\n");
        }
        body.append("const/4 v1, ").append(kind).append("\ninvoke-static {v0, v1}, ")
            .append(R).append("->scope([Ljava/lang/Object;I)V\nreturn-void");
        I.addInstructions(wrapper, 0, body.toString());
        clazz.getMethods().remove(original);
        original.setName(delegate);
        clazz.getMethods().add(original);
        clazz.getMethods().add(wrapper);
        System.out.println("Media Blur: " + descriptor + ", preserved " + redirected + " native restart call(s)");
    }
}
