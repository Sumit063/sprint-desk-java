package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ActivityResponse(
    @JsonProperty("_id") String id,
    String action,
    String createdAt,
    UserSummary actorId,
    IssueActivitySummary issueId,
    Map<String, Object> meta) {}