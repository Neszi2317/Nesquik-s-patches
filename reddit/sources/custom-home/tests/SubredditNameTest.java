import app.neszi.reddit.customhome.SubredditName;
public class SubredditNameTest {
 public static void main(String[] args) {
  String[][] good={{"nightfeeling","nightfeeling"},{"r/nightfeeling","nightfeeling"},{"https://www.reddit.com/r/nightfeeling/","nightfeeling"},{" R/NightFeeling ","nightfeeling"},{"https://old.reddit.com/r/Ask_Reddit/","ask_reddit"},{"00","00"},{"a____________________","a____________________"}};
  String[] bad={"","r/","a","a_____________________","r/all","popular","r/home","https://reddit.com.evil.test/r/nightfeeling/","https://reddit.com@evil.test/r/nightfeeling/","https://reddit.com:443/r/nightfeeling/","https://reddit.com/r/nightfeeling/comments/x","r/foo+bar","a b","a-b","https://reddit.com/r/nightfeeling/?x=1","https://reddit.com/r/nightfeeling/#x","https://reddit.com/r/%6eightfeeling/","https://reddit.com/r/nightfeeling//","/r/nightfeeling","r/mod",null};
  int count=0;
  for(String[] t:good){if(!SubredditName.normalize(t[0]).equals(t[1]))throw new AssertionError(t[0]);count++;}
  for(String t:bad){boolean rejected=false;try{SubredditName.normalize(t);}catch(IllegalArgumentException e){rejected=true;}if(!rejected)throw new AssertionError("Accepted "+t);count++;}
  System.out.println("PASS: "+count+" normalization / validation cases");
 }
}
