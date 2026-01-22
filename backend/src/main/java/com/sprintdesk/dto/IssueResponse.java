package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record IssueResponse(
    @JsonProperty("_id") String id,
    String ticketId,
    String title,
    String description,
    String status,
    String priority,
    List<String> labels,
    UserSummary assigneeId,
    UserSummary createdBy,
    String createdAt,
    String updatedAt) {}