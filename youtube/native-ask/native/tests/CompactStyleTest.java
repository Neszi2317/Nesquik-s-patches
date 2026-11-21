package app.neszi.asknative;
import java.util.*;
public class CompactStyleTest {
 public static void main(String[]args){
  byte[] icon=AskNative.field(2,AskNative.utf8("yt_fill_spark"));
  byte[] tap=AskNative.field(4,AskNative.field(169495254,AskNative.field(AskNative.MARKER,AskNative.utf8(AskNative.PREFIX+"abcdefghijk"))));
  byte[] own=NativeProtoTest.cat(icon,tap,new byte[]{56,9,88,3,104,2});
  byte[] style=new byte[]{56,2,88,1,104,1};
  byte[] actual=AskNative.compactStyle(own,style);
  NativeProtoTest.check(Arrays.equals(actual,NativeProtoTest.cat(icon,tap,style)),"original icon and current command preserved; compact styles replace old pill styles");
  NativeProtoTest.check(Arrays.equals(AskNative.compactStyle(actual,style),actual),"idempotent");
  NativeProtoTest.check(Arrays.equals(AskNative.compactStyle(own,new byte[0]),NativeProtoTest.cat(icon,tap)),"native style absence also respected");
  System.out.println("PASS compact style isolation; old style input unchanged; icon and click bytes preserved (JVM only)");
 }
}
