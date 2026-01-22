package com.sprintdesk.service;

import com.sprintdesk.dto.NotificationResponse;
import com.sprintdesk.model.Notification;
import com.sprintdesk.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final RealtimeService realtimeService;

  public NotificationService(
      NotificationRepository notificationRepository, RealtimeService realtimeService) {
    this.notificationRepository = notificationRepository;
    this.realtimeService = realtimeService;
  }

  public Notification createNotification(
      UUID userId, UUID workspaceId, UUID issueId, String type, String message) {
    Notification notification = new Notification();
    notification.setUserId(userId);
    notification.setWorkspaceId(workspaceId);
    notification.setIssueId(issueId);
    notification.setType(type);
    notification.setMessage(message);
    Notification saved = notificationRepository.save(notification);

    realtimeService.publishUserEvent(
        userId.toString(),
        "notification_created",
        java.util.Map.of(
            "message", saved.getMessage(), "notificationId", saved.getId().toString()));

    return saved;
  }

  public List<NotificationResponse> listNotifications(UUID userId, boolean unreadOnly) {
    List<Notification> notifications =
        unreadOnly
            ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
            : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    return notifications.stream().map(this::toResponse).toList();
  }

  public NotificationResponse markRead(UUID userId, UUID notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Notification not found"));
    if (notification.getReadAt() == null) {
      notification.setReadAt(Instant.now());
      notificationRepository.save(notification);
    }
    return toResponse(notification);
  }

  public long markAllRead(UUID userId) {
    List<Notification> notifications =
        notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
    Instant now = Instant.now();
    for (Notification notification : notifications) {
      notification.setReadAt(now);
    }
    notificationRepository.saveAll(notifications);
    return notifications.size();
  }

  private NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId().toString(),
        notification.getMessage(),
        notification.getType(),
        notification.getReadAt() == null ? null : notification.getReadAt().toString(),
        notification.getCreatedAt().toString(),
        notification.getIssueId() == null ? null : notification.getIssueId().toString());
  }
}