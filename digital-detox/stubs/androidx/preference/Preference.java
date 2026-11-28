package androidx.preference;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

/** Compile-only binding. Digital Detox supplies the AndroidX implementation. */
public class Preference {
  public Preference(Context context, AttributeSet attrs) {}
  public void setIntent(Intent intent) { throw new AssertionError(); }
}
