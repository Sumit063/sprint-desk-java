package com.sprintdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public class DemoProperties {
  private boolean enabled = true;
  private boolean seedOnStart = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isSeedOnStart() {
    return seedOnStart;
  }

  public void setSeedOnStart(boolean seedOnStart) {
    this.seedOnStart = seedOnStart;
  }
}