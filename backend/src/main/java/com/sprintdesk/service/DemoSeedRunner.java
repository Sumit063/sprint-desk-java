package com.sprintdesk.service;

import com.sprintdesk.config.DemoProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class DemoSeedRunner implements CommandLineRunner {
  private final DemoService demoService;
  private final DemoProperties demoProperties;

  public DemoSeedRunner(DemoService demoService, DemoProperties demoProperties) {
    this.demoService = demoService;
    this.demoProperties = demoProperties;
  }

  @Override
  public void run(String... args) {
    if (!demoProperties.isEnabled() || !demoProperties.isSeedOnStart()) {
      return;
    }
    demoService.resetDemoData();
  }
}