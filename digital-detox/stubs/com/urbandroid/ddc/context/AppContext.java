package com.urbandroid.ddc.context;

public class AppContext {
  public static void initAll(android.content.Context c) {}

  public static Settings settings() {
    throw new AssertionError();
  }
}
