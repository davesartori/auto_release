package com.nab.jenkins.plugins.release;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class GradleReleaseAction extends Builder implements Action {

  private AbstractProject project;

  public GradleReleaseAction(AbstractProject project) {
    this.project = project;
  }

  @Override public String getIconFileName() {
    if (GradleReleaseBuildWrapper.hasReleasePermission(project)) {
      return "installer.gif"; //$NON-NLS-1$
    }
    // by returning null the link will not be shown.
    return null;
  }

  @Override public String getDisplayName() {
    return "Build release";
  }

  @Override public String getUrlName() {
    return "gradlerelease";
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
    if (project.scheduleBuild(0, new ReleaseCause(), null, null)) {
      resp.sendRedirect(req.getContextPath() + '/' + project.getUrl());
    } else {
      resp.sendRedirect(req.getContextPath() + '/' + project.getUrl() + '/' + getUrlName() + "/failed");
    }
  }

  private boolean runGradle(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException {
    GradleLogger gradleLogger = new GradleLogger(listener);
    EnvVars env = build.getEnvironment(listener);

    // args
    ArgumentListBuilder args = new ArgumentListBuilder();

    //We are using the wrapper and don't care about the installed gradle versions
    String execName = (launcher.isUnix())
        ? GradleInstallation.UNIX_GRADLE_WRAPPER_COMMAND
        : GradleInstallation.WINDOWS_GRADLE_WRAPPER_COMMAND;
    FilePath normalizedRootBuildScriptDir = new FilePath(build.getModuleRoot(), "build.gradle");
    FilePath gradleWrapperFile = new FilePath(normalizedRootBuildScriptDir, execName);
    gradleWrapperFile.chmod(0744);
    args.add(gradleWrapperFile.getRemote());

    FilePath rootLauncher;
    rootLauncher = normalizedRootBuildScriptDir;
    if (rootLauncher == null) {
      rootLauncher = build.getProject().getSomeWorkspace();
    }

    try {
      GradleConsoleAnnotator gca = new GradleConsoleAnnotator(
          listener.getLogger(), build.getCharset());
      int r;
      try {
        r = launcher.launch().cmds(args).envs(env).stdout(gca)
            .pwd(rootLauncher).join();
      } finally {
        gca.forceEol();
      }
      boolean success = r == 0;
      // if the build is successful then set it as success otherwise as a failure.
      build.setResult(Result.SUCCESS);
      if (!success) {
        build.setResult(Result.FAILURE);
      }
      return success;
    } catch (IOException e) {
      Util.displayIOException(e, listener);
      e.printStackTrace(listener.fatalError("command execution failed"));
      build.setResult(Result.FAILURE);
      return false;
    }

  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    @CopyOnWrite
    private volatile GradleInstallation[] installations = new GradleInstallation[0];

    public DescriptorImpl() {
      load();
    }

    protected DescriptorImpl(Class<? extends GradleReleaseAction> clazz) {
      super(clazz);
    }

    /**
     * Obtains the {@link GradleInstallation.DescriptorImpl} instance.
     */
    public GradleInstallation.DescriptorImpl getToolDescriptor() {
      return ToolInstallation.all().get(GradleInstallation.DescriptorImpl.class);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    protected void convert(Map<String, Object> oldPropertyBag) {
      if (oldPropertyBag.containsKey("installations")) {
        installations = (GradleInstallation[]) oldPropertyBag.get("installations");
      }
    }

    @Override
    public String getHelpFile() {
      return "/plugin/gradle/help.html";
    }

    @Override
    public String getDisplayName() {
      return Messages.step_displayName();
    }

    public GradleInstallation[] getInstallations() {
      return installations;
    }

    public void setInstallations(GradleInstallation... installations) {
      this.installations = installations;
      save();
    }
  }
}
