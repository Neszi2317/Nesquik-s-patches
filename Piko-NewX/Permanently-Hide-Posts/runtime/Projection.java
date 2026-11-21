package fr.nesquik.hidden;
import com.x.models.*;
import com.x.models.timelines.items.k1;
import static app.morphe.extension.newx.timeline.NewXTimelineFilter.*;
import java.util.*;

/** Projects immutable URT lists at the UI boundary. X retains its unmodified source list. */
public final class Projection {
    private static Set<String> previousIds;
    private static java.lang.ref.WeakReference<Object> previousInput=new java.lang.ref.WeakReference<>(null);
    private static java.lang.ref.WeakReference<Object> previousResult=new java.lang.ref.WeakReference<>(null);
    public static synchronized Object render(Object input){
        Store.observe();
        Set<String> ids=Store.ids();
        if(!Store.enabled() || ids.isEmpty() || !(input instanceof List<?>)) return input;
        Object cached=previousResult.get();
        if(input==previousInput.get() && ids==previousIds && cached!=null)return cached;
        List<?> rows=(List<?>)input;
        List<Object> kept=filter(rows,ids,0,null);
        Object result=kept==null?input:immutableList(kept);
        previousIds=ids;
        previousInput=new java.lang.ref.WeakReference<>(input);
        previousResult=new java.lang.ref.WeakReference<>(result);
        return result;
    }
    // Allocate a replacement only after the first changed row.
    private static List<Object> filter(List<?> rows,Set<String> ids,int depth,Set<Object> removed){
        List<Object> kept=null;
        for(int n=0;n<rows.size();n++){
            Object row=rows.get(n);
            Object next=visit(row,ids,depth,removed);
            if(next!=row && kept==null)kept=new ArrayList<>(rows.subList(0,n));
            if(kept!=null && (next!=null || row==null))kept.add(next);
        }
        return kept;
    }
    private static Object visit(Object row,Set<String> ids,int depth,Set<Object> removed){
        if(depth>32 || row==null)return row;
        if(row instanceof k1){
            k1 post=(k1)row;
            if(ids.contains(String.valueOf(post.getId())) || hidden(post.a,ids,0)){
                if(removed!=null)removed.add(post.getId());
                return null;
            }
        }else if(isTimelineModuleItem(row)){
            Object old=getModuleItem(row), next=visit(old,ids,depth+1,removed);
            if(old!=next)return next==null?null:copyModuleItem(row,next,isModuleItemDispensable(row));
        }else if(isTimelineModule(row)){
            List<?> children=getModuleInnerContent(row);
            if(children==null)return row;
            Set<Object> gone=new HashSet<>();
            List<Object> kept=filter(children,ids,depth+1,gone);
            if(kept==null)return row;
            if(removed!=null)removed.addAll(gone);
            if(kept.isEmpty())return null;
            Object display=getModuleDisplayType(row);
            if(display!=null && isVerticalConversation(display)){
                List<?> old=getVerticalConversationPostIds(display);
                if(old!=null){
                    List<Object> links=new ArrayList<>(old);
                    if(links.removeAll(gone))display=copyVerticalConversation(display,links);
                }
            }
            return copyModule(row,kept,getModuleHeader(row),getModuleFooter(row),display,
                getModuleSortIndex(row),getModuleEntryId(row),getModuleClientEventInfo(row));
        }
        return row;
    }
    private static boolean hidden(p6 result,Set<String> ids,int depth){
        if(!(result instanceof s1) || depth>16)return false;
        s1 item=(s1)result;
        q8 repost=item.d;
        return matches(item.b,ids) || hidden(item.c,ids,depth+1) ||
            (repost!=null && (matches(repost.a,ids) || hidden(repost.b,ids,depth+1)));
    }
    private static boolean matches(v0 post,Set<String> ids){
        return post!=null && (ids.contains(String.valueOf(post.a)) || ids.contains(String.valueOf(post.B)));
    }
}
