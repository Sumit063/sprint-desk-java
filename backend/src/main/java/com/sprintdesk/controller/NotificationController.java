package com.sprintdesk.controller;

import com.sprintdesk.dto.NotificationResponse;
import com.sprintdesk.security.SecurityUtils;
import com.sprintdesk.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> listNotifications(
      @RequestParam(required = false) String unread) {
    UUID userId = requireUser();
    boolean unreadOnly = "true".equalsIgnoreCase(unread);
    List<NotificationResponse> notifications =
        notificationService.listNotifications(userId, unreadOnly);
    return ResponseEntity.ok(Map.of("notifications", notifications));
  }

  @PatchMapping("/read-all")
  public ResponseEntity<Map<String, Object>> markAllRead() {
    UUID userId = requireUser();
    long updated = notificationService.markAllRead(userId);
    return ResponseEntity.ok(Map.of("updated", updated));
  }

  @PatchMapping("/{notificationId}/read")
  public ResponseEntity<Map<String, Object>> markRead(@PathVariable UUID notificationId) {
    UUID userId = requireUser();
    NotificationResponse notification = notificationService.markRead(userId, notificationId);
    return ResponseEntity.ok(Map.of("notification", notification));
  }

  private UUID requireUser() {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return userId;
  }
}