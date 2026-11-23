package androidx.compose.runtime;

import java.util.HashMap;
import java.util.Map;
import kotlin.jvm.functions.Function2;

public final class i {
    public record Provided(g0 key, Object value) {}

    public static final class Composer {
        private final Map<g0, Object> locals;

        public Composer() {
            this(Map.of());
        }

        private Composer(Map<g0, Object> locals) {
            this.locals = locals;
        }

        public Object m(g0 key) {
            return locals.getOrDefault(key, key.fallback());
        }
    }

    public static void c(Provided value, Function2<Object, Object, Object> child, Composer parent, int flags) {
        Map<g0, Object> locals = new HashMap<>(parent.locals);
        locals.put(value.key(), value.value());
        child.invoke(new Composer(locals), flags);
    }

    public static State s(Object value) {
        return new State(value);
    }

    public static final class State {
        private Object value;

        State(Object value) { this.value = value; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }
}
