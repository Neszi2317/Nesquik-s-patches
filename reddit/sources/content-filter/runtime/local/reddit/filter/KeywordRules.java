package local.reddit.filter;
import java.util.*;
/** Immutable literal Aho-Corasick matcher. Built once per configuration change. */
public final class KeywordRules {
    private static final class Node {
        final HashMap<Character, Node> next = new HashMap<>();
        Node fail; boolean end;
    }
    private final Node root = new Node();
    public final String text;
    public final int count;
    private static char fold(char c) { return Character.toLowerCase(Character.toUpperCase(c)); }
    public static String normalize(String text) {
        if (text == null) return "";
        if (text.length() > 2_000_000) throw new IllegalArgumentException("Liste trop volumineuse (2 millions de caractères maximum).");
        LinkedHashMap<String,String> unique = new LinkedHashMap<>();
        for (String line : text.replace("\uFEFF", "").split("\\r\\n|\\r|\\n")) {
            line = line.trim(); if (line.isEmpty()) continue;
            StringBuilder key = new StringBuilder();
            for (int i=0;i<line.length();i++) key.append(fold(line.charAt(i)));
            if (!unique.containsKey(key.toString())) unique.put(key.toString(), line);
            if (unique.size()>50000) throw new IllegalArgumentException("50 000 règles maximum.");
        }
        StringBuilder out = new StringBuilder();
        for (String line : unique.values()) { if (out.length()>0) out.append('\n'); out.append(line); }
        return out.toString();
    }
    public KeywordRules(String input) {
        text = normalize(input); count = text.isEmpty()?0:text.split("\n").length;
        root.fail=root;
        if (!text.isEmpty()) for (String line:text.split("\n")) {
            Node p=root;
            for (int i=0;i<line.length();i++) {
                char c=fold(line.charAt(i)); Node next=p.next.get(c);
                if(next==null){next=new Node();p.next.put(c,next);}p=next;
            } p.end=true;
        }
        ArrayDeque<Node> queue=new ArrayDeque<>();
        for(Node p:root.next.values()){p.fail=root;queue.add(p);}
        while(!queue.isEmpty()) {
            Node p=queue.remove();
            for(Map.Entry<Character,Node> entry:p.next.entrySet()) {
                char c=entry.getKey();Node n=entry.getValue(), f=p.fail;
                while(f!=root&&!f.next.containsKey(c))f=f.fail;
                Node candidate=f.next.get(c);n.fail=candidate==null?root:candidate;
                n.end |= n.fail.end;queue.add(n);
            }
        }
    }
    public boolean matches(String value) {
        if(count==0||value==null)return false;Node p=root;
        for(int i=0;i<value.length();i++) {
            char c=fold(value.charAt(i));
            while(p!=root&&!p.next.containsKey(c))p=p.fail;
            Node next=p.next.get(c);p=next==null?root:next;if(p.end)return true;
        }return false;
    }
}
