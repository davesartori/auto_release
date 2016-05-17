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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class GradleReleaseAction extends Builder implements Action {

  private AbstractProject project;

  @DataBoundConstructor
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
    if (project.scheduleBuild(0, new ReleaseCause(), this)) {
      resp.sendRedirect(req.getContextPath() + '/' + project.getUrl());
    } else {
      resp.sendRedirect(req.getContextPath() + '/' + project.getUrl() + '/' + getUrlName() + "/failed");
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
