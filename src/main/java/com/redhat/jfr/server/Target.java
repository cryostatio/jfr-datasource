package com.redhat.jfr.server;

public class Target {
  private final String target;
  private final String type;

  public Target(String target, String type) {
    this.target = target;
    this.type = type;
  }
  
  public String getTarget() {
    return this.target;
  }

  public String getType() {
    return this.type;
  }
}