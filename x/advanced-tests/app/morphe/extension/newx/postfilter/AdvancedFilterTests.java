package app.morphe.extension.newx.postfilter;
import java.util.*;import org.json.*;import app.morphe.extension.newx.postfilter.AdvancedModels.Module;import static app.morphe.extension.newx.postfilter.AdvancedModels.*;
/** Guards concrete bypasses: linked parents, Unicode obfuscation, stale cache and empty modules. */
public class AdvancedFilterTests {
 static int checks;static void check(boolean b,String msg){checks++;if(!b)throw new AssertionError(msg);System.out.println("PASS "+msg);}
 static void config(String word,boolean strict)throws Exception {JSONObject j=new JSONObject();j.put("keywords",new JSONArray(Arrays.asList(word)));j.put("strict",strict);java.lang.reflect.Field f=AdvancedConfig.class.getDeclaredField("current");f.setAccessible(true);f.set(null,new AdvancedConfig(j));}
 static List<?> filter(Object...v){return (List<?>)AdvancedFilter.filter(Arrays.asList(v));}
 static Post p(String i,String text){Post p=new Post(i);p.result.canonical.text=text;return p;}
 public static void main(String[]args)throws Exception{
  config("interdit",false);check(filter(p("1","Ceci est INTERDIT")).isEmpty(),"post text case-insensitive");
  Post author=p("2","safe");author.result.canonical.user.username="xxINTERDITxx";check(filter(author).isEmpty(),"username");
  author=p("3","safe");author.result.canonical.user.name="Interdit";check(filter(author).isEmpty(),"display name");
  author=p("4","safe");author.result.canonical.user.bio="profil interdit";check(filter(author).isEmpty(),"biography");
  Post q=p("5","safe");q.result.canonical.quote="6";q.result.quote=p("6","interdit").result;check(filter(q).isEmpty(),"quoted post propagates to container");
  Post a=p("7","safe"),b=p("8","interdit"),c=p("9","safe");a.result.canonical.parent="8";c.result.canonical.parent="7";
  check(filter(c,a,b).isEmpty(),"out-of-order three-level reply chain");
  config("interdit",false);a=p("10","safe");a.result.canonical.parent="11";check(filter(a).size()==1,"missing parent permissive");filter(p("11","interdit"));check(filter(a).isEmpty(),"parent arrives in later page");
  config("interdit",true);check(filter(a).isEmpty(),"missing parent strict");
  config("interdit",false);a=p("12","safe");b=p("13","safe");a.result.canonical.parent="13";b.result.canonical.parent="12";check(filter(a,b).size()==2,"safe cycle terminates");b.result.canonical.text="interdit";check(filter(a,b).isEmpty(),"cycle with forbidden node propagates");
  check(filter(new Trend("#Interdit"),new Trend("safe")).size()==1,"trend removal");User u=new User();u.name="interdit";check(filter(new Account(u)).isEmpty(),"account suggestion");
  Module empty=new Module(Arrays.asList(new Wrapper(p("14","interdit")),new Wrapper(p("15","interdit"))));check(filter(empty).isEmpty(),"consecutive blocked items remove empty module");
  Module mixed=new Module(Arrays.asList(new Wrapper(p("16","interdit")),new Wrapper(p("17","safe"))));mixed.display=Arrays.asList("16","17");List<?> out=filter(mixed);check(out.size()==1&&((Module)out.get(0)).children.size()==1&&((List<?>)((Module)out.get(0)).display).equals(Arrays.asList("17")),"module and vertical IDs compacted");
  Object cursor=new Object();List<Object> unchanged=Arrays.asList(p("18","safe"),cursor);check(AdvancedFilter.filter(unchanged)==unchanged,"unchanged list identity and pagination cursor retained");
  check(filter(p("19","interdit"),cursor).get(0)==cursor,"cursor survives removals");
  check(((List<?>)AdvancedFilter.filterSuggestions(Arrays.asList(new Suggestion("interdit","safe"),new Suggestion("safe","Interdit"),new Suggestion("safe","safe"),new Account(u)))).size()==1,"search suggestions and search accounts removed");
  Post longPost=p("20","safe");longPost.result.canonical.note="interdit";check(filter(longPost).isEmpty(),"long note text");
  Post linked=p("21","safe");linked.result.canonical.link="site interdit";check(filter(linked).isEmpty(),"link preview text");
  Notification notification=new Notification();notification.posts.add(new NPost(22,"interdit"));Post reply=p("23","safe");reply.result.canonical.parent="22";
  check(filter(reply,notification).isEmpty(),"notification text removed and propagates to observed reply");
  notification=new Notification();notification.users.add(u);check(filter(notification).isEmpty(),"notification source name");
  config("interdit",true);Post original=p("24","safe"),r1=p("25","safe"),r2=p("26","safe");r1.result.repost=original.result;r2.result.repost=original.result;check(filter(r1,r2).size()==2,"shared repost context does not cause strict false positive");
  config("autre",false);check(filter(p("19","interdit")).size()==1,"configuration revision clears blocked cache");
  AdvancedMatcher m=new AdvancedMatcher(Arrays.asList("ÉCOLE","ecole","  "),"substring",true);check(m.rules().size()==1&&m.matches("E\u0301C\u200bOLE"),"Unicode accents, format chars, duplicates");
  m=new AdvancedMatcher(Arrays.asList("chat"),"word",true);check(m.matches("un chat!")&&!m.matches("chateau")&&!m.matches("chat_12"),"Unicode whole-word boundaries");
  m=new AdvancedMatcher(Arrays.asList("foo\\D+bar"),"regex",true);check(m.matches("fooZZbar")&&!m.matches("foo12bar"),"regex escapes preserved");
  long start=System.nanoTime();m=new AdvancedMatcher(Arrays.asList("(a+)+$"),"regex",false);check(!m.matches("a".repeat(100000)+"!"),"RE2 nested quantifier no backtracking hang");System.out.printf(Locale.ROOT,"Regex stress %.3f ms%n",(System.nanoTime()-start)/1e6);
  boolean rejected=false;try{new AdvancedMatcher(Arrays.asList("(a)\\1"),"regex",false);}catch(RuntimeException e){rejected=true;}check(rejected,"unsupported backreference rejected");
  check(AdvancedLists.parse("\"a,b\",c\n\"quote\"\"test\"","csv").equals(Arrays.asList("a,b","c","quote\"test")),"quoted CSV");
  check(AdvancedLists.parse("{\"keywords\":[\"abc\",\"def\"]}","json").size()==2,"JSON object import");
  for(int size:new int[]{1000,5000}){List<String> list=new ArrayList<>();for(int i=0;i<size;i++)list.add("term_"+i+"_end");start=System.nanoTime();m=new AdvancedMatcher(list,"substring",true);double build=(System.nanoTime()-start)/1e6;check(m.rules().size()==size&&m.matches("last term_"+(size-1)+"_end"),size+" rules import and match");
   String text="A normal post with a link and some harmless words. ".repeat(6);for(int i=0;i<2000;i++)m.matches(text);start=System.nanoTime();for(int i=0;i<20000;i++)if(m.matches(text))throw new AssertionError("false positive");System.out.printf(Locale.ROOT,"BENCH rules=%d build_ms=%.3f posts=20000 text_chars=%d average_us=%.3f%n",size,build,text.length(),(System.nanoTime()-start)/2e7);}
  AdvancedGraph g=new AdvancedGraph();for(int i=999;i>=0;i--)g.observe("n"+i,i==0,i==0?Collections.emptyList():Arrays.asList("n"+(i-1)));g.evaluate(false);check(g.blocked("n999"),"1000-deep chain without recursion");
  app.morphe.extension.shared.Utils.file=java.nio.file.Files.createTempFile("advanced-pref-test", ".properties");
  JSONObject saved=new JSONObject();saved.put("keywords",new JSONArray(Arrays.asList("savedword")));saved.put("enabled",false);AdvancedConfig.save(saved);
  java.lang.reflect.Field cache=AdvancedConfig.class.getDeclaredField("current");cache.setAccessible(true);cache.set(null,null);
  AdvancedConfig loaded=AdvancedConfig.get();check(loaded.keywords.equals(Arrays.asList("savedword"))&&!loaded.enabled,"persistent configuration reload does not overwrite with Morphe seed");
  java.nio.file.Files.delete(app.morphe.extension.shared.Utils.file);
  System.out.println("TOTAL PASS "+checks);System.out.println(AdvancedFilter.diagnostics());
 }
}
