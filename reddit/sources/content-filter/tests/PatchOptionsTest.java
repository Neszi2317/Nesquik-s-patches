import java.util.*;import java.lang.reflect.*;import java.nio.file.*;
public class PatchOptionsTest {
 static int tests;
 static void ok(boolean v,String label){if(!v)throw new AssertionError(label);tests++;System.out.println("PASS "+label);}
 public static void main(String[] args)throws Exception {
  Class<?> c=Class.forName("local.reddit.patches.ContentFilterKt");
  Method normalize=c.getDeclaredMethod("readRules",List.class,String.class,boolean.class);normalize.setAccessible(true);
  ok(normalize.invoke(null,Arrays.asList(" Trump ","TRUMP","\n\na.b\nc++","(x)","[y]"),null,false).equals("Trump\na.b\nc++\n(x)\n[y]"),"Manager keyword list normalization preserves literal characters");
  ok(normalize.invoke(null,Arrays.asList(" AskReddit ","r/ASKREDDIT","/r/askreddit","AskScience"),null,true).equals("askreddit\naskscience"),"Manager subreddit normalization and exact names");
  Locale oldLocale=Locale.getDefault();
  try {Locale.setDefault(new Locale("tr","TR"));
   ok(normalize.invoke(null,List.of("laCaka","LACAKA","/r/lacaka/","INDIA"),null,true).equals("lacaka\nindia"),"Manager ROOT case normalization independent of device locale");
  } finally {Locale.setDefault(oldLocale);}
  Path tmp=Files.createTempFile("reddit-options-", ".txt");
  try {
   Files.writeString(tmp,"\uFEFFCardiology\r\ncardiology\n\nother\n");
   ok(normalize.invoke(null,Arrays.asList("cardiology","local"),tmp.toString(),false).equals("cardiology\nlocal\nother"),"TXT merged with list; BOM CRLF and duplicates removed");
   Files.write(tmp,new byte[]{(byte)0xc3,(byte)0x28});
   try{normalize.invoke(null,List.of(),tmp.toString(),false);throw new AssertionError("invalid UTF-8 accepted");}catch(InvocationTargetException expected){tests++;System.out.println("PASS malformed UTF-8 rejected");}
  }finally{Files.deleteIfExists(tmp);}
  try{normalize.invoke(null,List.of("/r/Ask Reddit"),null,true);throw new AssertionError("invalid name accepted");}catch(InvocationTargetException expected){tests++;System.out.println("PASS invalid subreddit fails at patch time");}
  ok(normalize.invoke(null,null,null,false).equals(""),"unset Manager options are empty");
  Object patch=c.getMethod("getContentFiltersPatch").invoke(null);
  Map<?,?> opts=(Map<?,?>)patch.getClass().getMethod("getOptions").invoke(patch);
  ok(opts.size()==4&&opts.containsKey("blockedKeywords")&&opts.containsKey("blockedSubreddits"),"four options exported to patch metadata");
  ok(opts.get("keywordsFile").getClass().getSimpleName().equals("FilePathOption")&&opts.get("subredditsFile").getClass().getSimpleName().equals("FilePathOption"),"official FilePathOption types for TXT pickers");
  for(Object o:opts.values())System.out.println("OPTION "+o.getClass().getMethod("getTitle").invoke(o));
  System.out.println("TOTAL "+tests+" patch option checks passed");
 }
}
