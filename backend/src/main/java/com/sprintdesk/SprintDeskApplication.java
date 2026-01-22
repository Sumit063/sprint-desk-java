package com.sprintdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SprintDeskApplication {
  public static void main(String[] args) {
    SpringApplication.run(SprintDeskApplication.class, args);
  }
}
