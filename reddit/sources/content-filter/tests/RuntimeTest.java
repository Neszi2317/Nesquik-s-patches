import java.util.*;import java.lang.reflect.*;
import local.reddit.filter.*;
import com.reddit.domain.model.*;
// Minimal fixtures follow the actual 2026.47.0 field descriptors. No Android/UI emulation claimed.
class ps50 {String c;ps50(String name){c=name;}}
class qs50 {ps50 a;qs50(String name){a=new ps50(name);}}
class nj50 {qs50 i;nj50(String name){i=new qs50(name);}}
class pj50 {String a;nj50 d;pj50(String id,String name){a=id;d=new nj50(name);}}
class opa {
 public Object a;public Map b;public List c;public Map d,e,f,g;public List h;public Map i;public Object j;
 public opa(Object a,Map b,List c,Map d,Map e,Map f,Map g,List h,Map i,Object j){this.a=a;this.b=b;this.c=c;this.d=d;this.e=e;this.f=f;this.g=g;this.h=h;this.i=i;this.j=j;}
}
class qs40 {String e;List f;qs40(String id,Object... children){e=id;f=Arrays.asList(children);}}
class mu40 {String a,d="normal",b,c,e,f,g,h,i;mu40(String text){a=text;}}
class pw40 {List i;pw40(Object... flairs){i=Arrays.asList(flairs);}}
class y39 {Object t;String e="t3_compact",q,k="normal";y39(String text){q=text;}}
class ghw {String m,n;ghw(String name){m="r/"+name;n="/r/"+name+"/";}}
class g560 {String i;g560(String text){i=text;}}
class vdd0 {String i;vdd0(String text){i=text;}}
class vof0 {qs40 e;vof0(qs40 v){e=v;}}
class kk50 {String e="t3_preview";Object A,l,n,o,p,q,s;kk50(Object link,Object title,Object preview){A=link;n=title;o=preview;}}
class w270 {String i;w270(String text){i=text;}}
class p9m0 {String e="t3_thumbnail";Object i,j;p9m0(Object title,Object preview){i=title;j=preview;}}
class vmt {String e="t3_linear";Object j,k,n;vmt(Object title,Object content){j=title;n=content;}}
class umt {Object a;umt(Object text){a=text;}}
class TextContent {Object element;TextContent(Object x){element=x;}public Object getElement(){return element;}}
class w0h0 {String b;w0h0(String name){b=name;}}
class z0h0 {String b;z0h0(String name){b=name;}} // Profile behavior must remain visible.
class v0h0 {Object a;v0h0(Object v){a=v;}}
class a1h0 {Object b;a1h0(Object v){b=new v0h0(v);}}
class b1h0 {Object k;b1h0(Object v){k=new v0h0(v);}}
class w86 {String b;w86(String name){b=name;}}
class ozg0 {Object a;ozg0(Object v){a=v;}}
class mzg0 {Object b;mzg0(String name){b=new ozg0(new w86(name));}}
class hbf0 {String k;hbf0(String name){k=name;}}
class skf0 {Object f;skf0(String name){f=new hbf0(name);}}
class hag0 {String s,e,O,P;hag0(String name,String title,String body,String preview){s=name;e=title;O=body;P=preview;}}
class vmf0 {Object e;vmf0(String name,String title,String body,String preview){e=new hag0(name,title,body,preview);}}
public class RuntimeTest {
 static int tests;
 static void ok(boolean v,String label){if(!v)throw new AssertionError(label);tests++;System.out.println("PASS "+label);}
 static void configure(String text)throws Exception {Field f=RedditContentFilter.class.getDeclaredField("snapshot");f.setAccessible(true);f.set(null,new KeywordRules(text));}
 public static void main(String[] args)throws Exception{
  configure("cardiology\nexampleblockedword");
  Link normal=new Link("t3_normal","Normal","text","text"),title=new Link("t3_title","CARDIOLOGY news","",""),self=new Link("t3_self","Normal","","Contains exampleblockedword"),body=new Link("t3_body","Normal","cardiology","");
  List<Link> original=Arrays.asList(normal,title,self,body);List<?> filtered=RedditContentFilter.filterListing(original);
  ok(filtered.equals(Arrays.asList(normal)),"posts: normal visible; title/selftext/body removed, case insensitive");
  ok(original.size()==4,"input listing not mutated");
  ok(!RedditContentFilter.hideFeedPost(new qs40("t3_1",new g560("normal"))),"feed normal visible");
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_2",new g560("Cardiology"))),"feed title hidden");
  ok(RedditContentFilter.hideFeedPost(new y39("ExampleBlockedWord")),"compact title hidden");
  ok(RedditContentFilter.hideFeedPost(new vof0(new qs40("t3_2",new g560("Cardiology")))),"search wrapper title hidden");
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_3",new vdd0("{\"document\":[{\"e\":\"par\",\"c\":[{\"e\":\"text\",\"t\":\"exampleblockedword\"}]}]}"))),"richtext body hidden");
  ok(!RedditContentFilter.hideFeedPost(new qs40("t3_4",new vdd0("{\"document\":[{\"e\":\"par\",\"c\":[{\"e\":\"link\",\"t\":\"normal\",\"u\":\"https://cardiology.com\"}]}]}"))),"richtext URL metadata excluded");
  RedditContentFilter.rememberLink(self);
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_self",new g560("Normal"))),"cached full selftext covers preview omission");
  ok(RedditContentFilter.hideFeedPost(new kk50(self,null,null)),"cached preview model uses actual full Link body");
  ok(RedditContentFilter.hideFeedPost(new kk50(null,null,new w270("cardiology"))),"preview text filters whole post");
  ok(RedditContentFilter.hideFeedPost(new p9m0(new g560("normal"),new w270("cardiology"))),"thumbnail card preview filters whole post");
  ok(RedditContentFilter.hideFeedPost(new vmt(new g560("normal"),new umt(new TextContent(new w270("cardiology"))))),"linear card text filters whole post");
  Map<String,Comment> comments=new LinkedHashMap<>();
  comments.put("t1_root",new Comment("t1_root","t3_post","Normal"));
  comments.put("t1_bad",new Comment("t1_bad","t1_root","CaRdIoLoGy"));
  comments.put("t1_child",new Comment("t1_child","t1_bad","Normal reply"));
  comments.put("t1_sibling",new Comment("t1_sibling","t1_root","Normal sibling"));
  List<String> ids=new ArrayList<>(comments.keySet());Map<String,List<String>> grouped=new LinkedHashMap<>();grouped.put("t1_root",ids);
  Map empty=Collections.emptyMap();opa state=new opa(null,comments,ids,empty,empty,empty,grouped,ids,empty,new Object());
  opa view=(opa)RedditContentFilter.filterCommentState(state);
  ok(view.j==state.j,"new 2026.47 comment state metadata preserved");
  ok(view.c.equals(Arrays.asList("t1_root","t1_sibling")),"comment body hides branch; normal comments with unrelated blocked metadata remain");
  ok(view.g.get("t1_root").equals(view.c),"grouped rendering list also pruned");
  ok(view.b==comments&&state.c.size()==4&&state.g==grouped,"original thread, comment data and maps intact");
  configure("");ok(RedditContentFilter.filterListing(original)==original,"empty list returns same post list");ok(RedditContentFilter.filterCommentState(state)==state,"empty list returns same comment state");
  configure("other");ok(RedditContentFilter.filterListing(original)==original,"removed keyword restores posts");ok(RedditContentFilter.filterCommentState(state)==state,"removed keyword restores comment branch");
  Field subs=RedditContentFilter.class.getDeclaredField("communities");subs.setAccessible(true);
  SubredditRules sr=new SubredditRules(" Foo\nr/FOO\n /r/foo/ \n\nBar ");
  ok(sr.count==2&&sr.matches("R/FoO")&&!sr.matches("foobar"),"subreddits normalize, deduplicate and match exact names");
  subs.set(null,sr);configure("");normal.subreddit="foo";
  ok(RedditContentFilter.filterListing(Arrays.asList(normal,title)).equals(Arrays.asList(title)),"subreddit-only list blocks actual Link community");
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_sub",new ghw("FOO"))),"feed header community removed with empty keyword list");
  ok(!RedditContentFilter.hideFeedPost(new qs40("t3_ok",new ghw("foobar"),new g560("foo"))),"community names are not keywords");
  y39 compact=new y39("normal");compact.k="foo";ok(RedditContentFilter.hideFeedPost(compact),"compact community hidden");
  kk50 preview=new kk50(null,null,null);preview.l=new ghw("foo");ok(RedditContentFilter.hideFeedPost(preview),"preview community header hidden");
  RedditContentFilter.rememberLink(normal);ok(RedditContentFilter.hideFeedPost(new qs40("t3_normal",new g560("normal"))),"cached Link community hidden");
  ok(RedditContentFilter.filterCommentState(state)==state,"subreddit list leaves comment tree unchanged");
  subs.set(null,new SubredditRules(""));ok(!RedditContentFilter.hideFeedPost(new qs40("t3_normal",new ghw("foo"))),"removing subreddit restores posts");
  subs.set(null,new SubredditRules("r/AskReddit"));configure("");
  a1h0 blockedSuggestion=new a1h0(new w0h0("ASKREDDIT")),similarSuggestion=new a1h0(new w0h0("AskScience")),profile=new a1h0(new z0h0("AskReddit"));
  Object query=new Object();List<Object> search=Arrays.asList(blockedSuggestion,similarSuggestion,profile,query);
  ok(RedditContentFilter.filterSearchItems(search).equals(Arrays.asList(similarSuggestion,profile,query)),"typeahead exact community match; similar communities, profiles and free queries preserved");
  ok(search.size()==4,"typeahead source/cache list unmodified");
  mzg0 blockedRow=new mzg0("/r/AskReddit"),allowedRow=new mzg0("AskRedditExtra");
  ArrayList<Object> row=new ArrayList<>(Arrays.asList(blockedRow,allowedRow));List<?> rowResult=RedditContentFilter.filterSearchItems(row);
  ok(rowResult instanceof ArrayList&&rowResult.equals(Arrays.asList(allowedRow)),"suggested community row filtered, ArrayList type preserved");
  ok(RedditContentFilter.filterSearchItems(Arrays.asList(blockedRow)).isEmpty(),"all blocked row becomes empty without null");
  ok(RedditContentFilter.hideFeedPost(new b1h0(new w0h0("askreddit"))),"cached suggestion feed community blocked");
  ok(!RedditContentFilter.hideFeedPost(new b1h0(new z0h0("askreddit"))),"cached suggestion profile preserved");
  ok(RedditContentFilter.hideFeedPost(new skf0("r/ASKREDDIT"))&&!RedditContentFilter.hideFeedPost(new skf0("AskRedditExtra")),"community search exact normalized name");
  ok(RedditContentFilter.hideFeedPost(new vmf0("AskReddit","normal","","")),"dynamic search posts of blocked community hidden");
  ok(!RedditContentFilter.hideFeedPost(new vmf0("AskScience","AskReddit","","")),"subreddit rules do not match titles");
  subs.set(null,new SubredditRules(""));
  ok(RedditContentFilter.filterSearchItems(search)==search,"empty subreddit list preserves original suggestions");
  ok(RedditContentFilter.filterSearchItems(null)==null,"null suggestion list safe");
  ok(!RedditContentFilter.hideFeedPost(new skf0("AskReddit")),"removed community rule restores results");
  configure("cardiology");
  ok(RedditContentFilter.hideFeedPost(new vmf0("AskScience","Cardiology","","")),"dynamic search keyword title");
  ok(RedditContentFilter.hideFeedPost(new vmf0("AskScience","normal","CARDIOLOGY","")),"dynamic search keyword selftext");
  ok(RedditContentFilter.hideFeedPost(new vmf0("AskScience","normal","","cardiology")),"dynamic search keyword preview text");
  ok(!BundledConfig.shouldApply(null,"")&&BundledConfig.shouldApply(null,"AskReddit"),"first run: empty Manager defaults preserve existing settings; provided defaults apply");
  ok(!BundledConfig.shouldApply("foo","foo")&&BundledConfig.shouldApply("foo","bar"),"unchanged Manager config preserves app edits; changed config applies once");
  ok(BundledConfig.shouldApply("foo",""),"cleared Manager list applies on next patched update");
  Locale previousLocale=Locale.getDefault();
  try {
   Locale.setDefault(new Locale("tr","TR"));
   SubredditRules cases=new SubredditRules("lacaka\nLACAKA\n/r/LaCaKa/\nINDIA");
   ok(cases.count==2&&cases.matches("laCaka")&&cases.matches("LACAKA"),"lacaka / laCaka / LACAKA case-insensitive with Turkish device locale");
   ok(new SubredditRules("LACAKA").matches("lacaka"),"uppercase rule matches lowercase subreddit");
   ok(cases.matches("r/LaCaKa")&&cases.matches("/r/LACAKA/")&&cases.matches("india"),"ROOT normalization and existing prefixes preserved");
   ok(!cases.matches("lacakaextra")&&!cases.matches("other"),"subreddit match remains exact");
   ok(cases.matchesHeader("https://www.reddit.com/r/laCaka/?feed=home#top"),"header URL parameters do not prevent exact community matching");
   ok(!cases.matchesHeader("https://evil.test/r/lacaka")&&!cases.matchesHeader("u/lacaka")&&!cases.matchesHeader("/r/lacaka/comments/abc"),"unrelated URLs users and deeper paths excluded");
   subs.set(null,cases);configure("");
   y39 mixed=new y39("normal");mixed.k="laCaka";
   ok(RedditContentFilter.hideFeedPost(mixed),"mixed-case subreddit actual compact feed path blocked");
   ok(RedditContentFilter.hideFeedPost(new qs40("t3_case",new ghw("LaCaKa"))),"mixed-case subreddit actual header path blocked");
  } finally {Locale.setDefault(previousLocale);subs.set(null,new SubredditRules(""));}
  configure("banane");
  Link flairOnly=new Link("t3_flair","normal","normal","normal");flairOnly.flair="Banane";
  ok(RedditContentFilter.filterListing(Arrays.asList(flairOnly,normal)).equals(Arrays.asList(normal)),"Link flair-only match hidden; unrelated post visible");
  RedditContentFilter.rememberLink(flairOnly);
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_flair",new g560("normal"))),"cached Link post flair hides whole card");
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_flair2",new pw40(new mu40("banane")))) ,"post flair-only match hides feed card");
  y39 flairCompact=new y39("normal");flairCompact.t=new pw40(new mu40("Banane"));
  ok(RedditContentFilter.hideFeedPost(flairCompact),"compact flair case-insensitive");
  kk50 flairPreview=new kk50(null,new g560("normal"),null);flairPreview.q=new pw40(new mu40("banane"));
  ok(RedditContentFilter.hideFeedPost(flairPreview),"preview flair-only match hidden");
  mu40 cleanFlair=new mu40("ordinary");cleanFlair.c="banane";cleanFlair.g="banane";cleanFlair.i="banane";
  ok(!RedditContentFilter.hideFeedPost(new qs40("t3_clean_flair",new pw40(cleanFlair))),"flair IDs colors tooltips not keyword content");
  Link authorOnly=new Link("t3_author","normal","normal","normal");authorOnly.authorFlair="banane";
  ok(RedditContentFilter.filterListing(Arrays.asList(authorOnly)).size()==1,"author flair excluded");
  ok(!RedditContentFilter.hideFeedPost(new qs40("t3_null_flair",new pw40(null,new mu40(null)))) ,"null and absent flair safe");
  configure("");ok(!RedditContentFilter.hideFeedPost(flairCompact),"empty rules restore flair card");
  configure("other");ok(!RedditContentFilter.hideFeedPost(new qs40("t3_flair",new g560("normal"))),"keyword removal reevaluates cached flair");
  configure("cardiology");
  configure("");subs.set(null,new SubredditRules("lacaca"));
  ok(RedditContentFilter.isSubredditBlocked("r/laCaca"),"active settings tester recognizes actual reported subreddit");
  ok(!RedditContentFilter.isSubredditBlocked("lacaka"),"different spelling remains distinct");
  RedditContentFilter.rememberGraphqlPost(new pj50("rawblocked","laCaca"));
  ok(RedditContentFilter.hideFeedPost(new qs40("rawblocked",new g560("Normal"))),"GraphQL community blocks whole home card with missing header");
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_rawblocked",new g560("Normal"))),"raw and fullname post IDs share source metadata");
  RedditContentFilter.rememberGraphqlPost(new pj50("rawallowed","laCacaExtra"));
  ok(!RedditContentFilter.hideFeedPost(new qs40("rawallowed",new g560("Normal"))),"GraphQL community match remains exact");
  pj50 nullCommunity=new pj50("nullcommunity","normal");nullCommunity.d.i=null;
  RedditContentFilter.rememberGraphqlPost(nullCommunity);RedditContentFilter.rememberGraphqlPost(null);
  ok(!RedditContentFilter.hideFeedPost(new qs40("nullcommunity",new g560("Normal"))),"missing raw community safe");
  Link lateLink=new Link("t3_late","Normal","","");
  RedditContentFilter.rememberLink(lateLink);lateLink.subreddit="laCaca";
  ok(!RedditContentFilter.hideFeedPost(new qs40("t3_late",new g560("Normal"))),"constructor cache cannot see later deserialization changes");
  RedditContentFilter.rememberLink(lateLink);
  ok(RedditContentFilter.hideFeedPost(new qs40("t3_late",new g560("Normal"))),"mapper refresh observes community populated after construction");
  subs.set(null,new SubredditRules(""));
  ok(!RedditContentFilter.hideFeedPost(new qs40("rawblocked",new g560("Normal"))),"removing community rule reevaluates raw cache");
  ok(!RedditContentFilter.hideFeedPost(new qs40("t3_late",new g560("Normal"))),"removing community rule reevaluates Link cache");
  String diagnostic=RedditContentFilter.diagnostics();
  ok(diagnostic.contains("2.3.0")&&diagnostic.contains("hidden.qs40")&&!diagnostic.contains("rawblocked")&&!diagnostic.contains("laCaca"),"diagnostics contain version and counters, no post IDs or subreddit names");
  configure("cardiology");
  KeywordRules literals=new KeywordRules("\uFEFFTrump\r\n trump \n\nTRUMP\na.b\nc++\n(x)\n[y]\nartificial intelligence");
  ok(literals.count==6,"BOM, CRLF, trimming, blanks and case-insensitive deduplication");
  ok(literals.matches("TrUmP")&&literals.matches("ARTIFICIAL INTELLIGENCE"),"case and full expression matching");
  ok(literals.matches("a.b")&&!literals.matches("axb")&&literals.matches("c++")&&literals.matches("(x)")&&literals.matches("[y]"),"regex metacharacters are literal");
  ok(new KeywordRules("école").matches("ÉCOLE"),"Unicode case-insensitive match");
  StringBuilder many=new StringBuilder();for(int i=0;i<10000;i++)many.append("keyword_").append(i).append("_end\n");
  long start=System.nanoTime();KeywordRules bulk=new KeywordRules(many.toString());long built=System.nanoTime();
  for(int i=0;i<100000;i++)if(bulk.matches("ordinary unrelated text in a comment"))throw new AssertionError("false positive");
  ok(bulk.matches("KEYWORD_9999_END"),"10,000-rule bulk matcher");
  System.out.println("PERF 10000 rules compile_ms="+(built-start)/1000000+" 100000 scans_ms="+(System.nanoTime()-built)/1000000);
  System.out.println("TOTAL "+tests+" checks passed; Android UI/scroll not executed.");
 }
}
