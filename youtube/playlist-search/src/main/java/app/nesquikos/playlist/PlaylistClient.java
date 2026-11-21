package app.nesquikos.playlist;
import android.os.Build;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
/** Authenticated browse based on Morphe PlaylistRoutes, GPL-3.0 + NOTICE. */
public final class PlaylistClient {
 static final String VERSION="20.26.46";
 volatile boolean cancelled;volatile HttpURLConnection active;
 public void cancel(){cancelled=true;HttpURLConnection c=active;if(c!=null)c.disconnect();}
 public boolean cancelled(){return cancelled;}
 public JSONObject fetch(String id,String token,PlaylistSession.Snapshot s)throws Exception{
  if(cancelled||!PlaylistSession.valid(s))throw new IOException("Session modifiée.");
  JSONObject c=new JSONObject().put("clientName","ANDROID").put("clientVersion",VERSION).put("deviceMake",Build.MANUFACTURER).put("deviceModel",Build.MODEL).put("osName","Android").put("osVersion",Build.VERSION.RELEASE).put("androidSdkVersion",Build.VERSION.SDK_INT).put("hl",Locale.getDefault().getLanguage()).put("gl",Locale.getDefault().getCountry());
  JSONObject body=new JSONObject().put("context",new JSONObject().put("client",c));if(token.isEmpty())body.put("browseId","VL"+id);else body.put("continuation",token);
  byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
  HttpURLConnection conn=(HttpURLConnection)new URL("https://youtubei.googleapis.com/youtubei/v1/browse?prettyPrint=false").openConnection();active=conn;
  try{
   conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setUseCaches(false);conn.setRequestMethod("POST");conn.setDoOutput(true);
   conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");conn.setRequestProperty("Accept","application/json");conn.setRequestProperty("User-Agent","com.google.android.youtube/"+VERSION+" (Linux; U; Android "+Build.VERSION.RELEASE+"; "+Locale.getDefault()+"; "+Build.MODEL+" Build/"+Build.ID+")");conn.setRequestProperty("X-YouTube-Client-Name","3");conn.setRequestProperty("X-YouTube-Client-Version",VERSION);conn.setRequestProperty("X-GOOG-API-FORMAT-VERSION","2");
   for(Map.Entry<String,String> h:s.headers.entrySet())conn.setRequestProperty(h.getKey(),h.getValue());if(cancelled||!PlaylistSession.valid(s))throw new IOException("Session modifiée.");
   conn.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=conn.getOutputStream()){out.write(bytes);}int code=conn.getResponseCode();if(code!=200)throw new IOException("YouTube HTTP "+code+". Recharge la playlist puis réessaie.");
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=conn.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(cancelled||!PlaylistSession.valid(s))throw new IOException("Compte modifié ou recherche annulée.");if(out.size()+n>16*1024*1024)throw new IOException("Page trop volumineuse.");out.write(buf,0,n);}}
   return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
  }finally{active=null;conn.disconnect();}
 }
}
