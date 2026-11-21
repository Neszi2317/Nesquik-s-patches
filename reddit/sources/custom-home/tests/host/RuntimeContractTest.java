import android.content.*;
import android.os.Bundle;
import app.neszi.reddit.customhome.CustomHome;
import com.reddit.feeds.home.impl.ui.HomeFeedScreen;
import com.reddit.screens.listing.compose.SubredditFeedScreen;
public class RuntimeContractTest {
 enum Tab { Home, Chat, Inbox }
 static int count;
 static void check(boolean b){count++;if(!b)throw new AssertionError("Contract #"+count);}
 public static void main(String[] args){
  Context c=new Context();CustomHome.init(c);
  SharedPreferences p=c.getSharedPreferences("neszi_custom_home_v1",0);
  check(CustomHome.enabled());check(CustomHome.blocking());
  check(CustomHome.hideHome(Tab.Home));check(!CustomHome.hideHome(Tab.Chat));check(!CustomHome.hideHome(null));
  HomeFeedScreen h=new HomeFeedScreen();h.c.putString("host_screen_instance_id","parent");h.c.putString("Screen.STATE_INSTANCE_PROPERTIES","unsafe_home_state");
  Object result=CustomHome.replaceFactoryResult(h);check(result instanceof SubredditFeedScreen);
  var sub=(SubredditFeedScreen)result;check("nightfeeling".equals(sub.c.getString("subredditName")));
  check("parent".equals(sub.c.getString("host_screen_instance_id")));check(!sub.c.containsKey("Screen.STATE_INSTANCE_PROPERTIES"));
  check(h.c.containsKey("Screen.STATE_INSTANCE_PROPERTIES"));check(CustomHome.replaceEntry(sub)==sub);
  check(CustomHome.replaceFactoryResult(sub)==sub);check(CustomHome.replaceFactoryResult(null)==null);
  check(CustomHome.replaceEntry(h) instanceof SubredditFeedScreen);
  p.edit().putString("subreddit","r/test_home").commit();check("test_home".equals(CustomHome.subreddit()));
  check("test_home".equals(((SubredditFeedScreen)CustomHome.replaceEntry(h)).c.getString("subredditName")));
  p.edit().putString("subreddit","https://evil.test/r/test").commit();check("nightfeeling".equals(CustomHome.subreddit()));
  Bundle b=new Bundle();b.putInt("currentTabIndex",4);check(CustomHome.prepareHomeArgs(b).getInt("currentTabIndex")==0);check(b.getInt("currentTabIndex")==4);
  p.edit().putBoolean("disable_main_feed",false).commit();check(!CustomHome.blocking());check(CustomHome.replaceEntry(h)==h);
  CustomHome.beginActivity(c);check(CustomHome.replaceFactoryResult(h) instanceof SubredditFeedScreen);check(CustomHome.replaceFactoryResult(h)==h);
  p.edit().putBoolean("enabled",false).commit();check(!CustomHome.enabled());check(!CustomHome.hideHome(Tab.Home));check(CustomHome.replaceFactoryResult(h)==h);check(CustomHome.replaceEntry(h)==h);check(CustomHome.prepareHomeArgs(b)==b);
  p.edit().putBoolean("enabled",true).putBoolean("disable_main_feed",true).commit();check(CustomHome.replaceEntry(h) instanceof SubredditFeedScreen);
  System.out.println("PASS: "+count+" runtime contracts using explicit JVM doubles (not Android lifecycle tests)");
 }
}
