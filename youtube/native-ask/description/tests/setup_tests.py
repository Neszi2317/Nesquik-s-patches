from pathlib import Path
root=Path(__file__).parent/'stubs'
files={
'android/util/Log.java':'package android.util; public class Log {public static int i(String t,String m){System.out.println(t+": "+m);return 0;}public static int w(String t,String m){System.out.println(t+": "+m);return 0;}}',
'android/util/Base64.java':'package android.util; public class Base64 {public static final int NO_WRAP=2;public static byte[] decode(String s,int f){return java.util.Base64.getDecoder().decode(s);}public static String encodeToString(byte[] b,int f){return java.util.Base64.getEncoder().encodeToString(b);}}',
'android/content/SharedPreferences.java':'package android.content;public interface SharedPreferences {String getString(String k,String d);Editor edit();interface Editor {Editor putString(String k,String v);Editor remove(String k);void apply();}}',
'android/content/Context.java':'''package android.content;import java.util.*;import android.content.res.Resources;import android.content.pm.PackageManager;
public class Context {public static final int MODE_PRIVATE=0;public final Map<String,String> saved=new HashMap<>();public final Resources res=new Resources();public Resources getResources(){return res;}public PackageManager getPackageManager(){return new PackageManager();}public String getPackageName(){return "com.google.android.youtube";}public SharedPreferences getSharedPreferences(String n,int m){return new SharedPreferences(){public String getString(String k,String d){return saved.getOrDefault(k,d);}public Editor edit(){return new Editor(){public Editor putString(String k,String v){saved.put(k,v);return this;}public Editor remove(String k){saved.remove(k);return this;}public void apply(){}};}};}}''',
'android/content/pm/PackageManager.java':'package android.content.pm;public class PackageManager {public PackageInfo getPackageInfo(String s,int f){return new PackageInfo();}}',
'android/content/pm/PackageInfo.java':'package android.content.pm;public class PackageInfo {public String versionName="21.30.216";}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public final Configuration config=new Configuration();public Configuration getConfiguration(){return config;}}',
'android/content/res/Configuration.java':'package android.content.res;import android.os.LocaleList;public class Configuration {public int uiMode=32,orientation=1,screenWidthDp=400,densityDpi=480;public LocaleList getLocales(){return new LocaleList();}}',
'android/os/LocaleList.java':'package android.os;public class LocaleList {public String toLanguageTags(){return "fr-FR";}}',
'app/morphe/extension/shared/Utils.java':'package app.morphe.extension.shared;import android.content.Context;public class Utils {public static final Context CONTEXT=new Context();public static Context getContext(){return CONTEXT;}}',
'app/morphe/extension/youtube/patches/VideoInformation.java':'package app.morphe.extension.youtube.patches;public class VideoInformation {public static String video="abcDEF123_-";public static String getVideoId(){return video;}}',
'app/morphe/extension/youtube/patches/ask/AskFlyoutMenuPatch.java':'package app.morphe.extension.youtube.patches.ask;public class AskFlyoutMenuPatch {public interface MessageAccess {byte[] patch_toByteArray();MessageAccess patch_parseSameType(byte[] bytes);}}'
}
for name,data in files.items():p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data+'\n')
