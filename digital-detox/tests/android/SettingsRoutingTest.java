package fr.nesquik.detox;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.preference.Preference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=36, manifest=Config.NONE, qualifiers="fr")
public class SettingsRoutingTest {
  @Test public void originalSettingsClick() throws Exception { clickAndOpen("com.urbandroid.ddc"); }
  @Test public void clonedSettingsClick() throws Exception { clickAndOpen("com.urbandroid.ddc.morphe"); }

  private static void clickAndOpen(String installedPackage) throws Exception {
    try (ActivityController<Activity> source=Robolectric.buildActivity(Activity.class).setup()) {
      Activity activity=source.get();
      String apk=System.getProperty(installedPackage);
      assertNotNull("Provide the patched APK for " + installedPackage, apk);
      AssetManager assets=AssetManager.class.getDeclaredConstructor().newInstance();
      int cookie=(int)AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,apk);
      assertTrue(cookie>0);
      Resources base=activity.getResources();
      Resources packaged=new Resources(assets,base.getDisplayMetrics(),base.getConfiguration());
      Context context=new ContextThemeWrapper(activity, android.R.style.Theme_DeviceDefault_Settings) {
        @Override public String getPackageName() { return installedPackage; }
        @Override public Resources getResources() { return packaged; }
        @Override public AssetManager getAssets() { return assets; }
      };
      int settings=packaged.getIdentifier("settings_main","xml","com.urbandroid.ddc");
      if(settings==0)settings=packaged.getIdentifier("settings_main","xml",installedPackage);
      assertNotEquals(0,settings);
      PreferenceScreen screen=new PreferenceManager(context).inflateFromResource(context,settings,null);
      Preference row=screen.findPreference("nesquik_detox_sync");
      assertNotNull(row);
      row.performClick();
      Intent launched=Shadows.shadowOf(activity).getNextStartedActivity();
      assertNotNull(launched);
      assertEquals(new ComponentName(installedPackage, Devices.class.getName()), launched.getComponent());
      try (ActivityController<Devices> panel=Robolectric.buildActivity(Devices.class,launched).setup()) {
        assertEquals("Synchronisation des appareils",panel.get().getTitle());
        assertTrue(containsText(panel.get().findViewById(android.R.id.content),"Synchroniser par défaut"));
      }
    }
  }

  private static boolean containsText(View view, String text) {
    if (view instanceof TextView && text.contentEquals(((TextView)view).getText())) return true;
    if (view instanceof ViewGroup) {
      ViewGroup group=(ViewGroup)view;
      for (int i=0;i<group.getChildCount();i++) if(containsText(group.getChildAt(i),text))return true;
    }
    return false;
  }
}
