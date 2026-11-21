package com.reddit.domain.model;
public class Link {
 private final boolean adult;
 public Link(boolean adult){this.adult=adult;}
 public boolean getOver18(){return adult;}
}
