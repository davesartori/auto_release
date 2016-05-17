package com.nab.jenkins.plugins.release;

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotationDescriptor;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;

public class GradleOutcomeNote extends ConsoleNote {
  public GradleOutcomeNote() {
  }

  @Override
  public ConsoleAnnotator annotate(Object context, MarkupText text,
      int charPos) {
    if (text.getText().contains("FAIL"))
      text.addMarkup(0, text.length(),
          "<span class=gradle-outcome-failure>", "</span>");
    if (text.getText().contains("SUCCESS"))
      text.addMarkup(0, text.length(),
          "<span class=gradle-outcome-success>", "</span>");
    return null;
  }

  @Extension
  public static final class DescriptorImpl extends
      ConsoleAnnotationDescriptor {
    public String getDisplayName() {
      return "Gradle build outcome";
    }
  }
}
