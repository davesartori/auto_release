package com.nab.jenkins.plugins.release;

import hudson.model.TaskListener;

import java.io.Serializable;

public class GradleLogger implements Serializable {

  private TaskListener listener;

  public GradleLogger(TaskListener listener) {
    this.listener = listener;
  }

  public void info(String message) {
    listener.getLogger().println("[Gradle] - " + message);
  }

  public void error(String message) {
    listener.getLogger().println("[Gradle] - [ERROR] " + message);
  }

}
