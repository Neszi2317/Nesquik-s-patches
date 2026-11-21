package app.neszi.asknative;
import java.util.*;
import java.io.*;
public class NativeProtoTest {
 static byte[] f(int n,byte[] b){return AskNative.field(n,b);}
 static byte[] s(String s){return AskNative.utf8(s);}
 static byte[] cat(byte[]...bs){ByteArrayOutputStream o=new ByteArrayOutputStream();for(byte[] b:bs)AskNative.write(o,b);return o.toByteArray();}
 static byte[] button(String icon,String title){return f(10,f(AskNative.SECONDARY,cat(f(2,s(icon)),f(3,s(title)),f(4,f(1234,s("ORIGINAL_SHARE_COMMAND"))),new byte[]{56,2,88,1,104,2},f(999,s("opaque")))));}
 static byte[] root(byte[] data,boolean compact,String video,String identifier){
  byte[] model=compact?f(413471385,f(1,f(2185,data))):f(389155117,f(17,data));
  byte[] element=cat(f(1,f(168777401,f(5,model))),f(2,f(183314536,f(1,s(identifier)))));
  return f(1,f(49399797,f(1,f(216561405,cat(f(1,f(153515154,f(172660663,element))),f(2,s(video)))))));
 }
 static byte[] extract(byte[] root,boolean compact){
  byte[] m=AskNative.first(AskNative.first(AskNative.first(AskNative.first(AskNative.first(AskNative.first(AskNative.first(AskNative.first(AskNative.first(AskNative.first(root,1),49399797),1),216561405),1),153515154),172660663),1),168777401),5);
  return compact?AskNative.first(AskNative.first(AskNative.first(m,413471385),1),2185):AskNative.first(AskNative.first(m,389155117),17);
 }
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  for(boolean compact:new boolean[]{false,true}){
   AskNative.genuineVisual=null;
   byte[] share=button("yt_outline_share","Partager");byte[] save=f(10,f(484598023,f(2,s("save_entity"))));
   byte[] data=cat(f(1,share),f(1,save),f(99,s("unknown data retained")));
   byte[] input=root(data,compact,"abcdefghijk",compact?"compactify_video_action_bar.e":"video_action_bar.e");
   byte[] changed=AskNative.transformBytes(input);
   byte[] outer=f(7,f(51779735,input));
   byte[] rewritten=AskNative.transformNextBytes(outer);
   check(Arrays.equals(changed,AskNative.first(AskNative.first(rewritten,7),51779735)),"decoded root replacement contains modified bar");
   List<AskNative.Field> fs=AskNative.fields(extract(changed,compact));
   check(fs.size()==4,"one added");check(Arrays.equals(fs.get(0).v,share),"Share byte-for-byte unchanged");check(Arrays.equals(fs.get(2).v,save),"Save unchanged");
   byte[] ask=AskNative.first(AskNative.first(fs.get(1).v,10),AskNative.SECONDARY);
   check(AskNative.first(ask,2).length==0,"no fabricated icon before capture");check(AskNative.text(AskNative.first(ask,3)).equals("Gemini"),"honest capture bootstrap label");
   byte[] cmd=AskNative.first(AskNative.first(ask,4),169495254);
   check(AskNative.text(AskNative.first(cmd,AskNative.MARKER)).equals(AskNative.PREFIX+"abcdefghijk"),"click targets response video");
   check(!new String(ask,java.nio.charset.StandardCharsets.ISO_8859_1).contains("ORIGINAL_SHARE_COMMAND"),"Share command never cloned");
   check(Arrays.equals(changed,AskNative.transformBytes(changed)),"idempotent");
   byte[] nativeAsk=root(cat(f(1,share),f(1,button("yt_fill_experimental_spark","Ask"))),compact,"abcdefghijk","video_action_bar.e");
   check(Arrays.equals(extract(nativeAsk,compact),extract(AskNative.transformBytes(nativeAsk),compact)),"existing native Ask actions preserved (compact outer video binding allowed)");
   byte[] badId=root(data,compact,"invalid","video_action_bar.e");check(badId==AskNative.transformBytes(badId),"invalid id unchanged");
   byte[] other=root(data,compact,"abcdefghijk","unrelated.e");check(other==AskNative.transformBytes(other),"unrelated renderer unchanged");
   byte[] missingShare=root(f(1,save),compact,"abcdefghijk","video_action_bar.e");check(missingShare!=AskNative.transformBytes(missingShare),"missing Share still creates native Ask");
  }
  boolean rejected=false;try{AskNative.fields(new byte[]{10,100,1});}catch(IllegalArgumentException e){rejected=true;}check(rejected,"truncated protobuf rejected");
  System.out.println("PASS: expanded+compact insertion, order, style, video command, Share+Save preservation, unknown fields, no duplicate, unsupported/malformed input");
 }
}
