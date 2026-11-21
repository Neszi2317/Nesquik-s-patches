package app.morphe.extension.youtube.patches.ask;
import app.neszi.asknative.AskNative;
public class AskFlyoutMenuPatch {
 public interface MenuAccess { java.util.List<Object> patch_getRows(); boolean patch_dismissPopup(); default Object patch_createRow(MenuItemAccess item){return null;} }
 public interface MessageAccess {byte[] patch_toByteArray();MessageAccess patch_parseSameType(byte[] b);}
 public interface MenuItemAccess extends MessageAccess {}
 public interface MenuRowAccess { MenuItemAccess patch_getMenuItem(); }
 public interface CommandExecutorAccess {void patch_executeCommand(byte[] b);}
 private static final Object pendingLock=new Object();
 private static String pendingWelcomeToken,pendingSummarizeToken;
 private static long pendingSuggestionSelectionId=-1;
 public static String opened;
 public static CommandExecutorAccess executor;
 public static void setCommandExecutor(CommandExecutorAccess e){executor=e;}
 private static void openAskPanel(String id){showAskPanel(id);}
 public static void showAskPanel(String id){
  AskNative.beginShow(id);
  opened=id;pendingWelcomeToken="welcome";pendingSummarizeToken="summary";pendingSuggestionSelectionId=42;
  AskNative.beforeShow();executor.patch_executeCommand(new byte[]{1});
 }
 public static boolean autoSummary(){return pendingWelcomeToken!=null||pendingSummarizeToken!=null||pendingSuggestionSelectionId>=0;}
}
