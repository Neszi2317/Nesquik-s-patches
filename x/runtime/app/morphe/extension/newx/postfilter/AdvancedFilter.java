package app.morphe.extension.newx.postfilter;
import java.util.*;
/** Pre-render filtering, reusing Piko's immutable-list/module-copy bridges. */
public final class AdvancedFilter {
    private static final AdvancedGraph GRAPH=new AdvancedGraph();private static AdvancedConfig last;
    private static long passes,removed,failures;private static String lastError="";
    public static synchronized String diagnostics(){return "Passes : "+passes+" · supprimés : "+removed+" · cache : "+GRAPH.size()+" · erreurs : "+failures+(lastError.isEmpty()?"":"\n"+lastError);}
    public static synchronized Object filter(Object source){
        if(!(source instanceof Iterable<?>))return source;
        try {AdvancedConfig c=AdvancedConfig.get();if(c!=last){GRAPH.clear();last=c;}if(!c.enabled||c.keywords.isEmpty())return source;
            passes++;IdentityHashMap<Object,Boolean> seen=new IdentityHashMap<>();for(Object x:(Iterable<?>)source)observeItem(x,c,seen,0);
            GRAPH.evaluate(c.strict);ArrayList<Object> out=new ArrayList<>();boolean changed=false;
            for(Object x:(Iterable<?>)source){Object n=prune(x,c,0);if(n!=null)out.add(n);if(n!=x)changed=true;}
            return changed?AdvancedModels.immutable(out):source;
        }catch(RuntimeException e){failures++;lastError=e.getClass().getSimpleName()+": "+e.getMessage();return source;}
    }
    public static synchronized Object filterSuggestions(Object source){if(!(source instanceof List<?>))return source;
        try{AdvancedConfig c=AdvancedConfig.get();if(!c.enabled||c.keywords.isEmpty()||!c.search&&!c.accounts)return source;
            ArrayList<Object> out=new ArrayList<>();boolean changed=false;for(Object o:(List<?>)source){boolean hide=c.search&&(c.matcher.matches(AdvancedModels.suggestionText(o))||c.matcher.matches(AdvancedModels.suggestionDisplay(o)));
                hide|=c.accounts&&userBlocked(AdvancedModels.suggestedUser(o),c);if(hide){removed++;changed=true;}else out.add(o);}
            return changed?Collections.unmodifiableList(out):source;
        }catch(RuntimeException e){failures++;lastError=e.toString();return source;}}
    private static void observeItem(Object o,AdvancedConfig c,IdentityHashMap<Object,Boolean> seen,int depth){
        if(o==null||depth>64||seen.put(o,Boolean.TRUE)!=null)return;
        if(AdvancedModels.isWrapper(o)){observeItem(AdvancedModels.unwrap(o),c,seen,depth+1);return;}
        if(AdvancedModels.isModule(o)){List<?> list=AdvancedModels.children(o);if(list!=null)for(Object x:list)observeItem(x,c,seen,depth+1);return;}
        if(c.posts&&AdvancedModels.isPost(o))observePost(AdvancedModels.result(o),c,seen,depth+1);
        if(c.notifications){List<?> targets=AdvancedModels.notificationPosts(o);if(targets!=null)for(Object t:targets)if(c.matcher.matches(AdvancedModels.notificationPostText(t))){
            long key=AdvancedModels.notificationPostId(t),original=AdvancedModels.notificationOriginalId(t);if(key>0)GRAPH.observe(Long.toString(key),true,Collections.emptyList());if(original>0)GRAPH.observe(Long.toString(original),true,Collections.emptyList());}}
    }
    private static String id(Object x){if(x==null)return null;if(x instanceof Number){long n=((Number)x).longValue();return n>0?Long.toString(n):null;}if(x instanceof String)return (String)x;return AdvancedModels.identifier(x);}
    private static String observePost(Object context,AdvancedConfig c,IdentityHashMap<Object,Boolean> seen,int depth){
        if(context==null||depth>64)return null;
        if(seen.put(context,Boolean.TRUE)!=null)return id(AdvancedModels.postId(AdvancedModels.canonical(context)));
        Object canonical=AdvancedModels.canonical(context);if(canonical==null)return null;
        String self=id(AdvancedModels.postId(canonical));Object author=AdvancedModels.author(canonical);
        boolean direct=c.matcher.matches(AdvancedModels.text(canonical))||c.matcher.matches(AdvancedModels.noteText(canonical));
        if(c.authors)direct|=c.matcher.matches(AdvancedModels.username(author))||c.matcher.matches(AdvancedModels.name(author));
        if(c.bios)direct|=c.matcher.matches(AdvancedModels.bio(author));
        if(c.links)direct|=c.matcher.matches(AdvancedModels.linkText(canonical));
        ArrayList<String> parents=new ArrayList<>();
        if(c.replies)parents.add(id(AdvancedModels.parentId(canonical)));
        if(c.quotes){parents.add(id(AdvancedModels.quoteId(canonical)));Object quoted=AdvancedModels.quote(context);if(c.strict&&quoted!=null&&AdvancedModels.canonical(quoted)==null)direct=true;String q=observePost(quoted,c,seen,depth+1);if(q!=null)parents.add(q);}
        Object repost=AdvancedModels.repost(context);if(repost!=null){String r=observePost(repost,c,seen,depth+1);if(r!=null)parents.add(r);else if(c.strict)direct=true;}
        GRAPH.observe(self,direct,parents);return self;
    }
    private static boolean userBlocked(Object user,AdvancedConfig c){return user!=null&&(c.matcher.matches(AdvancedModels.username(user))||c.matcher.matches(AdvancedModels.name(user))||(c.bios&&c.matcher.matches(AdvancedModels.bio(user))));}
    private static boolean notificationBlocked(Object o,AdvancedConfig c){
        if(c.matcher.matches(AdvancedModels.notificationMessage(o))||c.matcher.matches(AdvancedModels.notificationContext(o)))return true;
        List<?> posts=AdvancedModels.notificationPosts(o);if(posts!=null)for(Object p:posts)if(c.matcher.matches(AdvancedModels.notificationPostText(p))||GRAPH.blocked(id(AdvancedModels.notificationPostId(p)))||GRAPH.blocked(id(AdvancedModels.notificationOriginalId(p))))return true;
        List<?> users=AdvancedModels.notificationUsers(o);if(c.authors&&users!=null)for(Object u:users)if(c.matcher.matches(AdvancedModels.notificationUsername(u))||c.matcher.matches(AdvancedModels.notificationName(u)))return true;
        return false;
    }
    private static Object prune(Object o,AdvancedConfig c,int depth){
        if(o==null)return null;if(depth>64)return c.strict?null:o;
        if(AdvancedModels.isWrapper(o)){Object item=AdvancedModels.unwrap(o),n=prune(item,c,depth+1);if(n==null){removed++;return null;}return n==item?o:AdvancedModels.copyWrapper(o,n,AdvancedModels.dispensable(o));}
        if(c.posts&&AdvancedModels.isPost(o)){Object canonical=AdvancedModels.canonical(AdvancedModels.result(o));
            if((canonical==null&&c.strict)||GRAPH.blocked(id(AdvancedModels.postId(canonical)))){removed++;return null;}}
        if(c.trends&&(c.matcher.matches(AdvancedModels.trendText(o))||c.matcher.matches(AdvancedModels.trendDescription(o))||c.matcher.matches(AdvancedModels.trendDomain(o))||c.matcher.matches(AdvancedModels.trendMetadata(o)))){removed++;return null;}
        if(c.trends){List<?> groups=AdvancedModels.trendGroups(o);if(groups!=null)for(Object g:groups)if(c.matcher.matches(AdvancedModels.groupName(g))){removed++;return null;}}
        if(c.notifications&&notificationBlocked(o,c)){removed++;return null;}
        if(c.accounts&&userBlocked(AdvancedModels.suggestedUser(o),c)){removed++;return null;}
        if(AdvancedModels.isModule(o)){List<?> list=AdvancedModels.children(o);if(list==null)return o;ArrayList<Object> kept=new ArrayList<>();boolean changed=false;
            for(Object x:list){Object n=prune(x,c,depth+1);if(n!=null)kept.add(n);if(n!=x)changed=true;}
            if(!changed)return o;if(kept.isEmpty()){removed++;return null;}Object display=AdvancedModels.display(o);
            if(AdvancedModels.vertical(display)){List<?> ids=AdvancedModels.verticalIds(display);if(ids!=null){ArrayList<Object> keptIds=new ArrayList<>();
                HashSet<String> present=new HashSet<>();for(Object k:kept){Object it=AdvancedModels.isWrapper(k)?AdvancedModels.unwrap(k):k;
                    if(AdvancedModels.isPost(it))present.add(id(AdvancedModels.postId(AdvancedModels.canonical(AdvancedModels.result(it)))));}
                for(Object i:ids)if(present.contains(id(i)))keptIds.add(i);display=AdvancedModels.copyVertical(display,keptIds);}}
            return AdvancedModels.copyModule(o,kept,AdvancedModels.header(o),AdvancedModels.footer(o),display,AdvancedModels.sort(o),AdvancedModels.entry(o),AdvancedModels.event(o));}
        return o;
    }
}
