package app.neszi.asknative;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native Elements button; no Android View insertion and no copied Morphe extension. */
public final class AskNative {
    static final int MARKER = 500000001;
    static final int SECONDARY = 461054335;
    static final String PREFIX = "neszi.ask.native:";
    static final String FULLSCREEN_PREFIX = "neszi.ask.fullscreen:";
    static volatile byte[] nativeFullscreenPresentation;
    static volatile byte[] genuineFullscreenVisual;
    private static volatile boolean directFullscreen;
    private static boolean currentFullscreen;
    private static final ThreadLocal<Boolean> fullscreenPanel = new ThreadLocal<>();
    private static volatile int quickSeen, quickChanged, quickNative;
    private static final String OFFICIAL = "app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch";
    private static volatile String directVideo;
    private static volatile long directTime;
    private static String currentDirectVideo;
    private static volatile Class<?> access;
    private static final ThreadLocal<Boolean> directShow = new ThreadLocal<>();
    private static final ThreadLocal<String> directPanelVideo = new ThreadLocal<>();
    static volatile byte[] genuineVisual;
    private static volatile byte[] nativePanelIdentifier;
    // Original watch opening TARGET ONEOF and presentation only; no video/token/callback payloads.
    static volatile byte[] nativeWatchPresentation;
    private static boolean presentationCaptureAttempted;
    private static boolean visualLoaded;
    private static Context context() {
        try { return (Context) Class.forName("app.morphe.extension.shared.Utils").getMethod("getContext").invoke(null); }
        catch (Exception ignored) { return null; }
    }
    static boolean tablet() {
        Context c = context();
        return c != null && c.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }
    private static SharedPreferences preferences() {
        Context c = context();
        return c == null ? null : c.getSharedPreferences("neszi_ask_original", Context.MODE_PRIVATE);
    }
    private static String visualKey() { return tablet() ? "visual_tablet" : "visual_phone"; }
    private static synchronized void loadVisual() {
        if (visualLoaded) return;
        SharedPreferences p = preferences();
        if (p == null) return;
        visualLoaded = true;
        try {
            String saved = p.getString(visualKey(), "");
            if (!saved.isEmpty()) genuineVisual = Base64.decode(saved, Base64.NO_WRAP);
            String id = p.getString("watch_identifier", "");
            if (!id.isEmpty()) nativePanelIdentifier = Base64.decode(id, Base64.NO_WRAP);
            String fsVisual=p.getString("fullscreen_visual_v1", "");
            if(!fsVisual.isEmpty())genuineFullscreenVisual=Base64.decode(fsVisual,Base64.NO_WRAP);
            String fullscreen = p.getString("fullscreen_presentation_v1", "");
            if (fullscreen.startsWith("v1:")) nativeFullscreenPresentation=Base64.decode(fullscreen.substring(3),Base64.NO_WRAP);
            String opening = p.getString("watch_presentation_v1", "");
            if (opening.startsWith("v1:")) nativeWatchPresentation = Base64.decode(opening.substring(3),Base64.NO_WRAP);
        } catch (IllegalArgumentException ex) { Log.i("AskNative", "Original visual cache invalid; waiting for native capture"); }
    }
    static boolean nativeAsk(byte[] button) {
        String wire = new String(button, StandardCharsets.ISO_8859_1);
        String name = text(first(button,2));
        String a11y = text(first(button,5)).toLowerCase(Locale.ROOT);
        return wire.contains("PAyouchat") || name.equals("yt_fill_spark") || name.equals("yt_fill_experimental_spark") ||
            a11y.equals("poser une question") || a11y.equals("ask") || a11y.equals("ask a question");
    }
    static void captureVisual(byte[] nativeButton) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean hasIcon = false;
        for (Field f : fields(nativeButton)) {
            if (f.n==2 || f.n==35 || f.n==44 || f.n==48 || f.n==51) { write(out,f.raw); hasIcon = true; }
            else if (f.n==7 || f.n==11 || f.n==13 || f.n==40) write(out,f.raw);
        }
        if (hasIcon) genuineVisual = out.toByteArray();
        byte[] tapModel = first(nativeButton,4);
        presentationCaptureAttempted = true;
        byte[] captured = findWatchPresentation(tapModel,0);
        boolean newlyCaptured = captured != null && !Arrays.equals(captured,nativeWatchPresentation);
        if (captured != null) nativeWatchPresentation = captured;
        byte[] tap = first(tapModel,169495254);
        byte[] identifier = first(first(tap,138681778),10);
        if (text(first(identifier,3)).equals("PAyouchat")) nativePanelIdentifier=identifier;
        SharedPreferences p = preferences();
        if (p!=null) {
            SharedPreferences.Editor e=p.edit();
            if(genuineVisual!=null)e.putString(visualKey(),Base64.encodeToString(genuineVisual,Base64.NO_WRAP));
            if (nativePanelIdentifier!=null) e.putString("watch_identifier",Base64.encodeToString(nativePanelIdentifier,Base64.NO_WRAP));
            if (nativeWatchPresentation!=null) e.putString("watch_presentation_v1","v1:"+Base64.encodeToString(nativeWatchPresentation,Base64.NO_WRAP));
            e.apply();
        }
        Log.i("AskNative","0.8.2: native visual captured; watch presentation="+(captured!=null));
        if(newlyCaptured)new Handler(Looper.getMainLooper()).post(() -> {
            try { Class.forName("app.morphe.extension.shared.Utils").getMethod("showToastLong",String.class)
                .invoke(null,"Ask : ouverture native du panneau mémorisée"); }
            catch(Exception ignored) { }
        });
    }
    /** Search only the genuine Ask onTap, including Elements serial-command wrappers. */
    static byte[] findWatchPresentation(byte[] message,int depth) {
        if(depth>14 || message.length>262144)return null;
        List<Field> parsed;
        try { parsed=fields(message); } catch(IllegalArgumentException ignored){return null;}
        for(Field f:parsed) {
            if(f.n==138681778 && f.w==2) {
                byte[] captured=watchPresentation(f.v);
                if(captured!=null)return captured;
            }
        }
        for(Field f:parsed)if(f.w==2 && f.v.length>0) {
            byte[] captured=findWatchPresentation(f.v,depth+1);
            if(captured!=null)return captured;
        }
        return null;
    }
    static byte[] watchPresentation(byte[] endpoint) {
        List<Field> parsed;
        try { parsed=fields(endpoint); } catch(IllegalArgumentException ignored){return null;}
        boolean ask=text(first(endpoint,1)).equals("PAyouchat") ||
            text(first(first(endpoint,10),3)).equals("PAyouchat");
        if(!ask)return null; // Never learn another panel's settings.
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Field f:parsed) {
            // Verified 21.20.405 ShowEngagementPanelEndpoint: target1 XOR identifier10,
            // presentation8 and flags5/6/16. Exclude inline panel2, request/config data9/11/13/14,
            // strings3 and executable callbacks7/17.
            if((f.n==1 || f.n==10) && f.w==2)write(out,f.raw);
            else if((f.n==5 || f.n==6 || f.n==16) && f.w==0)write(out,f.raw);
            else if(f.n==8 && f.w==2) {
                byte[] presentation=map(f.v,1,popup -> without(popup,2));
                write(out,field(8,presentation)); // Omit presentation callback, retain its native flags.
            }
        }
        return out.toByteArray();
    }
    static byte[] without(byte[] message,int number) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Field f:fields(message))if(f.n!=number)write(out,f.raw);
        return out.toByteArray();
    }
    static byte[] visualForDevice(byte[] visual, boolean tablet) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out,visual);
        if (tablet) write(out,field(3,utf8("Poser une question")));
        write(out,field(5,utf8("Poser une question à Gemini")));
        return out.toByteArray();
    }

    static Class<?> access() throws Exception {
        if (access == null) access = Class.forName(OFFICIAL + "$MessageAccess");
        return access;
    }
    static byte[] bytes(Object message) throws Exception {
        return (byte[]) access().getMethod("patch_toByteArray").invoke(message);
    }
    static Object parse(Object message, byte[] bytes) throws Exception {
        return access().getMethod("patch_parseSameType", byte[].class).invoke(message, (Object) bytes);
    }
    private static final ThreadLocal<Boolean> parsing = new ThreadLocal<>();
    private static volatile int decoded, modified;
    private static boolean reported;
    // Implemented with exact native instanceof checks by the patch; no global protobuf scans.
    public static int messageKind(Object response) { return 0; }
    public static Object transformParsed(Object response) {
        int kind = messageKind(response);
        if (kind == 0 || Boolean.TRUE.equals(parsing.get())) return response;
        parsing.set(true);
        try {
            decoded++;
            byte[] original = bytes(response);
            byte[] changed = kind == 1 ? transformNextBytes(original) : transformBytes(original);
            if (changed == original) return response;
            Object result = parse(response, changed);
            modified++;
            Log.i("AskNative", "0.5: replaced decoded watch root before distribution");
            return result;
        } catch (Exception ex) {
            Log.e("AskNative", "0.5: decoded response unchanged", ex);
            return response;
        } finally { parsing.remove(); }
    }
    static byte[] transformNextBytes(byte[] root) {
        byte[] transformed=map(root,7,primary -> scanWatchMetadata(primary,0,new int[]{0}));
        String video=watchVideoId(first(first(root,7),51779735));
        if(video==null)return transformed;
        // Search this exact watch response by validated Elements identifier, not an assumed field8.
        try { return walkQuickElements(transformed,video,0,new int[]{0}); }
        catch(IllegalArgumentException ex) { Log.i("AskNative","Fullscreen schema unsupported; overlay preserved");return transformed; }
    }
    static String watchVideoId(byte[] single) {
        byte[] results=first(first(single,1),49399797);
        for(Field f:fields(results))if(f.n==1 && f.w==2) {
            String id=text(first(first(f.v,216561405),2));
            if(id.matches("[A-Za-z0-9_-]{11}"))return id;
        }
        return null;
    }
    /** Bounded discovery of Elements renderers, only inside NextResponse.playerOverlays. */
    static boolean containsQuickIdentifier(byte[] data) {
        byte[] needle=utf8("quick_actions.e");
        outer:for(int i=0;i<=data.length-needle.length;i++) {
            for(int j=0;j<needle.length;j++)if(data[i+j]!=needle[j])continue outer;
            return true;
        }
        return false;
    }
    static byte[] walkQuickElements(byte[] data,String id,int depth,int[] nodes) {
        if(!containsQuickIdentifier(data))return data;
        if(depth>24 || ++nodes[0]>5000 || data.length>16777216)throw new IllegalArgumentException("overlay budget");
        List<Field> parsed;
        try { parsed=fields(data); }catch(IllegalArgumentException opaque){return data;}
        ByteArrayOutputStream out=new ByteArrayOutputStream();boolean changed=false;
        for(Field f:parsed) {
            byte[] value=f.v;
            if(f.w==2) {
                if(f.n==172660663) {
                    try {
                        String name=text(first(first(first(f.v,2),183314536),1));
                        if(name.startsWith("quick_actions.e")) {
                            quickSeen++;
                            value=map(f.v,1,type -> map(type,168777401,component ->
                                map(component,5,model -> insertQuickModel(model,id,0))));
                            if(value!=f.v)quickChanged++;
                        }
                    }catch(IllegalArgumentException unsupported){value=f.v;}
                }
                if(value==f.v)value=walkQuickElements(f.v,id,depth+1,nodes);
            }
            if(value!=f.v){write(out,field(f.n,value));changed=true;}else write(out,f.raw);
        }
        return changed?out.toByteArray():data;
    }
    // A confirmed SecondaryButtonViewModel path is required. Unknown fullscreen schemas remain untouched.
    static int[] secondaryPath(byte[] item,int depth) {
        if(depth>8)return null;
        List<Field> parsed;
        try{parsed=fields(item);}catch(IllegalArgumentException opaque){return null;}
        for(Field f:parsed)if(f.n==SECONDARY && f.w==2)return new int[]{SECONDARY};
        for(Field f:parsed)if(f.w==2){
            int[] child=secondaryPath(f.v,depth+1);
            if(child!=null){int[] result=new int[child.length+1];result[0]=f.n;System.arraycopy(child,0,result,1,child.length);return result;}
        }
        return null;
    }
    static byte[] atPath(byte[] item,int[] path){for(int n:path)item=first(item,n);return item;}
    static byte[] wrapPath(int[] path,byte[] button){for(int i=path.length-1;i>=0;i--)button=field(path[i],button);return button;}
    static byte[] iconAndStyle(byte[] source,boolean includeStyle) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Field f:fields(source))if(f.n==2 || f.n==35 || f.n==44 || f.n==48 || f.n==51 ||
            (includeStyle && (f.n==7 || f.n==11 || f.n==13 || f.n==40)))write(out,f.raw);
        return out.toByteArray();
    }
    static void captureFullscreen(byte[] button) {
        if(iconAndStyle(button,false).length>0) {
            genuineFullscreenVisual=iconAndStyle(button,true);
            SharedPreferences visualPrefs=preferences();
            if(visualPrefs!=null)visualPrefs.edit().putString("fullscreen_visual_v1",Base64.encodeToString(genuineFullscreenVisual,Base64.NO_WRAP)).apply();
        }
        byte[] captured=findWatchPresentation(first(button,4),0);
        if(captured==null || Arrays.equals(captured,nativeFullscreenPresentation))return;
        nativeFullscreenPresentation=captured;
        SharedPreferences p=preferences();
        if(p!=null)p.edit().putString("fullscreen_presentation_v1","v1:"+Base64.encodeToString(captured,Base64.NO_WRAP)).apply();
        new Handler(Looper.getMainLooper()).post(() -> {
            try{Class.forName("app.morphe.extension.shared.Utils").getMethod("showToastLong",String.class).invoke(null,"Ask : panneau latéral natif mémorisé");}catch(Exception ignored){}
        });
    }
    static byte[] insertQuickModel(byte[] model,String id,int depth) {
        if(depth>12 || model.length>262144)return model;
        List<Field> parsed;
        try{parsed=fields(model);}catch(IllegalArgumentException opaque){return model;}
        // Locate the native repeated action list through its Share item; preserve its exact wrapper path.
        for(Field share:parsed)if(share.w==2) {
            int[] path=secondaryPath(share.v,0);
            if(path==null)continue;
            byte[] button=atPath(share.v,path);
            String icon=text(first(button,2));
            String a11y=text(first(button,5)).toLowerCase(Locale.ROOT);
            if(!icon.equals("yt_outline_share") && !icon.equals("yt_outline_experimental_share") &&
                !a11y.startsWith("partager") && !a11y.startsWith("share") &&
                !text(first(button,17)).equals("id.video.share"))continue;
            int count=0;boolean exists=false;
            for(Field candidate:parsed)if(candidate.n==share.n && candidate.w==2) {
                count++;
                byte[] b=atPath(candidate.v,path);
                if(text(first(b,17)).equals("id.neszi.ask.fullscreen"))exists=true;
                else if(nativeAsk(b)){captureFullscreen(b);quickNative++;exists=true;}
            }
            if(exists)return model;
            if(count<2)continue; // Refuse an unconfirmed single-child container.
            byte[] own=makeButton(button,id,true);
            byte[] wrapped=wrapPath(path,own);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            for(Field f:parsed){write(out,f.raw);if(f==share)write(out,field(f.n,wrapped));}
            return out.toByteArray();
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();boolean changed=false;
        for(Field f:parsed){
            byte[] value=f.w==2?insertQuickModel(f.v,id,depth+1):f.v;
            if(value!=f.v){write(out,field(f.n,value));changed=true;}else write(out,f.raw);
        }
        return changed?out.toByteArray():model;
    }
    public static void observeWatch(Object response) {
        if (reported) return;
        reported = true;
        String status;
        try {
            String wire = new String(bytes(response), StandardCharsets.ISO_8859_1);
            boolean present = wire.contains("id.neszi.ask.direct") || wire.contains("yt_fill_spark") || wire.contains("yt_fill_experimental_spark");
            status = "Ask 0.8.2 : " + (present ? "modèle Ask présent" : "modèle Ask absent") +
                " · paysage : " + (quickChanged>0 ? "ajouté" : quickNative>0 ? "natif" : quickSeen>0 ? "format non pris en charge" : "non reçu") + " · panneau " + (nativeWatchPresentation == null ? (presentationCaptureAttempted ? "commande native non reconnue" : "natif à mémoriser (VPN)") : "natif mémorisé");
        } catch (Exception ex) { status = "Ask 0.5 : diagnostic " + ex.getClass().getSimpleName(); }
        if(!tablet()) {
            loadMinimal();
            status="Ask 0.8.2 : " + (minimalSeen ? "minimaliste " + (minimalVisual!=null?"mémorisé":minimalRestored?"ajouté":"observé") : "barre observée") +
                " · panneau " + (nativeWatchPresentation==null?"ouvrir Ask avec VPN pour mémoriser":"mémorisé");
        }
        if(tablet()) {
            loadMenu();
            status="Ask 0.8.2 : menu ⋯ " + (nativeMenuVisual==null?"à ouvrir pour mémoriser l’entrée native":"mémorisé") +
                " · panneau " + (nativeWatchPresentation==null?"à mémoriser":"mémorisé");
        }
        final String message = status;
        Log.i("AskNative", message);
        new Handler(Looper.getMainLooper()).post(() -> {
            try { Class.forName("app.morphe.extension.shared.Utils").getMethod("showToastLong", String.class).invoke(null, message); }
            catch (Exception ex) { Log.e("AskNative", "Status unavailable", ex); }
        });
    }
    // SingleColumnWatchNextResults -> metadata -> native Elements action list.
    static byte[] transformBytes(byte[] root) {
        return map(root, 1, primary -> map(primary, 49399797, secondary ->
            map(secondary, 1, contents -> map(contents, 216561405, metadata -> {
                return transformMetadata(metadata);
            }))));
    }
    static byte[] transformMetadata(byte[] metadata) {
        String id=text(first(metadata,2));if(!id.matches("[A-Za-z0-9_-]{11}"))return metadata;
        return map(metadata,1,tertiary -> map(tertiary,153515154,renderer -> map(renderer,172660663,element -> {
            if(!actionElement(element))return element;
            byte[] edited=editActionElement(element,id);
            // Only compact bars need the binding across Morphe's subsequent Classic/Minimal conversion.
            byte[] component=first(first(edited,1),168777401);
            if(first(first(first(first(component,5),413471385),1),2185).length>0 &&
                !text(first(edited,ELEMENT_VIDEO)).equals(id))edited=concat(without(edited,ELEMENT_VIDEO),field(ELEMENT_VIDEO,utf8(id)));
            return edited;
        })));
    }
    // Both phone and tablet watch primary contents. Never scan related-video menus outside this scope.
    static byte[] scanWatchMetadata(byte[] data,int depth,int[] budget) {
        if(depth>20 || ++budget[0]>4000)return data;
        List<Field> parsed;try{parsed=fields(data);}catch(IllegalArgumentException e){return data;}
        ByteArrayOutputStream out=new ByteArrayOutputStream();boolean changed=false;
        for(Field f:parsed){
            byte[] value=f.v;
            if(f.w==2) {
                if(f.n==216561405)value=transformMetadata(f.v);
                else if(f.n==51779735 || containsActionBar(f.v))value=scanWatchMetadata(f.v,depth+1,budget);
            }
            if(value!=f.v){write(out,field(f.n,value));changed=true;}else write(out,f.raw);
        }
        return changed?out.toByteArray():data;
    }
    static boolean containsActionBar(byte[] data) {
        return new String(data,StandardCharsets.ISO_8859_1).contains("video_action_bar.e");
    }
    static long number(byte[] message,int field) {
        for(Field f:fields(message))if(f.n==field && f.w==0)return readVarintValue(f.v);
        return 0;
    }
    static long readVarintValue(byte[] raw) {
        long value=0;int shift=0;for(byte b:raw){value|=(long)(b&127)<<shift;if((b&128)==0)return value;shift+=7;}return value;
    }
    static byte[] numberField(int n,long value) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();varint(out,((long)n)<<3);varint(out,value);return out.toByteArray();
    }
    // Only presentation is persisted, never an old video's command, panel or request tokens.
    static volatile byte[] nativeMenuVisual;
    static volatile String menuBefore = "", menuAfter = "";
    private static boolean menuLoaded;
    private static java.lang.ref.WeakReference<Object> ownMenu = new java.lang.ref.WeakReference<>(null);
    private static String lastMenuStatus = "";
    static synchronized void loadMenu() {
        if(menuLoaded)return;
        SharedPreferences p=preferences();if(p==null)return;
        menuLoaded=true;
        try {
            String saved=p.getString("tablet_menu_visual_v1", "");
            if(!saved.isEmpty()) {
                byte[] visual=Base64.decode(saved,Base64.NO_WRAP);
                // Revalidate persisted allowlisted fields before reuse.
                if(Arrays.equals(visual,menuVisual(visual)) && first(visual,1).length>0 && first(visual,2).length>0)
                    nativeMenuVisual=visual;
            }
            menuBefore=p.getString("tablet_menu_before_v1", "");
            menuAfter=p.getString("tablet_menu_after_v1", "");
        }catch(IllegalArgumentException e){Log.i("AskNative","Invalid menu cache ignored");}
    }
    static byte[] menuVisual(byte[] renderer) {
        ByteArrayOutputStream textOut=new ByteArrayOutputStream();
        // Keep run text only: no navigation commands, accessibility tracking or video entities.
        for(Field run:fields(first(renderer,1)))if(run.n==1 && run.w==2) {
            byte[] value=first(run.v,1);
            if(value.length>0)write(textOut,field(1,field(1,value)));
        }
        long icon=number(first(renderer,2),1);
        if(textOut.size()==0 || icon<=0)return new byte[0];
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        write(out,field(1,textOut.toByteArray()));write(out,field(2,numberField(1,icon)));
        return out.toByteArray();
    }
    // Native watch menus mix service, navigation and toggle renderers. Their outer tags differ.
    static byte[] menuRenderer(byte[] item) {
        List<Field> parsed;try{parsed=fields(item);}catch(IllegalArgumentException ignored){return new byte[0];}
        for(Field f:parsed)if(f.w==2 && f.n>1000000) {
            try {if(first(f.v,1).length>0 && number(first(f.v,2),1)>0)return f.v;}
            catch(IllegalArgumentException ignored){}
        }
        return new byte[0];
    }
    static String menuLabel(byte[] renderer) {
        StringBuilder label=new StringBuilder();
        for(Field f:fields(first(renderer,1)))if(f.n==1 && f.w==2)label.append(text(first(f.v,1)));
        return label.toString().trim().toLowerCase(Locale.ROOT).replace('’','\'');
    }
    static String menuIdentity(byte[] item) {
        byte[] renderer=menuRenderer(item);
        return number(first(renderer,2),1)+":"+menuLabel(renderer);
    }
    static boolean ownMenuItem(byte[] item) {
        return text(first(first(first(item,66441155),3),MARKER)).startsWith(PREFIX);
    }
    static boolean genuineMenuAsk(byte[] item) {
        if(ownMenuItem(item))return false;
        byte[] renderer=menuRenderer(item);String label=menuLabel(renderer);
        // SPARK also belongs to the official Summarize item, so the icon alone is insufficient.
        return (number(first(renderer,2),1)==1090 && (label.equals("posez une question") ||
            label.equals("poser une question") || label.equals("ask") || label.equals("ask a question"))) ||
            new String(first(renderer,3),StandardCharsets.ISO_8859_1).contains("PAyouchat");
    }
    static final class MenuShape {
        int share=-1, ask=-1; boolean dislike,save,own;
        MenuShape(List<byte[]> items) {
            for(int i=0;i<items.size();i++) {
                byte[] item=items.get(i),r=menuRenderer(item);String label=menuLabel(r);
                long icon=number(first(r,2),1);
                if(icon==48 || icon==100 || label.equals("partager") || label.equals("share"))share=i;
                if(label.equals("je n'aime pas") || label.equals("dislike"))dislike=true;
                if(label.equals("enregistrer") || label.equals("save"))save=true;
                if(genuineMenuAsk(item))ask=i;
                if(ownMenuItem(item))own=true;
            }
        }
        boolean watch(){return share>=0 && dislike && save;}
    }
    static void menuStatus(String status) {
        Log.i("AskNative","0.8.2: "+status);
        if(status.equals(lastMenuStatus))return;lastMenuStatus=status;
        new Handler(Looper.getMainLooper()).post(() -> {
            try{Class.forName("app.morphe.extension.shared.Utils").getMethod("showToastLong",String.class)
                .invoke(null,"Ask 0.8.2 : "+status);}catch(Exception ignored){}
        });
    }
    static synchronized void captureMenu(List<byte[]> items,int index) {
        loadMenu();byte[] renderer=menuRenderer(items.get(index));
        byte[] visual=menuVisual(renderer);if(visual.length==0)return;
        nativeMenuVisual=visual;
        menuBefore=index>0?menuIdentity(items.get(index-1)):"";
        menuAfter=index+1<items.size()?menuIdentity(items.get(index+1)):"";
        byte[] presentation=findWatchPresentation(first(renderer,3),0);
        if(presentation!=null)nativeWatchPresentation=presentation;
        SharedPreferences p=preferences();
        if(p!=null) {
            SharedPreferences.Editor e=p.edit().putString("tablet_menu_visual_v1",Base64.encodeToString(visual,Base64.NO_WRAP))
                .putString("tablet_menu_before_v1",menuBefore).putString("tablet_menu_after_v1",menuAfter);
            if(presentation!=null)e.putString("watch_presentation_v1","v1:"+Base64.encodeToString(presentation,Base64.NO_WRAP));
            e.apply();menuStatus("entrée et position du menu ⋯ mémorisées");
        }
    }
    static int menuPosition(List<byte[]> items,int share) {
        loadMenu();
        if(nativeMenuVisual!=null) {
            for(int i=0;i<items.size();i++)if(!menuBefore.isEmpty() && menuBefore.equals(menuIdentity(items.get(i))))return i+1;
            for(int i=0;i<items.size();i++)if(!menuAfter.isEmpty() && menuAfter.equals(menuIdentity(items.get(i))))return i;
        }
        return share+1;
    }
    static byte[] askMenuItem(String id) {
        loadMenu();ByteArrayOutputStream out=new ByteArrayOutputStream();
        if(nativeMenuVisual!=null)write(out,nativeMenuVisual);
        else {
            write(out,field(1,field(1,field(1,utf8("Posez une question")))));
            write(out,field(2,numberField(1,1090)));
        }
        write(out,field(3,field(MARKER,utf8(PREFIX+id))));
        return field(66441155,out.toByteArray());
    }
    static byte[] transformMenuBytes(byte[] data,String id,boolean scoped) {
        if(number(data,19)!=0)return data;
        List<byte[]> items=new ArrayList<>();
        for(Field f:fields(data))if(f.n==1 && f.w==2)items.add(f.v);
        MenuShape shape=new MenuShape(items);
        if(!(shape.watch() || (scoped && shape.share>=0 && items.size()>=2)))return data;
        if(shape.ask>=0){captureMenu(items,shape.ask);return data;}
        if(shape.own || id==null || !id.matches("[A-Za-z0-9_-]{11}"))return data;
        int at=menuPosition(items,shape.share),index=0;
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Field f:fields(data)) {
            if(f.n==1 && f.w==2 && index++==at)write(out,field(1,askMenuItem(id)));
            write(out,f.raw);
        }
        if(at==items.size())write(out,field(1,askMenuItem(id)));
        return out.toByteArray();
    }
    // Scoped fallback for service menus embedded in the compact watch model.
    static byte[] tabletMenus(byte[] data,String id,int depth,boolean[] recognized) {
        if(depth>12 || data.length>262144)return data;
        List<Field> parsed;try{parsed=fields(data);}catch(IllegalArgumentException e){return data;}
        List<byte[]> items=new ArrayList<>();for(Field f:parsed)if(f.n==1 && f.w==2)items.add(f.v);
        MenuShape shape=new MenuShape(items);
        if(shape.share>=0 && items.size()>=2 && number(data,19)==0) {
            recognized[0]=true;return transformMenuBytes(data,id,true);
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();boolean changed=false;
        for(Field f:parsed){byte[] value=f.w==2?tabletMenus(f.v,id,depth+1,recognized):f.v;
            if(value!=f.v){write(out,field(f.n,value));changed=true;}else write(out,f.raw);}
        return changed?out.toByteArray():data;
    }
    static String currentVideo() throws Exception {
        return (String)Class.forName("app.morphe.extension.youtube.patches.VideoInformation").getMethod("getVideoId").invoke(null);
    }
    // Hooked at the native menu presentation method ENTRY, including watch-page callers.
    public static Object onNativeMenuOpening(Object owner,Object menu) {
        if(!tablet() || menu==null)return menu;
        try {
            byte[] original=bytes(menu),changed=transformMenuBytes(original,currentVideo(),false);
            if(changed==original)return menu;
            Object parsed=parse(menu,changed);
            if(Class.forName(OFFICIAL+"$MenuAccess").isInstance(owner))ownMenu=new java.lang.ref.WeakReference<>(owner);
            menuStatus(nativeMenuVisual==null?"entrée ajoutée dans le menu ⋯":"entrée du menu ⋯ restaurée");
            return parsed;
        }catch(Exception e){Log.e("AskNative","Native watch menu preserved",e);return menu;}
    }
    static Object rowItem(Object row) throws Exception {
        if(access().isInstance(row))return row;
        Class<?> api=Class.forName(OFFICIAL+"$MenuRowAccess");
        return api.isInstance(row)?api.getMethod("patch_getMenuItem").invoke(row):null;
    }
    public static void onNativeRowCreated(Object owner,Object item) {
        try {if(ownMenuItem(bytes(item)))ownMenu=new java.lang.ref.WeakReference<>(owner);}
        catch(Exception ignored){}
    }
    @SuppressWarnings("unchecked")
    public static void onNativePopupShown(Object popup) {
        if(!tablet())return;
        try {
            Class<?> api=Class.forName(OFFICIAL+"$MenuAccess");
            List<Object> rows=(List<Object>)api.getMethod("patch_getRows").invoke(popup);
            List<byte[]> items=new ArrayList<>();List<Object> messages=new ArrayList<>();
            for(Object row:rows) {
                Object message=rowItem(row);messages.add(message);
                items.add(message==null?new byte[0]:bytes(message));
            }
            MenuShape shape=new MenuShape(items);if(!shape.watch())return;
            if(shape.ask>=0){captureMenu(items,shape.ask);return;}
            ownMenu=new java.lang.ref.WeakReference<>(popup);if(shape.own)return;
            String id=currentVideo();if(id==null || !id.matches("[A-Za-z0-9_-]{11}"))return;
            Object exemplar=messages.get(shape.share),ask=parse(exemplar,askMenuItem(id));
            if(rows.get(shape.share)!=exemplar)
                ask=api.getMethod("patch_createRow",Class.forName(OFFICIAL+"$MenuItemAccess")).invoke(popup,ask);
            if(ask!=null) {
                rows.add(menuPosition(items,shape.share),ask);
                menuStatus(nativeMenuVisual==null?"entrée ajoutée dans le menu ⋯":"entrée du menu ⋯ restaurée");
            }
        }catch(Exception e){Log.e("AskNative","Unsupported tablet popup preserved",e);}
    }
    public static boolean onNativeMenuTap(Object item) {
        try {
            byte[] service=first(bytes(item),66441155);
            String marker=text(first(first(service,3),MARKER));
            if(!marker.startsWith(PREFIX))return false;
            String id=marker.substring(PREFIX.length());if(!id.matches("[A-Za-z0-9_-]{11}"))return true;
            Class<?> official=Class.forName(OFFICIAL);
            Method open=official.getDeclaredMethod("openAskPanel",String.class);open.setAccessible(true);
            try {
                Object popup=ownMenu.get();
                if(popup==null) {
                    java.lang.reflect.Field menu=official.getDeclaredField("menuRef");menu.setAccessible(true);
                    popup=((java.lang.ref.Reference<?>)menu.get(null)).get();
                }
                if(popup!=null)Class.forName(OFFICIAL+"$MenuAccess").getMethod("patch_dismissPopup").invoke(popup);
            }catch(Exception ignored){}
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try{directFullscreen=false;directVideo=id;directTime=SystemClock.uptimeMillis();open.invoke(null,id);}
                catch(Exception e){Log.e("AskNative","Native menu opening failed",e);}
            },150);
            return true;
        }catch(Exception e){Log.e("AskNative","Native menu route failed",e);return false;}
    }
    static final int ELEMENT_VIDEO = 500000002;
    static final int OVERFLOW = 37098, SAVE = 484598023;
    static volatile byte[] minimalVisual;
    static String minimalBefore="", minimalAfter="";
    private static boolean minimalLoaded;
    private static volatile boolean minimalSeen, minimalRestored;
    static synchronized void loadMinimal() {
        if(minimalLoaded)return;SharedPreferences p=preferences();if(p==null)return;
        minimalLoaded=true;
        try {
            String saved=p.getString("phone_minimal_visual_v1","");
            if(!saved.isEmpty())minimalVisual=Base64.decode(saved,Base64.NO_WRAP);
            minimalBefore=p.getString("phone_minimal_before_v1","");
            minimalAfter=p.getString("phone_minimal_after_v1","");
        }catch(IllegalArgumentException e){minimalVisual=null;}
    }
    static boolean ownContent(byte[] content) {
        return text(first(first(content,SECONDARY),17)).startsWith("id.neszi.ask.");
    }
    static boolean askContent(byte[] content) {
        return ownContent(content) || nativeAsk(first(content,SECONDARY));
    }
    static String contentKey(byte[] content) {
        byte[] button=first(content,SECONDARY);
        if(button.length>0)return "button:"+text(first(button,2));
        for(Field f:fields(content))if(f.w==2)return "type:"+f.n;
        return "";
    }
    static boolean shareContent(byte[] content) {
        byte[] b=first(content,SECONDARY);
        String icon=text(first(b,2)),id=text(first(b,17));
        return icon.equals("yt_outline_share") || icon.equals("yt_outline_experimental_share") || id.startsWith("id.video.share");
    }
    static byte[] concat(byte[] a,byte[] b) {ByteArrayOutputStream out=new ByteArrayOutputStream();write(out,a);write(out,b);return out.toByteArray();}
    static List<byte[]> repeated(byte[] data,int n) {
        List<byte[]> out=new ArrayList<>();for(Field f:fields(data))if(f.n==n && f.w==2)out.add(f.v);return out;
    }
    static byte[] replaceRepeated(byte[] data,int n,List<byte[]> values) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();boolean emitted=false;
        for(Field f:fields(data)) {
            if(f.n==n && f.w==2){if(!emitted){for(byte[] v:values)write(out,field(n,v));emitted=true;}}
            else write(out,f.raw);
        }
        if(!emitted)for(byte[] v:values)write(out,field(n,v));
        byte[] result=out.toByteArray();return Arrays.equals(result,data)?data:result;
    }
    static void captureMinimal(List<byte[]> contents) {
        loadMinimal();
        for(int i=0;i<contents.size();i++) {
            byte[] b=first(contents.get(i),SECONDARY);
            if(ownContent(contents.get(i)) || !nativeAsk(b))continue;
            captureVisual(b);
            byte[] visual=iconAndStyle(b,true);
            if(visual.length==0)continue;
            String before=i>0?contentKey(contents.get(i-1)):"";
            String after=i+1<contents.size()?contentKey(contents.get(i+1)):"";
            boolean changed=!Arrays.equals(minimalVisual,visual) || !before.equals(minimalBefore) || !after.equals(minimalAfter);
            minimalVisual=visual;minimalBefore=before;minimalAfter=after;
            SharedPreferences p=preferences();
            if(p!=null && changed) {
                p.edit().putString("phone_minimal_visual_v1",Base64.encodeToString(visual,Base64.NO_WRAP))
                    .putString("phone_minimal_before_v1",before).putString("phone_minimal_after_v1",after).apply();
                menuStatus("bouton minimaliste et position mémorisés");
            }
        }
    }
    static byte[] minimalButton(String id,boolean title) {
        byte[] b=makeButton(null,id,false);
        if(minimalVisual!=null) {
            b=field(4,field(169495254,field(MARKER,utf8(PREFIX+id))));
            b=concat(b,concat(minimalVisual,field(17,utf8("id.neszi.ask.direct"))));
        }
        // Official layoutButton values. Menu/shown copy keeps the label for classic conversion.
        for(int n:new int[]{3,5,11,13,40})b=without(b,n);
        b=concat(b,concat(numberField(11,3),concat(numberField(13,4),numberField(40,1))));
        b=concat(b,field(5,utf8("Poser une question à Gemini")));
        if(title || (minimalVisual==null && genuineVisual==null))b=concat(b,field(3,utf8("Poser une question")));
        return b;
    }
    static byte[] minimalBar(byte[] data,String id) {
        if(tablet())return data; // Tablet native menus retain their dedicated path.
        minimalSeen=true;loadMinimal();
        List<byte[]> actions=repeated(data,1),contents=new ArrayList<>();
        byte[] overflow=null;int overflowIndex=-1;
        for(int i=0;i<actions.size();i++) {
            byte[] c=first(actions.get(i),10);contents.add(c);
            if(first(c,OVERFLOW).length>0){if(overflow!=null)return data;overflow=first(c,OVERFLOW);overflowIndex=i;}
        }
        captureMinimal(contents);
        if(overflow!=null)for(int list:new int[]{2,3})for(byte[] c:repeated(overflow,list))
            if(!ownContent(c) && nativeAsk(first(c,SECONDARY)))captureVisual(first(c,SECONDARY));
        for(byte[] c:contents)if(!ownContent(c) && nativeAsk(first(c,SECONDARY)))return data;
        if(overflow==null || id==null || !id.matches("[A-Za-z0-9_-]{11}"))return data;
        boolean share=false;for(byte[] c:contents)share|=shareContent(c);if(!share)return data;
        List<byte[]> visible=new ArrayList<>(),menu=new ArrayList<>(),shown=new ArrayList<>();
        for(byte[] action:actions)if(!askContent(first(action,10)))visible.add(action);
        for(byte[] c:repeated(overflow,2))if(!askContent(c))menu.add(c);
        long max=number(first(data,7),4);
        if(max<=0)max=actions.size(); // Preserve the observed capacity, do not grow the row.
        // One slot for Ask; keep Save accessible in the overflow, never discard its command/entities.
        if(visible.size()+1>max) {
            int saveAt=-1;for(int i=0;i<visible.size();i++)if(first(first(visible.get(i),10),SAVE).length>0){saveAt=i;break;}
            if(saveAt<0)return data;
            byte[] save=first(visible.remove(saveAt),10);
            for(byte[] rich:repeated(overflow,3))if(first(rich,SAVE).length>0){save=rich;break;}
            boolean exists=false;for(byte[] c:menu)exists|=first(c,SAVE).length>0;
            if(!exists)menu.add(0,save);
        }
        if(visible.size()+1>max)return data;
        int at=-1;
        if(minimalVisual!=null && !minimalBefore.isEmpty())for(int i=0;i<visible.size();i++)
            if(minimalBefore.equals(contentKey(first(visible.get(i),10))))at=i+1;
        if(at<0 && minimalVisual!=null && !minimalAfter.isEmpty())for(int i=0;i<visible.size();i++)
            if(minimalAfter.equals(contentKey(first(visible.get(i),10))))at=i;
        if(at<0)for(int i=0;i<visible.size();i++)if(shareContent(first(visible.get(i),10)))at=i+1;
        if(at<0)return data;
        byte[] ask=field(SECONDARY,minimalButton(id,false));
        visible.add(at,field(10,ask));
        for(byte[] action:visible) {
            byte[] c=first(action,10);if(first(c,OVERFLOW).length>0)continue;
            if(askContent(c))shown.add(field(SECONDARY,minimalButton(id,true)));
            else {
                byte[] rich=c;for(byte[] old:repeated(overflow,3))if(contentKey(old).equals(contentKey(c))){rich=old;break;}
                shown.add(rich);
            }
        }
        final byte[] newOverflow=replaceRepeated(replaceRepeated(overflow,2,menu),3,shown);
        for(int i=0;i<visible.size();i++) {
            byte[] action=visible.get(i);
            if(first(first(action,10),OVERFLOW).length>0)
                visible.set(i,map(action,10,c -> map(c,OVERFLOW,old -> newOverflow)));
        }
        byte[] result=replaceRepeated(data,1,visible);
        if(result!=data){minimalRestored=true;menuStatus("bouton minimaliste restauré");}
        return result;
    }
    static boolean actionElement(byte[] element) {
        String identifier=text(first(first(first(element,2),183314536),1));
        if(identifier.startsWith("video_action_bar.e") || identifier.startsWith("compactify_video_action_bar.e"))return true;
        byte[] component=first(first(element,1),168777401);
        String uri=text(first(first(first(component,3),172035250),1));
        return uri.contains("compactify_video_action_bar.eml") || uri.contains("video_action_bar.eml");
    }
    static byte[] editActionElement(byte[] element,String id) {
        return map(element,1,type -> map(type,168777401,component -> map(component,5,model -> {
            byte[] normal=map(model,389155117,bar -> map(bar,17,data -> insert(data,id)));
            return map(normal,413471385,youtube -> map(youtube,1,view -> map(view,2185,data ->
                number(data,3)==6 ? minimalBar(data,id) : insert(data,id,true))));
        })));
    }
    private static final ThreadLocal<byte[]> layoutInput=new ThreadLocal<>();
    public static void beforeActionBarLayout(byte[] element) {
        layoutInput.remove();
        try {
            if(element==null || !containsActionBar(element) || !actionElement(element))return;
            layoutInput.set(element);
            byte[] bar=compactData(element);
            if(number(bar,3)==6)minimalBar(bar,null); // Observe original native visual before conversion.
        }catch(Exception ignored){layoutInput.remove();}
    }
    static byte[] compactData(byte[] element) {
        return first(first(first(first(first(first(element,1),168777401),5),413471385),1),2185);
    }
    static byte[] restoreClassicSave(byte[] element,byte[] original) {
        if(original==null)return element;
        byte[] bar=compactData(element),old=compactData(original);
        if(number(bar,3)!=4 || number(old,3)!=6 ||
            !Arrays.equals(first(element,ELEMENT_VIDEO),first(original,ELEMENT_VIDEO)))return element;
        List<byte[]> actions=repeated(bar,1);byte[] save=null,style=null;
        for(byte[] action:actions) {
            byte[] c=first(action,10);if(first(c,SAVE).length>0)return element;
            if(shareContent(c))style=first(c,SECONDARY);
        }
        for(byte[] action:repeated(old,1)) {
            byte[] overflow=first(first(action,10),OVERFLOW);
            for(byte[] c:repeated(overflow,2))if(first(c,SAVE).length>0)save=c;
        }
        if(save==null)return element;
        final byte[] classicStyle=style!=null?style:concat(numberField(11,5),numberField(13,2));
        byte[] converted=map(save,SAVE,b -> map(b,1,h -> map(h,479644279,t -> {
            byte[] defaults=map(t,1,v -> map(v,SECONDARY,button -> compactStyle(button,classicStyle)));
            return map(defaults,2,v -> map(v,SECONDARY,button -> compactStyle(button,classicStyle)));
        })));
        actions.add(field(10,converted));
        final byte[] restored=replaceRepeated(bar,1,actions);
        return map(element,1,t -> map(t,168777401,c -> map(c,5,m -> map(m,413471385,l -> map(l,1,v -> map(v,2185,b -> restored))))));
    }
    public static byte[] afterActionBarLayout(byte[] element) {
        byte[] original=layoutInput.get();layoutInput.remove();
        if(element==null)return null;
        try {
            if(!containsActionBar(element) || !actionElement(element))return element;
            String id=text(first(element,ELEMENT_VIDEO));
            // Even an unbound element can teach the native visual; it cannot receive a stale-video command.
            byte[] data=first(first(first(first(first(first(element,1),168777401),5),413471385),1),2185);
            if(number(data,3)!=6 && number(data,3)!=4)return element;
            if(!id.matches("[A-Za-z0-9_-]{11}")){minimalBar(data,null);return element;}
            return editActionElement(restoreClassicSave(element,original),id);
        }catch(Exception e){Log.e("AskNative","Minimal layout unchanged",e);return element;}
    }
    static byte[] insert(byte[] data, String id) { return insert(data,id,false); }
    static byte[] insert(byte[] data, String id, boolean compact) {
        loadVisual();
        if(compact && number(data,3)==6 && !tablet())return minimalBar(data,id);
        if(compact && tablet()) {
            boolean[] menu={false};byte[] transformed=tabletMenus(data,id,0,menu);
            // If the menu is created only on tap, onNativePopupShown supplies the item then.
            // Never force an extra visible tablet compact-row action.
            return transformed;
        }
        List<Field> fields = fields(data);
        byte[] share = null, template = null;
        int actionCount = 0;
        for (Field f : fields) if (f.n == 1 && f.w == 2) {
            actionCount++;
            byte[] button = first(first(f.v, 10), SECONDARY);
            if (template == null && button.length > 0) template = button;
            String icon = text(first(button, 2));
            if (text(first(button,17)).equals("id.neszi.ask.direct")) return data;
            if (nativeAsk(button)) { captureVisual(button); return data; }
            String a11y=text(first(button,5)).toLowerCase(Locale.ROOT);
            String label=text(first(button,3)).toLowerCase(Locale.ROOT);
            if (icon.equals("yt_outline_share") || icon.equals("yt_outline_experimental_share") ||
                a11y.startsWith("partager") || a11y.startsWith("share") || label.equals("partager") || label.equals("share") ||
                text(first(button,17)).equals("id.video.share")) share = f.v;
        }
        if (actionCount == 0) return data;
        byte[] style = share != null ? first(first(share, 10), SECONDARY) : template;
        byte[] ownButton = makeButton(style,id,false);
        if(style!=null)ownButton=compactStyle(ownButton,style);
        byte[] action = field(10, field(SECONDARY, ownButton));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean added = false;
        for (Field f : fields) {
            write(out, f.raw);
            if (!added && f.n == 1 && (share == null || Arrays.equals(f.v, share))) {
                write(out, field(1, action)); added = true;
            }
        }
        return out.toByteArray();
    }
    // Compact bars use their own visual style; do not import portrait pill styling.
    static byte[] compactStyle(byte[] button,byte[] style) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Field f:fields(button))if(f.n!=7 && f.n!=11 && f.n!=13 && f.n!=40)write(out,f.raw);
        for(Field f:fields(style))if(f.n==7 || f.n==11 || f.n==13 || f.n==40)write(out,f.raw);
        return out.toByteArray();
    }
    static byte[] makeButton(byte[] style,String id,boolean fullscreen) {
        loadVisual();
        ByteArrayOutputStream button = new ByteArrayOutputStream();
        byte[] visual=fullscreen && genuineFullscreenVisual!=null ? genuineFullscreenVisual : genuineVisual;
        if (visual != null) {
            if(fullscreen && genuineFullscreenVisual==null) {
                // Use the real icon, but the fullscreen Share item's own style rather than portrait pill styles.
                write(button,iconAndStyle(visual,false));
                for(Field f:fields(style))if(f.n==7 || f.n==11 || f.n==13)write(button,f.raw);
                write(button,field(5,utf8("Poser une question à Gemini")));
            } else write(button,visualForDevice(visual,!fullscreen && tablet()));
        } else {
            // Honest bootstrap: no fabricated icon, original visual must be captured once when eligible.
            if (style == null) write(button,new byte[]{56,9,88,3,104,2});
            else for (Field f : fields(style)) if ((f.n==7 || f.n==11 || f.n==13) && f.w==0) write(button,f.raw);
            write(button,field(3,utf8("Gemini")));
            write(button,field(5,utf8("Poser une question à Gemini")));
        }
        write(button, field(17, utf8(fullscreen ? "id.neszi.ask.fullscreen" : "id.neszi.ask.direct")));
        write(button, field(4, field(169495254, field(MARKER, utf8((fullscreen ? FULLSCREEN_PREFIX : PREFIX) + id)))));
        return button.toByteArray();
    }
    /** Called at the native Litho -> Innertube command adapter, before executor invocation. */
    public static Object route(Object command, Object executor) {
        try {
            byte[] commandBytes=bytes(command);
            String marker = text(first(commandBytes, MARKER));
            if(!marker.startsWith(PREFIX) && !marker.startsWith(FULLSCREEN_PREFIX)) {
                byte[] opening=findWatchPresentation(commandBytes,0);
                Context ctx=context();
                if(opening!=null && !tablet() && ctx!=null && ctx.getResources().getConfiguration().orientation==1 &&
                    !Arrays.equals(opening,nativeWatchPresentation)) {
                    nativeWatchPresentation=opening;
                    SharedPreferences p=preferences();
                    if(p!=null)p.edit().putString("watch_presentation_v1","v1:"+Base64.encodeToString(opening,Base64.NO_WRAP)).apply();
                    menuStatus("ouverture native du panneau mémorisée au clic");
                }
            }
            boolean fullscreen=marker.startsWith(FULLSCREEN_PREFIX);
            if (!fullscreen && !marker.startsWith(PREFIX)) return command;
            String id = marker.substring((fullscreen ? FULLSCREEN_PREFIX : PREFIX).length());
            Object noop = parse(command, new byte[0]);
            if (!id.matches("[A-Za-z0-9_-]{11}")) return noop;
            Class<?> official = Class.forName(OFFICIAL);
            Class<?> executorType = Class.forName(OFFICIAL + "$CommandExecutorAccess");
            official.getMethod("setCommandExecutor", executorType).invoke(null, executor);
            Method open = official.getDeclaredMethod("openAskPanel", String.class);
            open.setAccessible(true);
            // Finish native tap callbacks before opening the engagement panel.
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    directFullscreen=fullscreen; directVideo = id; directTime = SystemClock.uptimeMillis();
                    open.invoke(null, id);
                } catch (Exception ex) { Log.e("AskNative", "Official Ask opening failed", ex); }
            });
            return noop;
        } catch (Exception ex) {
            Log.e("AskNative", "Command route failed", ex);
            return command; // Own marker has no executable endpoint, never invokes Share.
        }
    }
    /** Official showAskPanel hook, after pending-summary fields are assigned. */
    public static void beginShow(String id) {
        boolean direct = id.equals(directVideo) && SystemClock.uptimeMillis() - directTime <= 15000;
        currentDirectVideo = direct ? id : null;
        currentFullscreen=direct && directFullscreen;
        fullscreenPanel.remove();
        if (direct) directVideo = null;
        directPanelVideo.remove();
        directShow.set(direct);
    }
    public static void beforeShow() {
        boolean direct = Boolean.TRUE.equals(directShow.get());
        directShow.remove();
        if (!direct) return;
        // Keep provenance for the command rewrite after the official fields have been assigned.
        directPanelVideo.set(currentDirectVideo);
        fullscreenPanel.set(currentFullscreen);
        try {
            Class<?> official = Class.forName(OFFICIAL);
            java.lang.reflect.Field lockField = official.getDeclaredField("pendingLock");
            lockField.setAccessible(true);
            synchronized (lockField.get(null)) {
                for (String n : new String[]{"pendingWelcomeToken", "pendingSummarizeToken"}) {
                    java.lang.reflect.Field f = official.getDeclaredField(n); f.setAccessible(true); f.set(null, null);
                }
                java.lang.reflect.Field f = official.getDeclaredField("pendingSuggestionSelectionId");
                f.setAccessible(true); f.setLong(null, -1);
            }
            Log.i("AskNative", "Opening current-video Ask without automatic summary");
        } catch (Exception ex) { Log.e("AskNative", "Could not disable automatic summary", ex); }
    }
    public static byte[] adaptPanelCommand(byte[] command) {
        String video = directPanelVideo.get();
        directPanelVideo.remove();
        boolean fullscreen=Boolean.TRUE.equals(fullscreenPanel.get());
        fullscreenPanel.remove();
        if (video == null) return command;
        byte[] fresh = null;
        try {
            Class<?> panel = Class.forName("app.morphe.extension.youtube.patches.ask.AskPanelCommand");
            java.lang.reflect.Field id = panel.getDeclaredField("templateVideoId"); id.setAccessible(true);
            java.lang.reflect.Field model = panel.getDeclaredField("templatePanel"); model.setAccessible(true);
            if (video.equals(id.get(null))) {
                fresh = (byte[]) model.get(null);
                // Preserve official per-panel selection isolation when restoring the current video's prompts.
                java.lang.reflect.Field key = panel.getDeclaredField("lastSelectionEntityKey"); key.setAccessible(true);
                String replacement = (String) key.get(null);
                if (fresh != null && replacement != null) {
                    Method retarget = panel.getDeclaredMethod("retarget",String.class,String.class,String.class,int.class,int[].class);
                    retarget.setAccessible(true);
                    String rewritten = (String) retarget.invoke(null,new String(fresh,StandardCharsets.ISO_8859_1),
                        "selected_index_entity",replacement,0,new int[1]);
                    fresh = rewritten.getBytes(StandardCharsets.ISO_8859_1);
                }
            }
        } catch (Exception ignored) { }
        return watchCommand(command,fresh,nativePanelIdentifier,fullscreen ? nativeFullscreenPresentation : nativeWatchPresentation);
    }
    static byte[] watchCommand(byte[] command, byte[] freshCurrentPanel, byte[] capturedIdentifier) {
        return watchCommand(command,freshCurrentPanel,capturedIdentifier,null);
    }
    static byte[] watchCommand(byte[] command, byte[] freshCurrentPanel, byte[] capturedIdentifier, byte[] presentation) {
        return map(command,138681778,endpoint -> {
            byte[] renderer=first(first(endpoint,2),138681066);
            if (freshCurrentPanel!=null) renderer=freshCurrentPanel;
            byte[] identifier=first(renderer,18);
            if (!text(first(identifier,3)).equals("PAyouchat")) identifier=capturedIdentifier;
            if (presentation==null && (identifier==null || !text(first(identifier,3)).equals("PAyouchat"))) {
                Log.i("AskNative","Native watch opening unavailable; preserving previous behavior");
                return endpoint;
            }
            ByteArrayOutputStream panelOut=new ByteArrayOutputStream();
            for (Field f:fields(renderer)) if (f.n!=20) write(panelOut,f.raw);
            varint(panelOut,20L<<3); varint(panelOut,1);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            for(Field f:fields(endpoint)) {
                if(f.n==2 || f.n==10)continue;
                if(presentation!=null && (f.n==1 || f.n==5 || f.n==6 || f.n==8 || f.n==16))continue;
                write(out,f.raw);
            }
            write(out,field(2,field(138681066,panelOut.toByteArray())));
            if(presentation!=null) {
                // Keep the ORIGINAL target form and absence of flags as well as their values.
                // In particular, don't convert legacy target1 into identifier10/BROWSE2.
                write(out,presentation);
                Log.i("AskNative","0.8.2: applying captured native watch target and presentation");
            } else {
                write(out,field(10,identifier));
            }
            return out.toByteArray();
        });
    }
    interface Edit { byte[] apply(byte[] bytes); }
    static byte[] map(byte[] data, int number, Edit edit) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); boolean changed = false;
        for (Field f : fields(data)) {
            byte[] value = f.n == number && f.w == 2 ? edit.apply(f.v) : f.v;
            if (value != f.v) { write(out, field(f.n, value)); changed = true; }
            else write(out, f.raw);
        }
        return changed ? out.toByteArray() : data;
    }
    static byte[] first(byte[] data, int n) {
        if (data != null) for (Field f : fields(data)) if (f.n == n && f.w == 2) return f.v;
        return new byte[0];
    }
    static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    static String text(byte[] b) { return new String(b, StandardCharsets.UTF_8); }
    static void write(ByteArrayOutputStream out, byte[] b) { out.write(b, 0, b.length); }
    static void varint(ByteArrayOutputStream out, long v) {
        while ((v & ~127L) != 0) { out.write((int)(v & 127) | 128); v >>>= 7; } out.write((int)v);
    }
    static byte[] field(int n, byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); varint(out, ((long)n << 3) | 2); varint(out, b.length); write(out,b); return out.toByteArray();
    }
    static final class Field {
        final int n,w; final byte[] v,raw;
        Field(int n,int w,byte[] v,byte[] raw) { this.n=n;this.w=w;this.v=v;this.raw=raw; }
    }
    static long read(byte[] b,int[] p) {
        long r=0; for(int s=0;s<64;s+=7) { if(p[0]>=b.length)throw new IllegalArgumentException("truncated varint");int v=b[p[0]++]&255;r|=(long)(v&127)<<s;if((v&128)==0)return r; }throw new IllegalArgumentException("overflow");
    }
    static List<Field> fields(byte[] b) {
        ArrayList<Field> result=new ArrayList<>();int[] p={0};
        while(p[0]<b.length) {
            int start=p[0]; long tag=read(b,p);int n=(int)(tag>>>3),w=(int)(tag&7);if(n==0)throw new IllegalArgumentException("zero field");
            int value=p[0];long len;
            switch(w) {
                case 0: read(b,p);break;
                case 1: p[0]+=8;break;
                case 2: len=read(b,p);value=p[0];if(len<0||len>b.length-value)throw new IllegalArgumentException("length");p[0]+=(int)len;break;
                case 5: p[0]+=4;break;
                default:throw new IllegalArgumentException("wire type");
            }
            if(p[0]>b.length)throw new IllegalArgumentException("truncated");
            result.add(new Field(n,w,Arrays.copyOfRange(b,value,p[0]),Arrays.copyOfRange(b,start,p[0])));
        }
        return result;
    }
}
