package android.content;

/** Lets the host fixtures supply disk-backed SharedPreferences without Android. */
public class ContextWrapper extends Context {
    private final Context base;
    public ContextWrapper(Context base) { this.base = base; }
    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return base.getSharedPreferences(name, mode);
    }
}
