package app.morphe.extension.newx.mediablur;
import android.content.Context;import android.content.SharedPreferences;
public final class BlurPreferences {
 static SharedPreferences prefs;
 static void init(Context c){if(prefs==null)prefs=c.getApplicationContext().getSharedPreferences("piko_in_app_media_blur",Context.MODE_PRIVATE);}
 static boolean enabled(){return prefs==null||prefs.getBoolean("enabled",true);}
 static boolean reveal(){return prefs==null||prefs.getBoolean("reveal",true);}
 static float radius(){String s=prefs==null?"strong":prefs.getString("strength","strong");return "light".equals(s)?12f:"medium".equals(s)?28f:"maximum".equals(s)?180f:60f;}
}
