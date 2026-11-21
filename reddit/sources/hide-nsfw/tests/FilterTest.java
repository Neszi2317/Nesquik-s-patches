import java.util.*;
import app.neszi.reddit.nsfw.NsfwFilter;
import com.reddit.domain.model.Link;
import com.reddit.feeds.model.IndicatorType;
import com.reddit.type.CellIndicatorType;
public class FilterTest {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("Failed #"+checks);}
 static class Node implements NsfwFilter.Sources {
  Object[] children;
  Node(Object... c){children=c;}
  public Object[] neszi_nsfwSources(){return children;}
 }
 enum Fake { NSFW }
 public static void main(String[] args){
  Link safe=new Link(false), adult=new Link(true);
  check(NsfwFilter.filter(null)==null);
  List<?> empty=Collections.emptyList();check(NsfwFilter.filter(empty)==empty);
  check(!NsfwFilter.isNsfw(null));check(NsfwFilter.isNsfw(adult));check(!NsfwFilter.isNsfw(safe));
  check(NsfwFilter.isNsfw(IndicatorType.NSFW));check(NsfwFilter.isNsfw(CellIndicatorType.NSFW));
  check(!NsfwFilter.isNsfw(IndicatorType.SPOILER));check(!NsfwFilter.isNsfw(IndicatorType.ORIGINAL));
  check(!NsfwFilter.isNsfw(IndicatorType.QUARANTINED));check(!NsfwFilter.isNsfw(Fake.NSFW));
  check(!NsfwFilter.isNsfw("NSFW"));check(!NsfwFilter.isNsfw("18+"));
  check(!NsfwFilter.isNsfw(new Object(){public String toString(){return "nsfw=true";}}));
  Node nested=new Node(new Node(Arrays.asList(IndicatorType.NSFW)));
  check(NsfwFilter.isNsfw(nested));check(NsfwFilter.isNsfw(new Node(new Node(adult))));
  check(!NsfwFilter.isNsfw(new Node(safe,IndicatorType.SPOILER)));
  List<Object> original=Collections.unmodifiableList(Arrays.asList(safe,adult,"header",nested,null));
  List<?> filtered=NsfwFilter.filter(original);
  check(filtered.equals(Arrays.asList(safe,"header",null)));check(original.size()==5);check(filtered!=original);
  check(adult.getOver18());check(NsfwFilter.filter(filtered)==filtered);
  check(NsfwFilter.filter(Arrays.asList(adult,nested)).isEmpty());
  List<?> safeOnly=Arrays.asList(safe,"header");check(NsfwFilter.filter(safeOnly)==safeOnly);
  Node cycle=new Node();cycle.children=new Object[]{cycle};check(!NsfwFilter.isNsfw(cycle));
  cycle.children=new Object[]{cycle,adult};check(NsfwFilter.isNsfw(cycle));
  Node nullChildren=new Node();nullChildren.children=null;check(!NsfwFilter.isNsfw(nullChildren));
  Node crosspost=new Node(new Node(safe),new Node(IndicatorType.NSFW));check(NsfwFilter.isNsfw(crosspost));
  List<?> big=new ArrayList<>();ArrayList<Object> large=new ArrayList<>();for(int i=0;i<10000;i++)large.add(i%2==0?safe:adult);
  check(NsfwFilter.filter(large).size()==5000);
  System.out.println("PASS: "+checks+" host assertions (explicit native-model doubles; no Android/device test)");
 }
}
