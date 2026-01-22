package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationResponse(
    @JsonProperty("_id") String id,
    String message,
    String type,
    String readAt,
    String createdAt,
    String issueId) {}