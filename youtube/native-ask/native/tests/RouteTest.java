package app.neszi.asknative;
import app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch;
import java.util.*;
public class RouteTest {
 public static class Message implements AskFlyoutMenuPatch.MenuItemAccess {
  byte[] b; public Message(byte[] b){this.b=b;}
  public byte[] patch_toByteArray(){return b;}
  public Message patch_parseSameType(byte[] b){return new Message(b);}
 }
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  Message regular=new Message(AskNative.field(123,AskNative.utf8("unchanged")));
  final int[] calls={0};AskFlyoutMenuPatch.CommandExecutorAccess executor=b->calls[0]++;
  check(AskNative.route(regular,executor)==regular,"ordinary command untouched");
  String id="abcdefghijk";Message own=new Message(AskNative.field(AskNative.MARKER,AskNative.utf8(AskNative.PREFIX+id)));
  Message noop=(Message)AskNative.route(own,executor);
  check(noop.b.length==0,"own command consumed");check(id.equals(AskFlyoutMenuPatch.opened),"current video used");
  check(AskFlyoutMenuPatch.executor==executor&&calls[0]==1,"official executor reused");
  check(!AskFlyoutMenuPatch.autoSummary(),"direct opening has no auto-summary");
  AskFlyoutMenuPatch.showAskPanel(id);check(AskFlyoutMenuPatch.autoSummary(),"ordinary flyout keeps auto-summary, even same video");
  Message invalid=new Message(AskNative.field(AskNative.MARKER,AskNative.utf8(AskNative.PREFIX+"bad")));
  check(((Message)AskNative.route(invalid,executor)).b.length==0&&calls[0]==2,"invalid marker consumed without opening");
  System.out.println("PASS: official bridge reflection, current-video route, noop command, direct summary suppression, normal flyout preserved, invalid marker rejected (JVM stubs; not Android execution)");
 }
}
