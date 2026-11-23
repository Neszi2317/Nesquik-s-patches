import androidx.compose.runtime.i.Composer;
import androidx.compose.ui.draw.j.Blurred;
import app.morphe.extension.newx.mediablur.BlurRuntime;
import java.util.function.Consumer;
import kotlin.jvm.functions.Function0;

public final class BlurScopeTest {
    private static final PostIdentityTest.Row ROOT = new PostIdentityTest.Row(100, 42);
    private static final PostIdentityTest.Row REPLY = new PostIdentityTest.Row(101, 42);
    private static final PostIdentityTest.Row QUOTE = new PostIdentityTest.Row(102, 42);
    private static final Object MODIFIER = new Object();
    private static final Function0<Object> NATIVE_CLICK = () -> null;
    private static int checks;

    public static final class Sheet {
        public PostIdentityTest.Controller f;
        public Sheet(PostIdentityTest.Controller controller) { f = controller; }
    }

    public static final class Media {
        public String a() { return "reply-media"; }
    }

    private static void check(boolean passes, String message) {
        if (!passes) throw new AssertionError(message);
        checks++;
    }

    private static void header(PostIdentityTest.Row row, Composer composer, Consumer<Composer> child) {
        BlurRuntime.scope(new Object[]{row, null, null, NATIVE_CLICK, child, composer, 0}, 2);
    }

    private static void body(PostIdentityTest.Row row, Composer composer, Consumer<Composer> child) {
        if (Boolean.getBoolean("blur.test.beforeReplyScope")) {
            child.accept(composer);
        } else {
            BlurRuntime.scope(new Object[]{row, child, null, composer, 0}, 3);
        }
    }

    private static Object media(Composer composer) {
        return BlurRuntime.media(new Media(), MODIFIER, composer);
    }

    private static void exerciseThread(Composer thread) {
        body(REPLY, thread, reply -> {
            Object original = media(reply);
            check(original == null || original == MODIFIER, "Reply starts clear");
        });
        header(ROOT, thread, root -> {
            check(BlurRuntime.headerVisible(false, root), "Button appears on the root header");
            ((Function0<?>) BlurRuntime.click(NATIVE_CLICK, root)).invoke();
            check(BlurRuntime.label("Grok", root).contains("Désactiver"), "Button activates the session");
            body(QUOTE, root, quote -> check(media(quote) == null, "Root quote remains clear"));
        });
        body(ROOT, thread, root -> check(media(root) == null, "Root body remains clear"));
        body(REPLY, thread, reply -> {
            check(media(reply) instanceof Blurred blur && blur.base() == MODIFIER
                && blur.horizontal() > 0 && blur.vertical() > 0, "Reply body receives the blur modifier");
            check(BlurRuntime.surface(1, reply) == 2, "Reply video uses a blur-compatible surface");
            body(ROOT, reply, quote -> check(media(quote) instanceof Blurred, "Quote in a reply stays eligible"));
            check(BlurRuntime.click(NATIVE_CLICK, reply) == NATIVE_CLICK, "Reply does not get a second toggle");
        });
        header(ROOT, thread, root -> ((Function0<?>) BlurRuntime.click(NATIVE_CLICK, root)).invoke());
        body(REPLY, thread, reply -> check(media(reply) == MODIFIER, "Second press clears the reply"));
    }

    public static void main(String[] args) {
        Composer outside = new Composer();
        body(REPLY, outside, reply -> check(media(reply) == null, "Home feed remains clear"));
        PostIdentityTest.Controller controller = new PostIdentityTest.Controller(100, 42);
        Consumer<Composer> scenarios = BlurScopeTest::exerciseThread;
        BlurRuntime.scope(new Object[]{scenarios, controller, null, null, outside, 0, 0}, 0);
        BlurRuntime.scope(new Object[]{new Sheet(controller), null, null, scenarios, outside, 0}, 1);
        System.out.println("PASS " + checks + " scope and media modifier assertions for detail, sheet and home feed");
    }
}
