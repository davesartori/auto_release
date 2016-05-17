package com.nab.jenkins.plugins.release;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Result;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@Slf4j
public class GradleReleaseBuildWrapper extends BuildWrapper {

  @DataBoundConstructor
  public GradleReleaseBuildWrapper() {
    super();
  }

  public static boolean hasReleasePermission(AbstractProject job) {
    return job.hasPermission(DescriptorImpl.CREATE_RELEASE);
  }

  @Override
  public Action getProjectAction(@SuppressWarnings("rawtypes") AbstractProject job) {
    return new GradleReleaseAction(job);
  }

  @Override public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException {
    log.info("Run release build");
    runGradle(build, launcher, listener);
    return null;
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
    FilePath normalizedRootBuildScriptDir = new FilePath(build.getModuleRoot(), "");
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

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension
  public static class DescriptorImpl extends BuildWrapperDescriptor {

    public static final Permission CREATE_RELEASE;

    static {
      Permission tmpPerm = null;
      try {
        // Jenkins changed the security model in a non backward compatible way :-(
        // JENKINS-10661
        Class<?> permissionScopeClass = Class.forName("hudson.security.PermissionScope");
        Object psArr = Array.newInstance(permissionScopeClass, 2);
        Field f;
        f = permissionScopeClass.getDeclaredField("JENKINS");
        Array.set(psArr, 0, f.get(null));
        f = permissionScopeClass.getDeclaredField("ITEM");
        Array.set(psArr, 1, f.get(null));

        Constructor<Permission> ctor = Permission.class.getConstructor(PermissionGroup.class,
            String.class,
            Localizable.class,
            Permission.class,
//						boolean.class,
            permissionScopeClass);
        //permissionScopes.getClass());
        tmpPerm = ctor.newInstance(Item.PERMISSIONS,
            "Release",
            Messages._CreateReleasePermission_Description(),
            Hudson.ADMINISTER,
//				                            true,
            f.get(null));
        LoggerFactory.getLogger(GradleReleaseBuildWrapper.class)
            .info("Using new style Permission with PermissionScope");

      }
      // all these exceptions are Jenkins < 1.421 or Hudson
      // wouldn't multicatch be nice!
      catch (Exception ex) {
        LoggerFactory.getLogger(GradleReleaseBuildWrapper.class).warn("Exception: ", ex.getMessage());
      }
      if (tmpPerm == null) {
        LoggerFactory.getLogger(GradleReleaseBuildWrapper.class)
            .warn("Using Legacy Permission as new style permission with PermissionScope failed");
        tmpPerm = new Permission(Item.PERMISSIONS,
            "Release", //$NON-NLS-1$
            Messages._CreateReleasePermission_Description(),
            Hudson.ADMINISTER);
      }
      CREATE_RELEASE = tmpPerm;
    }

    public DescriptorImpl() {
      super(GradleReleaseBuildWrapper.class);
      load();
    }

    @Override public boolean isApplicable(AbstractProject<?, ?> item) {
      return true;
    }

    @Override
    public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
      save();
      return true; // indicate that everything is good so far
    }

    @Override
    public String getDisplayName() {
      return Messages.Wrapper_DisplayName();
    }
  }
}
