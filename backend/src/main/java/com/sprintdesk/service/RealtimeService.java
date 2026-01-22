package com.sprintdesk.service;

import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeService {
  private final SimpMessagingTemplate messagingTemplate;

  public RealtimeService(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Publish a workspace-level event to all connected clients.
   */
  public void publishWorkspaceEvent(String workspaceId, String type, Object payload) {
    messagingTemplate.convertAndSend(
        "/topic/workspaces/" + workspaceId + "/events",
        Map.of("type", type, "payload", payload));
  }

  /**
   * Publish a user notification event to a specific user channel.
   */
  public void publishUserEvent(String userId, String type, Object payload) {
    messagingTemplate.convertAndSend(
        "/topic/users/" + userId + "/events",
        Map.of("type", type, "payload", payload));
  }
}