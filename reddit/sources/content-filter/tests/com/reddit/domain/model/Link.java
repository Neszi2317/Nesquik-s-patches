package com.reddit.domain.model;
public final class Link {
 public String flair="",authorFlair="";
 public String getLinkFlairText(){return flair;}
 public String subreddit="normal";
 public String getSubreddit(){return subreddit;}
 private final String title,body,selftext,id;
 public Link(String id,String title,String body,String selftext){this.id=id;this.title=title;this.body=body;this.selftext=selftext;}
 public String getTitle(){return title;}public String getBody(){return body;}public String getSelftext(){return selftext;}public String getKindWithId(){return id;}
}
