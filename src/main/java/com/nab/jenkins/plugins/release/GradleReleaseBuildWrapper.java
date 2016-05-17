package com.nab.jenkins.plugins.release;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import net.sf.json.JSONObject;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

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
    return null;
  }

  @Override public Environment setUp(Build build, Launcher launcher, BuildListener listener)
      throws IOException, InterruptedException {
    return null;
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
