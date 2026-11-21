package local.reddit.filter;

import android.app.*;
import android.content.*;
import android.preference.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Derived from Santodan RedditContentFilter: preferences, listing filtering,
 * Link accessors/registration and settings. 2026.47.0 port; targeted post flair paths, no network code. */
@SuppressWarnings({"unused","deprecation"})
public final class RedditContentFilter {
    private static final String KEYWORDS="redditplusplus_filter_keywords";
    private static final String SUBREDDITS="blocked_subreddits";
    private static volatile SubredditRules communities=new SubredditRules("");
    private static final String STORE="reddit_keyword_filter_v1";
    private static volatile Context applicationContext;
    private static volatile KeywordRules snapshot;
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Map<String,Field> FIELDS=new ConcurrentHashMap<>();
    private static final Map<String,Method> METHODS=new ConcurrentHashMap<>();
    // Bounded cache of actual Link text, never a permanent cache of decisions.
    private static final Map<String,PostInfo> POSTS=Collections.synchronizedMap(new LinkedHashMap<String,PostInfo>(256,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<String,PostInfo> e){return size()>2000;}
    });
    private static final Map<String,String> POST_COMMUNITIES=Collections.synchronizedMap(new LinkedHashMap<String,String>(256,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<String,String> e){return size()>2000;}
    });
    // Bounded aggregate diagnostics: class names/counters only; no titles, IDs or browsing history.
    private static final Map<String,java.util.concurrent.atomic.AtomicLong> DIAGNOSTICS=new ConcurrentHashMap<>();
    private static void count(String event){
        java.util.concurrent.atomic.AtomicLong counter=DIAGNOSTICS.get(event);
        if(counter==null){synchronized(DIAGNOSTICS){counter=DIAGNOSTICS.get(event);if(counter==null){if(DIAGNOSTICS.size()>=64)return;counter=new java.util.concurrent.atomic.AtomicLong();DIAGNOSTICS.put(event,counter);}}}
        counter.incrementAndGet();
    }
    private static String postKey(String id){return id.isEmpty()?"":id.startsWith("t3_")?id:"t3_"+id;}
    /** Exact GraphQL post -> subreddit path, before display-only metadata is produced. */
    public static void rememberGraphqlPost(Object fragment){
        if(fragment==null)return;
        try {
            String id=postKey(string(field(fragment,"a")));
            Object subreddit=field(field(field(fragment,"d"),"i"),"a");
            String name=string(field(subreddit,"c"));
            if(!id.isEmpty()&&!name.isEmpty()){POST_COMMUNITIES.put(id,name);count("source.graphql");}
        }catch(Exception ignored){count("error.graphql");}
    }
    public static String diagnostics(){
        KeywordRules k=rules();StringBuilder out=new StringBuilder("Reddit Local Keyword Filters 2.3.0\n");
        out.append("Runtime initialisé : ").append(k!=null).append("\nMots-clés : ").append(k==null?0:k.count).append("\nSubreddits : ").append(communities.count).append('\n');
        for(String key:new TreeSet<String>(DIAGNOSTICS.keySet()))out.append(key).append(" = ").append(DIAGNOSTICS.get(key).get()).append('\n');
        return out.toString();
    }
    public static boolean isSubredditBlocked(String name){rules();return communities.matches(name);}
    private static final Map<Object,String> RICH_TEXT=Collections.synchronizedMap(new WeakHashMap<Object,String>());
    private RedditContentFilter(){}
    public static void initialize(Context context) {
        if(context==null)return;
        applicationContext=context.getApplicationContext();
        if(snapshot==null) synchronized(RedditContentFilter.class) {
            if(snapshot==null) { seedConfiguration(context); communities=new SubredditRules(prefs(context).getString(SUBREDDITS,"")); snapshot=new KeywordRules(prefs(context).getString(KEYWORDS,"")); }
        }
    }
    private static void seedConfiguration(Context context) {
        SharedPreferences p=prefs(context);SharedPreferences.Editor edit=p.edit();
        String[] keys={KEYWORDS,SUBREDDITS};
        String[] defaults={BundledConfig.keywords(null,null),BundledConfig.subreddits(null,null)};
        for(int i=0;i<keys.length;i++){
            String seed="manager_seed_"+keys[i];String previous=p.getString(seed,null),next=defaults[i];
            if(BundledConfig.shouldApply(previous,next))edit.putString(keys[i],next);
            edit.putString(seed,next);
        }
        edit.apply();
    }
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences(STORE,Context.MODE_PRIVATE);}
    private static KeywordRules rules() {
        if(snapshot==null) {
            try {
                Object c=Class.forName("app.morphe.extension.shared.Utils").getMethod("getContext").invoke(null);
                if(c instanceof Context)initialize((Context)c);
            }catch(Exception ignored){}
        }
        return snapshot;
    }
    private static Object field(Object value,String name)throws Exception {
        if(value==null)return null;
        String key=value.getClass().getName()+"#"+name;Field f=FIELDS.get(key);
        if(f==null) {
            Class<?> type=value.getClass();
            while(type!=null){try{f=type.getDeclaredField(name);break;}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}
            if(f==null)throw new NoSuchFieldException(key);f.setAccessible(true);FIELDS.put(key,f);
        }return f.get(value);
    }
    private static Object call(Object value,String name)throws Exception {
        String key=value.getClass().getName()+"#"+name;Method m=METHODS.get(key);
        if(m==null){m=value.getClass().getMethod(name);m.setAccessible(true);METHODS.put(key,m);}return m.invoke(value);
    }
    private static String string(Object x){return x instanceof String?(String)x:"";}
    private static final class PostInfo {
        final String title,body,selftext,subreddit,flair;
        PostInfo(String t,String b,String s,String r,String f){title=t;body=b;selftext=s;subreddit=r;flair=f;}
        boolean matches(KeywordRules k){return communities.matches(subreddit)||k.matches(title)||k.matches(body)||k.matches(selftext)||k.matches(flair);}
    }
    private static PostInfo readLink(Object link)throws Exception {
        return new PostInfo(string(call(link,"getTitle")),string(call(link,"getBody")),string(call(link,"getSelftext")),string(call(link,"getSubreddit")),string(call(link,"getLinkFlairText")));
    }
    public static void rememberLink(Object link) {
        if(link==null)return;
        try {String id=postKey(string(call(link,"getKindWithId")));if(!id.isEmpty()){POSTS.put(id,readLink(link));count("source.link");}}catch(Exception ignored){count("error.link");}
    }
    private static boolean matchesLink(Object link,KeywordRules k)throws Exception {return readLink(link).matches(k);}
    /** Santodan's Listing hook, now only reads explicitly typed Link/Comment content. */
    public static List<?> filterListing(List<?> original) {
        KeywordRules k=rules();if(original==null||original.isEmpty()||k==null||(k.count==0&&communities.count==0))return original;
        ArrayList<Object> kept=new ArrayList<>(original.size());
        try {
            for(Object item:original) {
                if(item!=null&&item.getClass().getName().equals("com.reddit.domain.model.Link")&&matchesLink(item,k))continue;
                if(item!=null&&item.getClass().getName().equals("com.reddit.domain.model.Comment")&&k.matches(string(call(item,"getBody"))))continue;
                kept.add(item);
            }
            return kept.size()==original.size()?original:kept;
        }catch(Exception ignored){return original;}
    }
    public static List<?> filterFeedElements(List<?> original) {
        KeywordRules k=rules();if(original==null||k==null||(k.count==0&&communities.count==0))return original;
        ArrayList<Object> kept=new ArrayList<>(original.size());
        for(Object item:original)if(!hideFeedPost(item))kept.add(item);
        return kept.size()==original.size()?original:kept;
    }
    /** Exact 2026.47 model paths, verified at patch time. No arbitrary graph scan. */
    public static boolean hideFeedPost(Object model) {
        if(model==null)return false;
        String type=model.getClass().getName();count("seen."+type);
        boolean hidden=hideFeedPostInternal(model);
        if(hidden)count("hidden."+type);
        return hidden;
    }
    private static boolean hideFeedPostInternal(Object model) {
        KeywordRules k=rules();if(model==null||k==null||(k.count==0&&communities.count==0))return false;
        try {
            String type=model.getClass().getName();
            if(type.equals("vof0"))return hideFeedPost(field(model,"e"));
            if(isBlockedCommunityItem(model))return true;
            if(type.equals("vmf0")){
                Object post=field(model,"e");
                if(post==null||!post.getClass().getName().equals("hag0"))return false;
                return communities.matches(string(field(post,"s")))||k.matches(string(field(post,"e")))||k.matches(string(field(post,"O")))||k.matches(string(field(post,"P")));
            }
            switch(type){case "qs40":case "y39":case "kk50":case "vmt":case "myl0":case "p9m0":case "m9m0":case "b8m0":break;default:return false;}
            String id=postKey(string(field(model,"e")));
            if(communities.matches(POST_COMMUNITIES.get(id)))return true;
            PostInfo info=POSTS.get(id);
            if(info!=null&&info.matches(k))return true;
            return matchesPostText(model,k,0);
        }catch(Exception ignored){count("error.feed."+model.getClass().getName());return false;}
    }
    /** Search-only lists, filtered before the native typeahead mapper renders them. */
    public static List<?> filterSearchItems(List<?> original) {
        rules();if(original==null||original.isEmpty()||communities.count==0)return original;
        ArrayList<Object> kept=new ArrayList<>(original.size());
        for(Object item:original)if(!isBlockedCommunityItem(item))kept.add(item);
        return kept.size()==original.size()?original:kept;
    }
    private static boolean isBlockedCommunityItem(Object item) {
        if(item==null||communities.count==0)return false;
        try {
            String type=item.getClass().getName();Object behavior;
            switch(type) {
                case "a1h0":behavior=field(field(item,"b"),"a");break;
                case "b1h0":behavior=field(field(item,"k"),"a");break;
                case "mzg0":behavior=field(field(item,"b"),"a");break;
                case "skf0":
                    Object community=field(item,"f");
                    return community!=null&&community.getClass().getName().equals("hbf0")&&communities.matches(string(field(community,"k")));
                default:return false;
            }
            if(behavior==null)return false;
            String kind=behavior.getClass().getName();
            return (kind.equals("w0h0")||kind.equals("w86"))&&communities.matches(string(field(behavior,"b")));
        }catch(Exception ignored){return false;}
    }
    private static boolean matchesPostText(Object model,KeywordRules k,int depth)throws Exception {
        if(model==null||depth>5)return false;String type=model.getClass().getName();
        if(type.equals("com.reddit.domain.model.Link"))return matchesLink(model,k);
        if(type.equals("ghw"))return communities.matchesHeader(string(field(model,"n")))||communities.matchesHeader(string(field(model,"m")));
        if(type.equals("y39"))return communities.matches(string(field(model,"k")))||k.matches(string(field(model,"q")))||matchesPostText(field(model,"t"),k,depth+1);
        // PostFlairsElement -> typed presentation models. Never inspect author flairs or style/ID fields.
        if(type.equals("pw40")){
            Object flairs=field(model,"i");
            if(flairs instanceof Iterable<?>)for(Object flair:(Iterable<?>)flairs){
                if(flair!=null&&flair.getClass().getName().equals("mu40")&&
                    (k.matches(string(field(flair,"a")))||communities.matches(string(field(flair,"d")))))return true;
            }
            return false;
        }
        if(type.equals("g560")||type.equals("w270"))return k.matches(string(field(model,"i")));
        if(type.equals("vdd0"))return k.matches(richText(model));
        String[] paths;
        switch(type) {
            case "kk50":paths=new String[]{"A","l","n","o","p","q","s"};break;
            case "umt":
                Object text=field(model,"a");return text!=null&&matchesPostText(call(text,"getElement"),k,depth+1);
            case "vmt":paths=new String[]{"j","k","n"};break;
            case "myl0":paths=new String[]{"j","k"};break;
            case "p9m0":case "b8m0":paths=new String[]{"i","j"};break;
            case "m9m0":paths=new String[]{"i"};break;
            case "qs40":
                Object children=field(model,"f");
                if(children instanceof Iterable<?>)for(Object child:(Iterable<?>)children)if(matchesPostText(child,k,depth+1))return true;
                return false;
            default:return false;
        }
        for(String path:paths)if(matchesPostText(field(model,path),k,depth+1))return true;
        return false;
    }
    private static String richText(Object model)throws Exception {
        String cached=RICH_TEXT.get(model);if(cached!=null)return cached;
        String raw=string(field(model,"i"));StringBuilder out=new StringBuilder();
        try{collectRichText(new JSONObject(raw),out,0);}catch(JSONException e){return "";}
        String result=out.toString();RICH_TEXT.put(model,result);return result;
    }
    /** Only text nodes in the documented Reddit richtext document; excludes URLs/IDs/metadata. */
    private static void collectRichText(Object node,StringBuilder out,int depth)throws JSONException {
        if(depth>40)return;
        if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length();i++)collectRichText(a.opt(i),out,depth+1);return;}
        if(!(node instanceof JSONObject))return;JSONObject o=(JSONObject)node;
        String element=o.optString("e","");
        if(element.equals("text")||element.equals("i3y")||element.equals("link"))out.append(o.optString("t",""));
        for(String key:new String[]{"document","c","rows","cells"}) {
            Object v=o.opt(key);if(v instanceof JSONArray||v instanceof JSONObject)collectRichText(v,out,depth+1);
        }
        if(element.equals("qys")||element.equals("h")||element.equals("li")||element.equals("br")||element.equals("code"))out.append('\n');
    }
    /** Read-only view projection: keep original tree maps and remove matching branches from IDs. */
    public static Object filterCommentState(Object state) {
        List<?> ids=null;
        try {
            ids=(List<?>)field(state,"c");KeywordRules k=rules();if(ids==null||k==null||k.count==0)return state;
            Map<?,?> comments=(Map<?,?>)field(state,"b");
            Set<String> blocked=new HashSet<>();Map<String,List<String>> children=new HashMap<>();
            for(Map.Entry<?,?> entry:comments.entrySet()) {
                Object comment=entry.getValue();if(comment==null)continue;
                String id=string(entry.getKey());
                String parent=string(call(comment,"getParentKindWithId"));
                List<String> list=children.get(parent);if(list==null){list=new ArrayList<>();children.put(parent,list);}list.add(id);
                if(comment.getClass().getName().equals("com.reddit.domain.model.Comment")&&k.matches(string(call(comment,"getBody"))))blocked.add(id);
            }
            ArrayDeque<String> queue=new ArrayDeque<>(blocked);
            while(!queue.isEmpty()){List<String> list=children.get(queue.remove());if(list!=null)for(String child:list)if(blocked.add(child))queue.add(child);}
            if(blocked.isEmpty())return state;
            // Tree items include load-more rows. Parent chains from treeItems remove those too.
            // The ordinary comments map includes IComment load-more nodes; unknown decorations remain untouched.
            ArrayList<Object> kept=new ArrayList<>(ids.size());
            for(Object id:ids)if(!blocked.contains(string(id)))kept.add(id);
            Map<?,?> grouped=(Map<?,?>)field(state,"g");Map<Object,Object> filteredGroups=new LinkedHashMap<>();
            for(Map.Entry<?,?> entry:grouped.entrySet()) {
                if(blocked.contains(string(entry.getKey())))continue;
                Object value=entry.getValue();
                if(value instanceof List<?>) {
                    ArrayList<Object> list=new ArrayList<>();
                    for(Object id:(List<?>)value)if(!blocked.contains(string(id)))list.add(id);
                    value=list;
                }
                filteredGroups.put(entry.getKey(),value);
            }
            for(Constructor<?> constructor:state.getClass().getDeclaredConstructors()) {
                if(constructor.getParameterTypes().length!=10)continue;
                constructor.setAccessible(true);
                return constructor.newInstance(field(state,"a"),comments,kept,field(state,"d"),field(state,"e"),field(state,"f"),filteredGroups,field(state,"h"),field(state,"i"),field(state,"j"));
            }
            return state;
        }catch(Exception ignored){return state;}
    }
    public static void addSettingsFragment(android.preference.PreferenceFragment fragment) { if(fragment!=null)addSettingsScreen(fragment.getPreferenceScreen()); }
    public static void addSettingsScreen(PreferenceScreen screen) { if(screen!=null)addSettings(screen.getContext(),screen); }
    public static void addSettings(Context c,PreferenceScreen screen) {
        initialize(c);if(screen.findPreference(KEYWORDS)!=null)return;
        PreferenceCategory category=new PreferenceCategory(c);category.setTitle("Filtres de contenu");category.setOrder(-1000);screen.addPreference(category);
        Preference edit=new Preference(c);edit.setKey(KEYWORDS);edit.setTitle("Mots-clés bloqués");edit.setSummary("Texte littéral · une règle par ligne · posts et commentaires");
        edit.setOnPreferenceClickListener(p->{showEditor(c);return true;});category.addPreference(edit);
        Preference imp=new Preference(c);imp.setTitle("Importer une liste .txt");imp.setSummary("UTF-8 · ajouter ou remplacer · traitement local");
        imp.setOnPreferenceClickListener(p->{chooseImport(c);return true;});category.addPreference(imp);
        Preference subs=new Preference(c);subs.setKey(SUBREDDITS);subs.setTitle("Subreddits bloqués");subs.setSummary("Un nom ou r/nom par ligne · masque les posts du subreddit");
        subs.setOnPreferenceClickListener(p->{showSubreddits(c);return true;});category.addPreference(subs);
        Preference diagnostic=new Preference(c);diagnostic.setTitle("Diagnostic local du filtre");diagnostic.setSummary("Version active, règles chargées et vérification d’un subreddit");
        diagnostic.setOnPreferenceClickListener(p->{showDiagnostics(c);return true;});category.addPreference(diagnostic);
    }
    private static void showDiagnostics(Context c){
        EditText name=new EditText(c);name.setSingleLine(true);name.setHint("Nom du subreddit à vérifier");
        AlertDialog dialog=new AlertDialog.Builder(c).setTitle("Diagnostic local").setMessage(diagnostics()).setView(name)
            .setPositiveButton("Vérifier",null).setNeutralButton("Copier",(d,n)->{
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);
                if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic du filtre",diagnostics()));
            }).setNegativeButton("Fermer",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String input=name.getText().toString();dialog.setMessage(diagnostics()+"\nRègle pour ce nom : "+(isSubredditBlocked(input)?"BLOQUÉ":"ABSENTE"));
        }));dialog.show();
    }
    private static void showSubreddits(Context c) {
        initialize(c);EditText input=new EditText(c);input.setSingleLine(false);input.setMinLines(8);input.setMaxLines(16);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setText(prefs(c).getString(SUBREDDITS,""));
        AlertDialog d=new AlertDialog.Builder(c).setTitle("Subreddits bloqués").setMessage("Un nom ou r/nom par ligne. Majuscules ignorées. Actualisez le feed après enregistrement.").setView(input).setPositiveButton("Enregistrer",null).setNegativeButton("Annuler",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=input.getText().toString();d.dismiss();Context app=c.getApplicationContext();WORKER.execute(()->{
            try{SubredditRules next=new SubredditRules(text);if(!prefs(app).edit().putString(SUBREDDITS,next.text).commit())throw new IOException("Échec de sauvegarde");communities=next;toast(app,next.count+" subreddit(s) enregistré(s). Actualisez le feed.");}
            catch(Exception e){toast(app,"Enregistrement impossible : "+e.getMessage());}
        });}));d.show();
    }
    private static void showMenu(Context c) {
        new AlertDialog.Builder(c).setTitle("Filtres de contenu").setItems(new String[]{"Mots-clés bloqués","Importer une liste .txt","Subreddits bloqués","Diagnostic local du filtre"},(d,n)->{if(n==0)showEditor(c);else if(n==1)chooseImport(c);else if(n==2)showSubreddits(c);else showDiagnostics(c);}).show();
    }
    private static Activity activity(Context c) {
        for(int i=0;i<12&&c instanceof ContextWrapper;i++) {if(c instanceof Activity)return (Activity)c;c=((ContextWrapper)c).getBaseContext();}return c instanceof Activity?(Activity)c:null;
    }
    public static void showEditor(Context c) {
        initialize(c);EditText input=new EditText(c);input.setSingleLine(false);input.setMinLines(8);input.setMaxLines(16);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setText(prefs(c).getString(KEYWORDS,""));
        AlertDialog d=new AlertDialog.Builder(c).setTitle("Mots-clés bloqués").setMessage("Une expression par ligne. Majuscules ignorées. Les signes . + ( ) [ ] sont du texte.\nAprès modification, actualisez le feed ou rouvrez le fil.").setView(input).setPositiveButton("Enregistrer",null).setNeutralButton("Importer .txt",(x,y)->chooseImport(c)).setNegativeButton("Annuler",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=input.getText().toString();d.dismiss();save(c,text,false);}));d.show();
    }
    private static void save(Context c,String text,boolean append) {
        Context app=c.getApplicationContext();WORKER.execute(()->{
            try {
                String combined=append?prefs(app).getString(KEYWORDS,"")+"\n"+text:text;
                KeywordRules next=new KeywordRules(combined);
                if(!prefs(app).edit().putString(KEYWORDS,next.text).commit())throw new IOException("Échec de sauvegarde");snapshot=next;
                toast(app,next.count+" règle(s) enregistrée(s). Actualisez le feed ou rouvrez le fil.");
            }catch(Exception e){toast(app,"Import/enregistrement impossible : "+e.getMessage());}
        });
    }
    private static void toast(Context c,String message){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->Toast.makeText(c,message,Toast.LENGTH_LONG).show());}
    private static void chooseImport(Context c) {
        Activity a=activity(c);if(a==null){toast(c,"Ouvrez les réglages depuis Reddit.");return;}
        new AlertDialog.Builder(a).setTitle("Importer une liste UTF-8").setItems(new String[]{"Ajouter à la liste existante","Remplacer la liste"},(d,which)->{
            ImportFragment f=new ImportFragment();Bundle b=new Bundle();b.putBoolean("append",which==0);f.setArguments(b);
            a.getFragmentManager().beginTransaction().add(f,"local_keyword_import").commit();
        }).show();
    }
    public static final class ImportFragment extends Fragment {
        public ImportFragment(){}
        @Override public void onCreate(Bundle saved){super.onCreate(saved);if(saved==null){
            Intent i=new Intent("android.intent.action.OPEN_DOCUMENT");i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");i.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
            try{startActivityForResult(i,7419);}catch(ActivityNotFoundException e){toast(getActivity(),"Aucun sélecteur de fichiers disponible.");remove();}
        }}
        @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=7419)return;
            Activity a=getActivity();boolean append=getArguments().getBoolean("append");
            if(a!=null&&result==Activity.RESULT_OK&&data!=null&&data.getData()!=null){Context c=a.getApplicationContext();Uri uri=data.getData();
                WORKER.execute(()->{try(InputStream stream=c.getContentResolver().openInputStream(uri)){
                    if(stream==null)throw new IOException("Fichier inaccessible");
                    java.nio.charset.CharsetDecoder decoder=java.nio.charset.Charset.forName("UTF-8").newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT);
                    Reader reader=new InputStreamReader(stream,decoder);StringBuilder text=new StringBuilder();char[] buf=new char[8192];int n;
                    while((n=reader.read(buf))!=-1){text.append(buf,0,n);if(text.length()>2_000_000)throw new IOException("Liste trop volumineuse");}
                    save(c,text.toString(),append);
                }catch(Exception e){toast(c,"Import impossible : "+e.getMessage());}});
            }remove();
        }
        private void remove(){if(getFragmentManager()!=null)getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();}
    }
    /** Standalone fallback only when official Morphe settings are not included. */
    public static boolean openSettings(Activity a,Uri uri) {
        if(a==null||uri==null)return false;
        int id=a.getResources().getIdentifier("privacy_policy_uri","string",a.getPackageName());
        if(id!=0&&a.getString(id).equals(uri.toString())){showMenu(a);return true;}
        String url=uri.toString();
        if(url.equals("https://www.redditinc.com/policies/privacy-policy")||url.equals("https://www.reddit.com/policies/privacy-policy")||url.equals("https://www.reddit.com/help/privacypolicy")){showMenu(a);return true;}
        return false;
    }
}
