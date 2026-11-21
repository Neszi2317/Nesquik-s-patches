package app.morphe.extension.newx.postfilter;
import android.content.*;import org.json.*;import java.util.*;import java.io.*;import app.morphe.extension.shared.Utils;
public final class AdvancedConfig {
    private static volatile AdvancedConfig current;
    private static final String PREFS="piko_advanced_global_filter_v1", KEY="config";
    public final boolean enabled,strict,posts,trends,search,notifications,accounts,authors,bios,quotes,replies,links,fold;
    public final String mode;public final List<String> keywords;public final AdvancedMatcher matcher;
    public AdvancedConfig(JSONObject j)throws JSONException {
        enabled=j.optBoolean("enabled",true);strict=j.optBoolean("strict",false);posts=j.optBoolean("posts",true);trends=j.optBoolean("trends",true);
        notifications=j.optBoolean("notifications",true);search=j.optBoolean("search",true);accounts=j.optBoolean("accounts",true);authors=j.optBoolean("authors",true);bios=j.optBoolean("bios",true);quotes=j.optBoolean("quotes",true);
        replies=j.optBoolean("replies",true);links=j.optBoolean("links",true);fold=j.optBoolean("foldAccents",true);mode=j.optString("mode","substring");
        JSONArray a=j.optJSONArray("keywords");List<String> k=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++)k.add(a.getString(i));
        matcher=new AdvancedMatcher(k,mode,fold);keywords=matcher.rules();
    }
    public JSONObject json()throws JSONException {JSONObject j=new JSONObject();j.put("keywords",new JSONArray(keywords));j.put("mode",mode);j.put("enabled",enabled);j.put("strict",strict);
        j.put("posts",posts);j.put("trends",trends);j.put("accounts",accounts);j.put("search",search);j.put("notifications",notifications);j.put("authors",authors);j.put("bios",bios);j.put("quotes",quotes);j.put("replies",replies);j.put("links",links);j.put("foldAccents",fold);return j;}
    public static JSONObject seed()throws Exception {try(InputStream in=Utils.getContext().getAssets().open("piko-advanced-filter.json")){return new JSONObject(AdvancedLists.read(in));}}
    public static AdvancedConfig get(){AdvancedConfig c=current;if(c!=null)return c;synchronized(AdvancedConfig.class){if(current!=null)return current;
        try{String saved=Utils.getContext().getSharedPreferences(PREFS,0).getString(KEY,null);current=new AdvancedConfig(saved==null?seed():new JSONObject(saved));return current;}
        catch(Exception e){throw new IllegalStateException("Configuration Advanced Filter illisible",e);}}}
    /** Compile/validate before persistence. Called from the settings worker, never its UI thread. */
    public static synchronized void save(JSONObject data)throws Exception {AdvancedConfig c=new AdvancedConfig(data);
        if(!Utils.getContext().getSharedPreferences(PREFS,0).edit().putString(KEY,c.json().toString()).commit())throw new IOException("Échec de sauvegarde locale");current=c;}
    public static void reset()throws Exception{save(seed());}
}
