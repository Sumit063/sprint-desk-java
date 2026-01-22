package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueSummary(
    @JsonProperty("_id") String id,
    String ticketId,
    String title,
    String status,
    String priority) {}