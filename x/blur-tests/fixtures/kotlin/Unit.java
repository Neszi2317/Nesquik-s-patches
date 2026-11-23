package kotlin;

/** The bundled Kotlin Unit has an obfuscated singleton field in X. */
public final class Unit {
    public static final Unit a = new Unit();
    public static final Unit INSTANCE = a;
}
