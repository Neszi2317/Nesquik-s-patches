package fr.nesquik.hidden;
import com.x.models.*;
import com.x.models.timelines.items.k1;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import app.morphe.extension.newx.timeline.NewXTimelineFilter.Module;
import static app.morphe.extension.newx.timeline.NewXTimelineFilter.*;

public class HostTest {
    static int checks;
    static void check(boolean value,String name){
        if(!value)throw new AssertionError(name);
        checks++;System.out.println("PASS "+name);
    }
    static Set<String> read(String s)throws IOException{return Ids.read(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));}
    static k1 post(long n){
        k1 p=new k1();s1 data=new s1();data.b=new v0();data.b.a=new Id(n);p.a=data;return p;
    }
    static class Id extends h6 {
        final long n;Id(long n){this.n=n;}
        public String toString(){return Long.toString(n);}
    }
    static class Group {final List<?> actions;Group(List<?> list){actions=list;} }
    enum Action {None,Bookmark}
    static class Presenter {final k1 post;Presenter(k1 post){this.post=post;}}
    public static void main(String[] args)throws Exception{
        String max="9223372036854775807", head=Ids.HEADER+"\n";
        check(Ids.valid(max) && !Ids.valid("9223372036854775808"),"Exact positive 64-bit IDs");
        check(!Ids.valid("01")&&!Ids.valid("0")&&!Ids.valid("١")&&!Ids.valid("-7"),"Malformed ID rejected");
        check(read(head+"42\n42\r\n"+max).equals(Set.of("42",max)),"CRLF, duplicate and 64-bit import");
        for(String bad:List.of("v9\n42", head+"42\nwrong",head+"9".repeat(3145729))){
            try{read(bad);throw new AssertionError();}catch(IOException expected){check(true,"Invalid or oversized import rejected atomically");}
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        Ids.write(Set.of("42",max),out);
        check(Ids.read(new ByteArrayInputStream(out.toByteArray())).equals(Set.of("42",max)),"Export and reimport");
        k1 a=post(42),b=post(43),quote=post(44),repost=post(45);
        ((s1)quote.a).b.B=new Id(42);
        ((s1)repost.a).d=new q8();((s1)repost.a).d.a=((s1)a.a).b;
        List<Object> source=List.of(a,b,quote,repost,"cursor");
        check(Projection.render(source)==source,"Initial list identity");
        Menu.action=Action.None;
        Group group=new Group(List.of(Action.Bookmark));
        List<?> groups=List.of(group),added=Menu.add(groups);
        check(added.size()==2 && added.get(0)==group && Menu.add(added)==added,"Native groups preserved and action deduplicated");
        check(Menu.usesIcon(Action.None) && !Menu.usesIcon(Action.Bookmark) && !Menu.usesIcon(null),"Icon only for the selected menu token");
        check(Menu.label(Action.None,"x").equals("Masquer définitivement ce post") && Menu.label(Action.Bookmark,"Save").equals("Save"),"Targeted label");
        check(!Menu.handle(new Presenter(a),Action.Bookmark),"Native actions pass through");
        check(Menu.handle(new Presenter(a),Action.None),"Selected post click consumed");
        Store.IO.submit(()->{}).get();
        check(Store.ids().equals(Set.of("42")),"Only selected ID persisted");
        check(Projection.render(source).equals(List.of(b,"cursor")),"Direct post, quote and repost filtered; unrelated row and cursor preserved");
        Object child=new Wrapper(a);
        Module module=new Module(List.of(child,new Wrapper(b)));
        List<?> nested=(List<?>)Projection.render(List.of(module));
        check(((Module)nested.get(0)).children.size()==1 && ((Wrapper)((Module)nested.get(0)).children.get(0)).item==b,"Module compacted");
        check(((List<?>)Projection.render(List.of(new Module(List.of(child))))).isEmpty(),"Empty module removed");
        Store.enabled(false);
        check(Projection.render(source)==source,"Disable restores original list immediately on next render");
        Store.enabled(true);
        check(((List<?>)Projection.render(new ArrayList<>(source))).size()==2,"Re-enable and refreshed list");
        Store.edit(Set.of("43"),null,false);
        check(Projection.render(source).equals(List.of("cursor")),"Multiple hidden posts");
        Store.edit(Set.of(),"42",false);
        check(((List<?>)Projection.render(source)).contains(a),"Remove one ID");
        Store.edit(Set.of(),null,true);
        check(Projection.render(source)==source,"Clear restores source");
        Store.IO.shutdown();
        System.out.println(checks+" checks passed. Host fixtures only.");
    }
}
