package app.morphe.extension.newx.postfilter;
import java.util.*;
/** Bounded graph of observed posts. No network, order-independent propagation, cycle-safe BFS. */
public final class AdvancedGraph {
    private static final int LIMIT=16384;
    private final LinkedHashMap<String,Node> nodes=new LinkedHashMap<>(256,.75f,true);
    private Set<String> blocked=Collections.emptySet();
    public static final class Node {
        final boolean direct;final Set<String> parents;
        Node(boolean d,Collection<String> p){direct=d;parents=new HashSet<>(p);parents.remove(null);parents.remove("");}
    }
    public void observe(String id,boolean direct,Collection<String> parents){
        if(id==null||id.isEmpty())return;nodes.put(id,new Node(direct,parents));
        while(nodes.size()>LIMIT)nodes.remove(nodes.keySet().iterator().next());
    }
    public void evaluate(boolean strict){
        Map<String,Set<String>> children=new HashMap<>();HashSet<String> deny=new HashSet<>();ArrayDeque<String> q=new ArrayDeque<>();
        for(Map.Entry<String,Node> e:nodes.entrySet()){
            String id=e.getKey();Node n=e.getValue();boolean missing=false;
            for(String parent:n.parents){children.computeIfAbsent(parent,k->new HashSet<>()).add(id);if(!nodes.containsKey(parent))missing=true;}
            if(n.direct||(strict&&missing)){deny.add(id);q.add(id);}
        }
        while(!q.isEmpty()){Set<String> c=children.get(q.remove());if(c!=null)for(String id:c)if(deny.add(id))q.add(id);}
        // Cycles containing no forbidden content remain allowed. No recursive graph walk.
        blocked=deny;
    }
    public boolean blocked(String id){return id!=null&&blocked.contains(id);}
    public void clear(){nodes.clear();blocked=Collections.emptySet();}
    public int size(){return nodes.size();}
}
