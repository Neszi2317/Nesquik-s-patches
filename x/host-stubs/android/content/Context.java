package android.content;

import android.content.res.AssetManager;

/** Small host stand-in for the preferences test fixtures. */
public abstract class Context {
    public static final int MODE_PRIVATE = 0;
    public abstract SharedPreferences getSharedPreferences(String name, int mode);
    public Context getApplicationContext() { return this; }
    public AssetManager getAssets() { throw new UnsupportedOperationException(); }
}
