package com.nab.gradle.plugins.release;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AutoReleasePlugin implements Plugin<Project> {
  @Override public void apply(Project project) {
    project.getTasks().create("autoRelease", AutoReleaseTask.class);
  }
}
