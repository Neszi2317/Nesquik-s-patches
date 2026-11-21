package fr.nesquik.hidden;
import java.util.Set;
public class Restart {
    public static void main(String[] args){
        if(args[0].equals("seed")){
            Store.edit(Set.of("42"),null,false);
            Store.enabled(false);
            return;
        }
        if(!Store.ids().equals(Set.of("42"))||Store.enabled())throw new AssertionError("preferences round trip");
        Store.enabled(true);
        System.out.println("PASS production Store reload after JVM restart using disk-backed preference fixture");
    }
}
