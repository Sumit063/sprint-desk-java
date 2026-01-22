package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommentResponse(
    @JsonProperty("_id") String id,
    String body,
    UserSummary userId,
    String createdAt) {}