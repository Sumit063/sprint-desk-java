package com.sprintdesk.controller;

import com.sprintdesk.service.DemoService;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo")
@Profile("local")
public class DemoController {
  private final DemoService demoService;

  public DemoController(DemoService demoService) {
    this.demoService = demoService;
  }

  @PostMapping("/reset")
  public ResponseEntity<Map<String, Object>> resetDemo() {
    if (!demoService.isEnabled()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo mode disabled");
    }
    demoService.resetDemoData();
    return ResponseEntity.ok(Map.of("ok", true));
  }
}