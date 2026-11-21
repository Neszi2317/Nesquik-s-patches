package app.morphe.extension.newx.anonymous;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
/** Bounded cache. Stable keyed aliases, no account inventory persisted. */
public final class AliasEngine {
 public static final int CACHE_LIMIT=2048;
 private final Mac mac;
 private final LinkedHashMap<String,Alias> cache=new LinkedHashMap<String,Alias>(256,.75f,true){
  protected boolean removeEldestEntry(Map.Entry<String,Alias> e){return size()>CACHE_LIMIT;}
 };
 public static final class Alias {
  public final String name,handle;
  Alias(String name,String handle){this.name=name;this.handle=handle;}
 }
 public AliasEngine(byte[] key){
  if(key==null||key.length!=32)throw new IllegalArgumentException("Expected 256-bit key");
  try{mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.clone(),"HmacSHA256"));}
  catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
 }
 public synchronized Alias get(String id){
  if(!validId(id))return null;
  Alias found=cache.get(id);if(found!=null)return found;
  byte[] h=mac.doFinal(("piko-anonymous-v1:"+id).getBytes(StandardCharsets.UTF_8));
  int animal=(int)(u32(h,0)%NameBank.ANIMALS.length);
  int adjective=(int)(u32(h,4)%NameBank.MALE.length);
  String label=NameBank.ANIMALS[animal]+" "+(NameBank.FEMININE[animal]?NameBank.FEMALE:NameBank.MALE)[adjective];
  // Always include a stable suffix: adding a later account never renames an earlier one.
  found=new Alias(label+" · "+hex(h,8,4),"anonyme_"+hex(h,8,12));cache.put(id,found);return found;
 }
 static boolean validId(String id){if(id==null||id.length()==0||id.length()>20||id.charAt(0)=='0')return false;for(int i=0;i<id.length();i++)if(id.charAt(i)<'0'||id.charAt(i)>'9')return false;return true;}
 private static long u32(byte[] b,int p){return ((b[p]&255L)<<24)|((b[p+1]&255L)<<16)|((b[p+2]&255L)<<8)|(b[p+3]&255L);}
 static String hex(byte[] b,int p,int n){char[] alphabet="0123456789abcdef".toCharArray(),out=new char[n*2];for(int i=0;i<n;i++){out[2*i]=alphabet[(b[p+i]&255)>>>4];out[2*i+1]=alphabet[b[p+i]&15];}return new String(out);}
 public synchronized int cacheSize(){return cache.size();}
}
