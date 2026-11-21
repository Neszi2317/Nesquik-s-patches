package app.morphe.extension.newx.hidesensitive;
import java.util.*;
/** Guards disclosure by exception fallback, empty containers and stale filtering after an update. */
public final class SensitivePostFilterHostTest {
 static final class Post { boolean sensitive; boolean broken; Post(boolean s){sensitive=s;} }
 static final class Wrap { Object value; Wrap(Object v){value=v;} }
 static final class Module { List<Object> children; boolean broken; Module(Object... c){children=Arrays.asList(c);} }
 static final class Access extends SensitivePostFilter.Access {
  boolean post(Object o){return o instanceof Post;}
  boolean wrapper(Object o){return o instanceof Wrap;}
  boolean module(Object o){return o instanceof Module;}
  boolean sensitive(Object o){if(((Post)o).broken)throw new IllegalStateException();return ((Post)o).sensitive;}
  Object unwrap(Object o){return ((Wrap)o).value;}
  Object id(Object o){return o;}
  List<?> children(Object o){return ((Module)o).children;}
  Object freeze(List<Object> l){return Collections.unmodifiableList(l);}
  Object copyWrapper(Object o,Object c){return new Wrap(c);}
  Object copyModule(Object o,List<Object> c,Set<Object> ids){
   if(((Module)o).broken)throw new IllegalStateException();return new Module(c.toArray());
  }
 }
 static final Access A=new Access(); static int checks;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 static List<?> filter(Object... o){return (List<?>)SensitivePostFilter.filter(Arrays.asList(o),A);}
 public static void main(String[] args){
  Post safe=new Post(false), bad=new Post(true); Object cursor=new Object();
  check(filter(bad,safe,bad,cursor).equals(Arrays.asList(safe,cursor)),"compact order, cursor retained");
  List<Object> unchanged=Arrays.asList(safe,cursor);
  check(SensitivePostFilter.filter(unchanged,A)==unchanged,"unchanged identity");
  check(filter(new Wrap(bad),new Module(new Wrap(bad))).isEmpty(),"wrappers and empty modules removed");
  Module mixed=new Module(new Wrap(bad),new Wrap(safe));
  Module kept=(Module)filter(mixed).get(0);
  check(kept!=mixed&&kept.children.size()==1&&((Wrap)kept.children.get(0)).value==safe,"partial module rebuilt");
  check(mixed.children.size()==2,"source immutable");
  check(filter(new Module(new Wrap(new Module(new Wrap(bad))))).isEmpty(),"nested empty modules");
  Post broken=new Post(false);broken.broken=true;
  check(filter(broken,safe).equals(List.of(safe)),"classifier error cannot reveal post");
  mixed.broken=true;
  check(filter(mixed,safe).equals(List.of(safe)),"copy error cannot restore module");
  Wrap cycle=new Wrap(null);cycle.value=cycle;
  check(filter(cycle,safe).equals(List.of(safe)),"cycle bounded");
  Iterable<Object> failure=()->new Iterator<Object>(){public boolean hasNext(){throw new IllegalStateException();}public Object next(){return null;}};
  check(((List<?>)SensitivePostFilter.filter(failure,A)).isEmpty(),"page iteration error fails closed");
  check(filter(bad,cursor).equals(List.of(cursor)),"fully filtered page retains pagination");
  Post updated=new Post(false);List<Object> page=List.of(updated);
  check(SensitivePostFilter.filter(page,A)==page,"first clean pass");updated.sensitive=true;
  check(((List<?>)SensitivePostFilter.filter(page,A)).isEmpty(),"updated metadata never uses stale memo");
  check(SensitivePostFilter.filter(unchanged)==unchanged,"unselected feature pass-through");
  check(filter(null,safe).equals(List.of(safe)),"no null hole");
  System.out.println("PASS: "+checks+" production-traversal assertions (host JVM; not Android rendering)");
 }
}
