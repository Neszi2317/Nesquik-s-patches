package com.reddit.domain.model;
public final class Comment {
 private final String body,parent,id;
 public Comment(String id,String parent,String body){this.id=id;this.parent=parent;this.body=body;}
 public String getBody(){return body;}public String getParentKindWithId(){return parent;}public String getKindWithId(){return id;}
 public String unrelatedMetadata="exampleblockedword";
}
