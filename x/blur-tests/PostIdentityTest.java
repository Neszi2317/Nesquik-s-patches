import app.morphe.extension.newx.mediablur.PostIdentity;
public class PostIdentityTest {
 public static class Identifier {public long a;Identifier(long value){a=value;}}
 public static class Route {private final long post;Route(long value){post=value;}public long s(){return post;}}
 public static class Controller {public Route b;public Identifier d;Controller(long post,long account){b=new Route(post);d=new Identifier(account);}}
 public static class Row {public Identifier c,e;Row(long post,long account){c=new Identifier(post);e=new Identifier(account);}}
 static int count;static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
 public static void main(String[] args){Controller c=new Controller(100,42);Row root=new Row(100,42),reply=new Row(101,42),collision=new Row(42,42);
 long rootId=PostIdentity.rootPostId(c);check(rootId==100,"Root comes from navigation, never current account");
 check(PostIdentity.rowPostId(reply)==101,"Reply ID comes from post, never current account");
 check(!PostIdentity.mediaAllowed(null,PostIdentity.rowPostId(root),rootId),"Main post excluded");
 check(PostIdentity.mediaAllowed(null,PostIdentity.rowPostId(reply),rootId),"Reply from same account eligible (0.1.0 regression)");
 check(PostIdentity.mediaAllowed(null,PostIdentity.rowPostId(collision),rootId),"Account/post numeric collision harmless");
 check(!PostIdentity.mediaAllowed(false,200,rootId),"Quote inside main post stays excluded");
 check(PostIdentity.mediaAllowed(true,100,rootId),"Root quoted inside reply remains eligible");
 check(PostIdentity.mediaAllowed(true,200,rootId),"Nested reply quote stays eligible");
 System.out.println("PASS "+count+" production identity/scope regression assertions");}
}
