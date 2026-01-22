package com.sprintdesk.controller;

import com.sprintdesk.dto.ActivityResponse;
import com.sprintdesk.security.SecurityUtils;
import com.sprintdesk.service.ActivityService;
import com.sprintdesk.service.WorkspaceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/activities")
public class ActivityController {
  private final ActivityService activityService;
  private final WorkspaceService workspaceService;

  public ActivityController(ActivityService activityService, WorkspaceService workspaceService) {
    this.activityService = activityService;
    this.workspaceService = workspaceService;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> listActivities(
      @PathVariable UUID workspaceId, @RequestParam(defaultValue = "30") int limit) {
    UUID userId = requireUser();
    workspaceService.requireMember(workspaceId, userId);
    int safeLimit = Math.min(Math.max(limit, 1), 50);
    List<ActivityResponse> activities = activityService.listActivities(workspaceId, safeLimit);
    return ResponseEntity.ok(Map.of("activities", activities));
  }

  private UUID requireUser() {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return userId;
  }
}