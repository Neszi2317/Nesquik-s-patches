package app.morphe.extension.newx.postfilter;
import java.util.*;
import com.x.models.*;
import fr.nesquik.hidden.Menu;
import fr.nesquik.hidden.Projection;

public class FilterCombination {
    static AdvancedModels.Post row(String value,String text){
        AdvancedModels.Post post=new AdvancedModels.Post(value);
        post.result.canonical.text=text;
        s1 data=new s1();data.b=new v0();data.b.a=new h6(){public String toString(){return value;}};
        post.a=data;
        return post;
    }
    public static void main(String[] args)throws Exception{
        // Seeded by the host persistence test in the preceding JVM process.
        org.json.JSONObject config=new org.json.JSONObject().put("keywords",new org.json.JSONArray(List.of("forbidden")));
        var current=AdvancedConfig.class.getDeclaredField("current");
        current.setAccessible(true);current.set(null,new AdvancedConfig(config));
        var manual=row("42","ordinary");
        var keyword=row("43","forbidden");
        var survivor=row("44","ordinary");
        var rows=List.of(manual,keyword,survivor);
        if(!Projection.render(AdvancedFilter.filter(rows)).equals(List.of(survivor)))
            throw new AssertionError("Keyword then ID");
        if(!AdvancedFilter.filter(Projection.render(rows)).equals(List.of(survivor)))
            throw new AssertionError("ID then keyword");
        System.out.println("PASS production AdvancedFilter and Projection in both orders with one survivor");
    }
}
