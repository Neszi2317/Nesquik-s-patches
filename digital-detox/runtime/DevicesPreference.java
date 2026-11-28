package fr.nesquik.detox;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import androidx.preference.Preference;

/** AndroidX inflates this row with the installed app's themed context, including on clones. */
public final class DevicesPreference extends Preference {
  public DevicesPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setIntent(new Intent(context, Devices.class));
  }
}
