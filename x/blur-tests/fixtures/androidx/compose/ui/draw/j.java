package androidx.compose.ui.draw;

public final class j {
    public record Blurred(Object base, float horizontal, float vertical) {}

    public static Blurred b(Object base, float horizontal, float vertical, Object shape) {
        return new Blurred(base, horizontal, vertical);
    }
}
