package com.nab.jenkins.plugins.release;

import hudson.model.Cause.UserIdCause;

public class ReleaseCause extends UserIdCause {

  // Kept for backwards compatibility with saved builds from older versions of the plugin.
  // Should be removed in the future!
  @Deprecated
  private String authenticationName;

  @Override
  public String getUserName() {
    if (this.authenticationName != null) {
      return authenticationName;
    } else {
      return super.getUserName();
    }
  }

  @Override
  public String getShortDescription() {
    return Messages.ReleaseCause_ShortDescription(getUserName());
  }
}
