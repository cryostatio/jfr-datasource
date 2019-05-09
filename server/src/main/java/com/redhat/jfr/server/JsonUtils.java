package com.redhat.jfr.server;

public class JsonUtils {
  public static final String ARRAY_START = "[";
  public static final String ARRAY_END = "]";

  public static String buildArray(String contents) {
    return ARRAY_START + contents + ARRAY_END;
  }

}