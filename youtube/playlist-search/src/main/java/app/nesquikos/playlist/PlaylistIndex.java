package app.nesquikos.playlist;
import java.text.Normalizer;
import java.util.*;
import org.json.*;
/** Reads only playlist membership containers; never a generic search/recommendation list. */
public final class PlaylistIndex {
 public static final class Video {
  public final String id,title,channel; final String tk,ck;
  Video(String i,String t,String c){id=i;title=t;channel=c;tk=normalize(t);ck=normalize(c);}
 }
 public static final class Page {public final List<Video> videos=new ArrayList<Video>();public String next="",target="";public int unavailable;boolean recognized;}
 public static String normalize(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim();}
 public static List<Video> filter(Collection<Video> source,String q){String k=normalize(q);List<Video> r=new ArrayList<Video>();for(Video v:source)if(v.tk.contains(k)||v.ck.contains(k))r.add(v);return r;}
 static String text(JSONObject o){if(o==null)return "";if(o.has("simpleText"))return o.optString("simpleText");StringBuilder b=new StringBuilder();JSONArray a=o.optJSONArray("runs");if(a!=null)for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(r!=null)b.append(r.optString("text"));}return b.toString();}
 public static Page parse(JSONObject response,String id,boolean continuation,String target)throws JSONException{
  if(response.has("error"))throw new JSONException("YouTube a refusé la requête.");Page p=new Page();
  if(!continuation){
   JSONObject c=response.optJSONObject("contents");if(c!=null){JSONObject columns=c.optJSONObject("singleColumnBrowseResultsRenderer");if(columns==null)columns=c.optJSONObject("twoColumnBrowseResultsRenderer");
    if(columns==null)walk(c,id,p,0);else{JSONArray tabs=columns.optJSONArray("tabs");if(tabs!=null){boolean selected=false;
     for(int i=0;i<tabs.length();i++){JSONObject t=tabs.optJSONObject(i),r=t==null?null:t.optJSONObject("tabRenderer");if(r!=null&&r.optBoolean("selected"))selected=true;}
     for(int i=0;i<tabs.length();i++){JSONObject t=tabs.optJSONObject(i),r=t==null?null:t.optJSONObject("tabRenderer");if(r!=null&&(!selected||r.optBoolean("selected")))walk(r.optJSONObject("content"),id,p,0);}
    }}
   }
  }else{
   JSONObject cc=response.optJSONObject("continuationContents"),l=cc==null?null:cc.optJSONObject("playlistVideoListContinuation");if(l!=null)readList(l,id,p);
   if(!p.recognized)for(String key:new String[]{"onResponseReceivedActions","onResponseReceivedEndpoints","onResponseReceivedCommands"}){JSONArray a=response.optJSONArray(key);if(a==null)continue;
    for(int i=0;i<a.length();i++){JSONObject act=a.optJSONObject(i),append=act==null?null:act.optJSONObject("appendContinuationItemsAction");if(append==null)continue;
     String tid=append.optString("targetId");if(!target.isEmpty()&&!tid.isEmpty()&&!tid.equals(target))continue;
     JSONArray items=append.optJSONArray("continuationItems");if(items==null)continue;boolean known=items.length()==0;
     for(int j=0;j<items.length();j++){JSONObject v=items.optJSONObject(j);if(v!=null&&v.has("playlistVideoRenderer"))known=true;}
     if(!known)continue;if(p.recognized)throw new JSONException("Plusieurs continuations possibles.");p.recognized=true;p.target=tid;readItems(items,p);
    }
   }
  }
  if(!p.recognized)throw new JSONException("Format non reconnu ou playlist inaccessible. Aucun résultat global utilisé.");return p;
 }
 static void walk(JSONObject node,String id,Page p,int depth)throws JSONException{if(node==null||depth>8)return;JSONObject l=node.optJSONObject("playlistVideoListRenderer");if(l!=null){readList(l,id,p);return;}
  for(String k:new String[]{"sectionListRenderer","itemSectionRenderer"}){JSONObject w=node.optJSONObject(k);JSONArray a=w==null?null:w.optJSONArray("contents");if(a!=null)for(int i=0;i<a.length();i++)walk(a.optJSONObject(i),id,p,depth+1);}
 }
 static void readList(JSONObject l,String id,Page p)throws JSONException{
  String supplied=l.optString("playlistId");if(!supplied.isEmpty()&&!supplied.equals(id))throw new JSONException("Identifiant de playlist différent.");if(p.recognized)throw new JSONException("Plusieurs listes détectées.");
  JSONArray a=l.optJSONArray("contents");if(a==null)throw new JSONException("Vidéos absentes de la réponse.");p.recognized=true;p.target=l.optString("targetId");readItems(a,p);
  JSONArray cs=l.optJSONArray("continuations");if(cs!=null)for(int i=0;i<cs.length();i++){JSONObject c=cs.optJSONObject(i),n=c==null?null:c.optJSONObject("nextContinuationData");if(n!=null)next(p,n.optString("continuation"));}
 }
 static void next(Page p,String token)throws JSONException{if(token.isEmpty())return;if(!p.next.isEmpty()&&!p.next.equals(token))throw new JSONException("Continuations ambiguës.");p.next=token;}
 static void readItems(JSONArray a,Page p)throws JSONException{
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)throw new JSONException("Élément inconnu.");JSONObject v=o.optJSONObject("playlistVideoRenderer");
   if(v!=null){String id=v.optString("videoId"),title=text(v.optJSONObject("title")),channel=text(v.optJSONObject("shortBylineText"));if(channel.isEmpty())channel=text(v.optJSONObject("longBylineText"));if(!id.matches("[A-Za-z0-9_-]{11}")){p.unavailable++;continue;}p.videos.add(new Video(id,title,channel));continue;}
   JSONObject c=o.optJSONObject("continuationItemRenderer"),ep=c==null?null:c.optJSONObject("continuationEndpoint"),cmd=ep==null?null:ep.optJSONObject("continuationCommand");if(cmd!=null&&!cmd.optString("token").isEmpty()){next(p,cmd.optString("token"));continue;}
   throw new JSONException("Format de vidéo non reconnu — résultats partiels.");
  }
 }
}
