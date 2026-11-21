package app.morphe.extension.newx.anonymous;
/** Patch adds the resolved X interface and typed forwarding methods. No original field writes. */
public final class PresentationUser {
 public final Object original;
 public PresentationUser(Object original){if(original==null)throw new NullPointerException("original");this.original=original;}
 @Override public boolean equals(Object o){return o instanceof PresentationUser && original.equals(((PresentationUser)o).original);}
 @Override public int hashCode(){return original.hashCode();}
}
