// CHECKSTYLE:OFF

package com.nab.jenkins.plugins.release;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

@SuppressWarnings({
    "",
    "PMD"
})
public class Messages {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    /**
     * Gradle
     * 
     */
    public static String installer_displayName() {
        return holder.format("installer.displayName");
    }

    /**
     * Gradle
     * 
     */
    public static Localizable _installer_displayName() {
        return new Localizable(holder, "installer.displayName");
    }

    /**
     * gradle_auto_release
     * 
     */
    public static String PermissionGroup_Title() {
        return holder.format("PermissionGroup.Title");
    }

    /**
     * gradle_auto_release
     * 
     */
    public static Localizable _PermissionGroup_Title() {
        return new Localizable(holder, "PermissionGroup.Title");
    }

    /**
     * Invoke Gradle script
     * 
     */
    public static String step_displayName() {
        return holder.format("step.displayName");
    }

    /**
     * Invoke Gradle script
     * 
     */
    public static Localizable _step_displayName() {
        return new Localizable(holder, "step.displayName");
    }

    /**
     * Started by user {0}
     * 
     */
    public static String ReleaseCause_ShortDescription(Object arg1) {
        return holder.format("ReleaseCause.ShortDescription", arg1);
    }

    /**
     * Started by user {0}
     * 
     */
    public static Localizable _ReleaseCause_ShortDescription(Object arg1) {
        return new Localizable(holder, "ReleaseCause.ShortDescription", arg1);
    }

    /**
     * Perform Gradle Release
     * 
     */
    public static String ReleaseAction_perform_release_name() {
        return holder.format("ReleaseAction.perform.release.name");
    }

    /**
     * Perform Gradle Release
     * 
     */
    public static Localizable _ReleaseAction_perform_release_name() {
        return new Localizable(holder, "ReleaseAction.perform.release.name");
    }

    /**
     * Auto release build
     * 
     */
    public static String Wrapper_DisplayName() {
        return holder.format("Wrapper.DisplayName");
    }

    /**
     * Auto release build
     * 
     */
    public static Localizable _Wrapper_DisplayName() {
        return new Localizable(holder, "Wrapper.DisplayName");
    }

    /**
     * This permission allows users to create releases using the gradle_auto_release plugin.
     * 
     */
    public static String CreateReleasePermission_Description() {
        return holder.format("CreateReleasePermission.Description");
    }

    /**
     * This permission allows users to create releases using the gradle_auto_release plugin.
     * 
     */
    public static Localizable _CreateReleasePermission_Description() {
        return new Localizable(holder, "CreateReleasePermission.Description");
    }

}
