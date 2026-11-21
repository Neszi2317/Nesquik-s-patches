package app.nesquikos.playlist;
import java.net.URI;
import java.util.*;
/** Session-only identity snapshot. No credentials logged, persisted or sent outside YouTube. */
public final class PlaylistSession {
 static Map<String,String> current=Collections.emptyMap();static String pageId="";static boolean incognito;static long generation;
 public static final class Snapshot {public final long generation;public final Map<String,String> headers;Snapshot(long g,Map<String,String> h){generation=g;headers=Collections.unmodifiableMap(new HashMap<String,String>(h));}}
 public static synchronized void page(String p){if(p==null)p="";if(!pageId.equals(p)){pageId=p;generation++;current=Collections.emptyMap();}}
 public static synchronized void incognito(boolean v){if(incognito!=v){incognito=v;generation++;current=Collections.emptyMap();}}
 public static synchronized Snapshot snapshot(){return new Snapshot(generation,current);}
 public static synchronized boolean valid(Snapshot s){return s.generation==generation;}
 public static synchronized void capture(String url,Map<String,String> headers){try{
  URI u=new URI(url);if(!"https".equals(u.getScheme())||!"youtubei.googleapis.com".equals(u.getHost())||!u.getPath().startsWith("/youtubei/v1/")||headers==null)return;
  Map<String,String> n=new HashMap<String,String>();for(Map.Entry<String,String> e:headers.entrySet()){if(e.getValue()==null||e.getValue().isEmpty())continue;for(String k:new String[]{"Authorization","X-Goog-Visitor-Id","X-Goog-PageId","X-Goog-AuthUser"})if(k.equalsIgnoreCase(e.getKey()))n.put(k,e.getValue());}
  if(!n.containsKey("Authorization")&&!u.getPath().endsWith("/browse"))return;
  if(!pageId.isEmpty())n.put("X-Goog-PageId",pageId);if(incognito)n.remove("Authorization");
  for(String k:new String[]{"Authorization","X-Goog-PageId","X-Goog-AuthUser"})if(!Objects.equals(n.get(k),current.get(k))){generation++;break;}current=n;
 }catch(Exception ignored){}}
}
