package androidx.compose.runtime;

import kotlin.jvm.functions.Function0;

public final class g0 {
    private final Function0<?> defaultValue;

    public g0(Function0<?> defaultValue, byte type) {
        this.defaultValue = defaultValue;
    }

    public i.Provided a(Object value) {
        return new i.Provided(this, value);
    }

    Object fallback() {
        return defaultValue.invoke();
    }
}
