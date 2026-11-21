package app.morphe.extension.newx.postfilter;

import java.text.Normalizer;
import java.util.*;
import app.morphe.extension.newx.advanced.re2j.Pattern;

/** Immutable matcher; literal mode reuses Piko's existing Aho-Corasick implementation. */
public final class AdvancedMatcher {
    public static final int MAX_RULES=20000, MAX_CHARS=1000000;
    private final PhraseMatcher aho;
    private final List<String> rules;
    private final List<Pattern> regex;
    private final String mode;
    public final boolean foldAccents;
    public static String normalize(String s, boolean fold) {
        if(s==null)return "";
        s=Normalizer.normalize(s, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder b=new StringBuilder(s.length());
        for(int i=0;i<s.length();) {int cp=s.codePointAt(i);i+=Character.charCount(cp);
            if(Character.getType(cp)==Character.FORMAT)continue;
            b.appendCodePoint(Character.isSpaceChar(cp)?' ':cp);
        }
        s=b.toString().trim();
        if(fold){s=Normalizer.normalize(s,Normalizer.Form.NFD);b.setLength(0);
            for(int i=0;i<s.length();) {int cp=s.codePointAt(i);i+=Character.charCount(cp);int t=Character.getType(cp);
                if(t!=Character.NON_SPACING_MARK&&t!=Character.COMBINING_SPACING_MARK&&t!=Character.ENCLOSING_MARK)b.appendCodePoint(cp);}
            s=b.toString();}
        return s;
    }
    public static List<String> clean(Collection<String> input, boolean fold, boolean regex) {
        LinkedHashSet<String> out=new LinkedHashSet<>();int chars=0;
        for(String raw:input){if(raw==null)continue;String x=regex?raw.trim():normalize(raw,fold);if(x.isEmpty())continue;
            if(x.length()>2048)throw new IllegalArgumentException("Une règle dépasse 2048 caractères");
            if(out.add(x))chars+=x.length();
            if(out.size()>MAX_RULES||chars>MAX_CHARS)throw new IllegalArgumentException("Liste trop grande (20 000 règles / 1 Mo)");}
        return new ArrayList<>(out);
    }
    public AdvancedMatcher(List<String> input,String mode,boolean fold) {
        if(!Arrays.asList("substring","word","regex").contains(mode))throw new IllegalArgumentException("Mode inconnu");
        this.mode=mode;foldAccents=fold;rules=Collections.unmodifiableList(clean(input,fold,mode.equals("regex")));
        regex=new ArrayList<>();
        if(mode.equals("regex")){if(rules.size()>256)throw new IllegalArgumentException("Maximum 256 expressions régulières");
            for(String r:rules){if(r.length()>512)throw new IllegalArgumentException("Regex trop longue (512 caractères)");
                regex.add(Pattern.compile(r,Pattern.CASE_INSENSITIVE));}}
        aho=PhraseMatcher.of(mode.equals("regex")?Collections.emptyList():rules);
    }
    public List<String> rules(){return rules;}
    public boolean matches(String text){if(text==null||text.isEmpty()||rules.isEmpty())return false;
        // RE2/J guarantees linear matching and rejects backreferences/lookarounds.
        if(mode.equals("regex")){String s=Normalizer.normalize(text,Normalizer.Form.NFKC);for(Pattern p:regex)if(p.matcher(s).find())return true;return false;}
        String s=normalize(text,foldAccents);if(!aho.matches(s))return false;if(mode.equals("substring"))return true;
        // Aho rejects negative texts in O(n); inspect boundaries only for candidate hits.
        for(String r:rules){for(int from=0,at;(at=s.indexOf(r,from))>=0;from=at+1){int end=at+r.length();
            if((at==0||!word(s.codePointBefore(at)))&&(end==s.length()||!word(s.codePointAt(end))))return true;}}
        return false;
    }
    private static boolean word(int cp){return Character.isLetterOrDigit(cp)||cp=='_'||Character.getType(cp)==Character.NON_SPACING_MARK;}
}
