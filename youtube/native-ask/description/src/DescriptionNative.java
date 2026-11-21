package app.neszi.askdescription;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static app.neszi.askdescription.Wire.*;

/** Stores and restores ONLY a genuine native footer for the SAME video/configuration.
 * No View, drawable, text, endpoint or click handler is manufactured by this add-on.
 */
public final class DescriptionNative {
    static final String PANEL="video-description-ep-identifier";
    static final int FOOTER=260791572, ELEMENT=153515154;
    static final int MAX_FOOTER=262144, MAX_ENTRIES=16;
    private static final String TAG="AskDescription", ACCESS="app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch$MessageAccess";
    private static final LinkedHashMap<String,byte[]> memory=new LinkedHashMap<>();
    private static final ThreadLocal<Boolean> parsing=new ThreadLocal<>();
    private static final Set<String> messages=Collections.synchronizedSet(new HashSet<>());
    public static boolean isWatchRoot(Object object) { return false; } // Exact instanceof injected by patch.
    private static Class<?> bridge() throws Exception { return Class.forName(ACCESS); }
    private static byte[] bytes(Object object) throws Exception { return (byte[])bridge().getMethod("patch_toByteArray").invoke(object); }
    private static Object parse(Object object,byte[] bytes) throws Exception {
        return bridge().getMethod("patch_parseSameType",byte[].class).invoke(object,(Object)bytes);
    }
    private static Context context() throws Exception {
        return (Context)Class.forName("app.morphe.extension.shared.Utils").getMethod("getContext").invoke(null);
    }
    private static String video() throws Exception {
        return (String)Class.forName("app.morphe.extension.youtube.patches.VideoInformation").getMethod("getVideoId").invoke(null);
    }
    private static String config(Context context) throws Exception {
        Configuration c=context.getResources().getConfiguration();
        return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName+"/"+
            c.getLocales().toLanguageTags()+"/"+c.uiMode+"/"+c.orientation+"/"+c.screenWidthDp+"/"+c.densityDpi;
    }
    private static boolean validVideo(String video) { return video!=null && video.matches("[A-Za-z0-9_-]{11}"); }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("neszi_ask_description_v1",Context.MODE_PRIVATE); }
    static boolean description(byte[] panel) {
        return PANEL.equals(text(first(panel,1))) || PANEL.equals(text(first(first(panel,18),3)));
    }
    static boolean genuine(byte[] footer) {
        if(footer.length==0 || footer.length>MAX_FOOTER)return false;
        byte[] element=first(first(first(footer,FOOTER),1),ELEMENT);
        if(element.length==0)return false;
        String raw=text(element);
        // Exact native component names confirmed by Morphe DescriptionComponentsFilter.
        boolean component=raw.contains("input_composer_button.e") || raw.contains("youchat_entrypoint.e");
        // Conservative initial scope: the actual summary footer, not an arbitrary chat composer.
        boolean summary=raw.contains("Résume la vidéo") || raw.contains("Résumer la vidéo") ||
            raw.contains("Summarize the video") || raw.contains("Summarize this video");
        return component && summary;
    }
    static byte[] snapshot(byte[] panel) {
        if(!description(panel) || !genuine(first(panel,10)))return null;
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Field f:fields(panel))if(f.n==10 || f.n==39 || f.n==47)write(out,f.raw);
        return out.toByteArray();
    }
    static byte[] restore(byte[] panel,byte[] saved) {
        if(!description(panel) || saved==null || saved.length>MAX_FOOTER+64)return panel;
        // Preserve ANY native footer already supplied, including a newer native variation.
        if(first(panel,10).length!=0 || !genuine(first(saved,10)))return panel;
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Field f:fields(panel))if(f.n!=39 && f.n!=47)write(out,f.raw);
        write(out,saved);
        return out.toByteArray();
    }
    static String watchVideo(byte[] root) {
        byte[] results=first(first(first(first(root,7),51779735),1),49399797);
        for(Field f:fields(results))if(f.n==1 && f.w==2){
            String id=text(first(first(f.v,216561405),2));
            if(validVideo(id))return id;
        }
        return null;
    }
    static List<byte[]> panels(byte[] root) {
        List<byte[]> result=new ArrayList<>();
        // Verified bfxo NextResponse: 25/30 repeated bdwh; NOT an unrestricted recursive scan.
        for(Field f:fields(root))if((f.n==25 || f.n==30) && f.w==2) {
            byte[] panel=first(f.v,138681066);
            if(panel.length>0 && description(panel))result.add(panel);
        }
        return result;
    }
    private static void report(String code) {
        if(messages.size()>64)messages.clear();
        if(messages.add(code))Log.i(TAG,"0.1.0 "+code);
    }
    private static synchronized void store(Context c,String key,byte[] saved) {
        byte[] old=memory.get(key);
        if(Arrays.equals(old,saved))return;
        memory.remove(key);memory.put(key,saved);
        while(memory.size()>MAX_ENTRIES)memory.remove(memory.keySet().iterator().next());
        SharedPreferences p=prefs(c);
        LinkedHashSet<String> keys=new LinkedHashSet<>(Arrays.asList(p.getString("keys","").split("\n")));
        keys.remove("");keys.remove(key);keys.add(key);
        SharedPreferences.Editor edit=p.edit();
        while(keys.size()>MAX_ENTRIES){String first=keys.iterator().next();keys.remove(first);edit.remove(first);}
        edit.putString(key,Base64.encodeToString(saved,Base64.NO_WRAP)).putString("keys",String.join("\n",keys)).apply();
        report("native_footer_saved");
    }
    private static synchronized byte[] load(Context c,String key) {
        byte[] value=memory.get(key);if(value!=null)return value;
        String saved=prefs(c).getString(key,"");
        if(saved.isEmpty() || saved.length()>MAX_FOOTER*2)return null;
        try {return Base64.decode(saved,Base64.NO_WRAP);}catch(IllegalArgumentException ignored){return null;}
    }
    /** Observe unmodified watch responses: bind native footer to this response's video. */
    public static void observe(Object root) {
        if(!isWatchRoot(root) || Boolean.TRUE.equals(parsing.get()))return;
        try {
            byte[] data=bytes(root);if(data.length>4*1024*1024)return;
            Context c=context();if(c!=null)captureRoot(data,c);
        }catch(Exception e){Log.w(TAG,"observe_failed: "+e.getClass().getSimpleName());}
    }
    static void captureRoot(byte[] data,Context c) throws Exception {
        String id=watchVideo(data);if(!validVideo(id))return;
        for(byte[] panel:panels(data)) {
            byte[] saved=snapshot(panel);
            if(saved!=null)store(c,config(c)+"/"+id,saved);
            else report(first(panel,10).length==0?"description_received_without_footer":"description_footer_unrecognized");
        }
    }
    /** Runs before the ORIGINAL engagement-panel binder. Native rendering/click code untouched. */
    public static Object beforeBind(Object panel) {
        if(panel==null || Boolean.TRUE.equals(parsing.get()))return panel;
        try {
            byte[] original=bytes(panel);if(original.length>2*1024*1024 || !description(original))return panel;
            report("description_binder_reached");
            if(first(original,10).length!=0){report(genuine(first(original,10))?"native_footer_already_present":"existing_footer_preserved");return panel;}
            String id=video();if(!validVideo(id)) {report("current_video_unknown");return panel;}
            Context c=context();if(c==null)return panel;
            byte[] saved=load(c,config(c)+"/"+id);
            if(saved==null){report("no_native_footer_saved_for_this_video_and_configuration");return panel;}
            byte[] changed=restore(original,saved);if(changed==original)return panel;
            parsing.set(true);
            Object result=parse(panel,changed);
            report("native_footer_restored_same_video");
            return result;
        }catch(Exception e){Log.w(TAG,"restore_failed: "+e.getClass().getSimpleName());return panel;}
        finally {parsing.remove();}
    }
}
