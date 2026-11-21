package app.morphe.extension.newx.mediablur;
import static app.morphe.extension.newx.mediablur.Reflector.*;
/** Post IDs are h6/navigation.fm.s(); gh is a USER ID and must never select posts. */
public final class PostIdentity {
 public static long rootPostId(Object controller){return ((Number)call(field(controller,"b"),"s")).longValue();}
 public static long rowPostId(Object row){return ((Number)field(field(row,"c"),"a")).longValue();}
 public static boolean mediaAllowed(Boolean parent,long postId,long rootPostId){return parent!=null?parent.booleanValue():postId!=rootPostId;}
}
