package local.reddit.filter;
import java.util.*;
/** Exact community names, independent from substring keyword rules. */
public final class SubredditRules {
 public final String text; public final int count;
 private final Set<String> names;
 public SubredditRules(String input) {
  if(input.length()>2_000_000)throw new IllegalArgumentException("Liste trop volumineuse");
  LinkedHashSet<String> set=new LinkedHashSet<>();
  for(String line:input.replace("\uFEFF","").split("\\r?\\n")) {
   if(line.trim().isEmpty())continue;
   String name=canonical(line);
   if(name.isEmpty())throw new IllegalArgumentException("Nom de subreddit invalide : "+line);
   set.add(name);if(set.size()>50000)throw new IllegalArgumentException("Maximum 50000 subreddits");
  }
  names=Collections.unmodifiableSet(set);count=set.size();StringBuilder out=new StringBuilder();
  for(String name:set){if(out.length()>0)out.append('\n');out.append(name);}text=out.toString();
 }
 private static String canonical(String value) {
  if(value==null)return "";String s=value.trim().toLowerCase(Locale.ROOT);
  if(s.startsWith("/r/"))s=s.substring(3);else if(s.startsWith("r/"))s=s.substring(2);
  if(s.endsWith("/"))s=s.substring(0,s.length()-1);
  if(s.isEmpty())return "";
  for(int i=0;i<s.length();i++){char c=s.charAt(i);if(!(c>='a'&&c<='z'||c>='0'&&c<='9'||c=='_'))return "";}
  return s;
 }
 public boolean matches(String value){return count!=0&&names.contains(canonical(value));}
 /** Header display text must explicitly be a community, never an author name. */
 public boolean matchesHeader(String value){
  if(value==null)return false;String s=value.trim().toLowerCase(Locale.ROOT);
  for(String prefix:new String[]{"https://www.reddit.com","https://reddit.com","http://www.reddit.com","http://reddit.com"})if(s.startsWith(prefix+"/r/")){s=s.substring(prefix.length());break;}
  // Community links may carry routing parameters; do not broaden rule syntax.
  int query=s.indexOf('?'),fragment=s.indexOf('#');
  int end=query<0?fragment:fragment<0?query:Math.min(query,fragment);
  if(end>=0)s=s.substring(0,end);
  return (s.startsWith("r/")||s.startsWith("/r/"))&&matches(s);
 }
}
