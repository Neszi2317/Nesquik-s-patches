package app.morphe.extension.newx.anonymous;
import java.util.*;
/** Guards collision instability, grammar, identity leakage, LRU eviction and invalid-ID crashes. */
public class AliasEngineTests {
 static int checks;
 static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
 byte[] key=new byte[32];Arrays.fill(key,(byte)17);AliasEngine e=new AliasEngine(key),restart=new AliasEngine(key);
 ok(NameBank.ANIMALS.length*NameBank.MALE.length>=50000,"Bank capacity");
 ok(NameBank.FEMININE.length==NameBank.ANIMALS.length&&NameBank.MALE.length==NameBank.FEMALE.length,"Grammar array sizes");
 for(String bad:new String[]{null,"","0","-1","John","00001","12.2","123456789012345678901"})ok(e.get(bad)==null,"Invalid ID safe");
 String first=e.get("4821").name;Set<String> names=new HashSet<>(),handles=new HashSet<>();
 long start=System.nanoTime();
 for(int i=1;i<=100000;i++){String id=Long.toString(1000000000000000000L+i);var a=e.get(id);if(!names.add(a.name)||!handles.add(a.handle))throw new AssertionError("Collision at "+i);if(a.name.contains(id)||a.handle.contains(id))throw new AssertionError("ID disclosed");if(i%997==0)ok(a.name.equals(restart.get(id).name)&&a.handle.equals(restart.get(id).handle),"Stable across fresh engines");}
 System.out.println("100000 distinct accounts: zero alias/handle collisions; "+((System.nanoTime()-start)/1000000)+" ms cold host benchmark");
 ok(e.cacheSize()==AliasEngine.CACHE_LIMIT,"Bounded memory");ok(first.equals(e.get("4821").name),"Eviction does not change aliases");
 byte[] different=key.clone();different[0]++;ok(!new AliasEngine(different).get("4821").handle.equals(e.get("4821").handle),"Per-install secret");
 ok(e.get("1").name.matches(".+ · [0-9a-f]{8}"),"Permanent collision suffix");ok(e.get("1").handle.matches("anonyme_[0-9a-f]{24}"),"96-bit handle");
 start=System.nanoTime();for(int i=0;i<1000000;i++)e.get("4821");System.out.println("1000000 cached accesses: "+((System.nanoTime()-start)/1000000)+" ms host benchmark");
 // Exercise synchronization with simultaneous callers (Mac is not thread-safe without locking).
 List<Thread> workers=new ArrayList<>();List<Throwable> failures=Collections.synchronizedList(new ArrayList<>());
 for(int t=0;t<8;t++){Thread worker=new Thread(()->{try{for(int i=1;i<5000;i++)if(!e.get(""+i).name.equals(restart.get(""+i).name))throw new AssertionError("Concurrent alias drift");}catch(Throwable x){failures.add(x);}});workers.add(worker);worker.start();}for(Thread w:workers)w.join();ok(failures.isEmpty(),"Concurrent stability");
 System.out.println("PASS "+checks+" assertions; stability, 100000 collision sample, invalid inputs, grammar dimensions, cache, concurrency");
 }
}
