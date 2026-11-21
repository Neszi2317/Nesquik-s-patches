package app.morphe.extension.newx.mediablur;
import java.util.HashSet;
/** No View position or object identity is used as the media identity. */
public final class SessionState {
 private boolean active; private final HashSet<String> revealed=new HashSet<>();
 public boolean active(){return active;}
 public void toggle(){active=!active;}
 public boolean hidden(String key){return active&&!revealed.contains(key);}
 public void reveal(String key){if(key!=null)revealed.add(key);}
 public int revealedCount(){return revealed.size();}
 public void clear(){active=false;revealed.clear();}
}
