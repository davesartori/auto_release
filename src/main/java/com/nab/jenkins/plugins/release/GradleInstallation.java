package com.nab.jenkins.plugins.release;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GradleInstallation extends ToolInstallation
    implements EnvironmentSpecific<GradleInstallation>, NodeSpecific<GradleInstallation>, Serializable {

  public static final String UNIX_GRADLE_COMMAND = "gradle";
  public static final String WINDOWS_GRADLE_COMMAND = "gradle.bat";
  public static final String UNIX_GRADLE_WRAPPER_COMMAND = "gradlew";
  public static final String WINDOWS_GRADLE_WRAPPER_COMMAND = "gradlew.bat";

  private final String gradleHome;

  @DataBoundConstructor
  public GradleInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
    super(name, launderHome(home), properties);
    this.gradleHome = super.getHome();
  }

  private static String launderHome(String home) {
    if (home.endsWith("/") || home.endsWith("\\")) {
      // see https://issues.apache.org/bugzilla/show_bug.cgi?id=26947
      // Ant doesn't like the trailing slash, especially on Windows
      return home.substring(0, home.length() - 1);
    } else {
      return home;
    }
  }

  @Override
  public String getHome() {
    if (gradleHome != null) {
      return gradleHome;
    }
    return super.getHome();
  }

  public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
    return launcher.getChannel().call(new Callable<String, IOException>() {
      @Override public String call() throws IOException {
        File exe = getExeFile();
        if (exe.exists()) {
          return exe.getPath();
        }
        return null;
      }

      @Override public void checkRoles(RoleChecker checker) throws SecurityException {
      }

    });
  }

  private File getExeFile() {
    String execName = (Functions.isWindows()) ? WINDOWS_GRADLE_COMMAND : UNIX_GRADLE_COMMAND;
    String antHome = Util.replaceMacro(gradleHome, EnvVars.masterEnvVars);
    return new File(antHome, "bin/" + execName);
  }

  public GradleInstallation forEnvironment(EnvVars environment) {
    return new GradleInstallation(getName(), environment.expand(gradleHome), getProperties().toList());
  }

  public GradleInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
    return new GradleInstallation(getName(), translateFor(node, log), getProperties().toList());
  }

  @Extension
  public static class DescriptorImpl extends ToolDescriptor<GradleInstallation> {

    public DescriptorImpl() {
    }

    @Override
    public String getDisplayName() {
      return Messages.installer_displayName();
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new GradleInstaller(null));
    }

    // for compatibility reasons, the persistence is done by GradleBuilder.DescriptorImpl

    @Override
    public GradleInstallation[] getInstallations() {
      return Hudson.getInstance().getDescriptorByType(GradleReleaseAction.DescriptorImpl.class).getInstallations();
    }

    @Override
    public void setInstallations(GradleInstallation... installations) {
      Hudson.getInstance().getDescriptorByType(GradleReleaseAction.DescriptorImpl.class)
          .setInstallations(installations);
    }

  }

}
