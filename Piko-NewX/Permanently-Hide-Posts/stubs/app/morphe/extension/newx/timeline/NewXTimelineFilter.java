package app.morphe.extension.newx.timeline;
public class NewXTimelineFilter {
public static native boolean isTimelinePost(Object o);
public static native boolean isTimelineModule(Object o);
public static native boolean isTimelineModuleItem(Object o);
public static native boolean isModuleItemDispensable(Object o);
public static native Object getModuleItem(Object o);
public static native Object copyModuleItem(Object o, Object child, boolean dispensable);
public static native java.util.List<?> getModuleInnerContent(Object o);
public static native Object getModuleHeader(Object o);
public static native Object getModuleFooter(Object o);
public static native Object getModuleDisplayType(Object o);
public static native long getModuleSortIndex(Object o);
public static native String getModuleEntryId(Object o);
public static native Object getModuleClientEventInfo(Object o);
public static native Object getPostId(Object o);
public static native boolean isVerticalConversation(Object o);
public static native java.util.List<?> getVerticalConversationPostIds(Object o);
public static native Object copyVerticalConversation(Object o, java.util.List<?> ids);
public static native Object copyModule(Object o, java.util.List<?> children, Object header, Object footer,
                                    Object display, long sort, String id, Object event);
public static native Object immutableList(java.util.List<Object> list);
}
