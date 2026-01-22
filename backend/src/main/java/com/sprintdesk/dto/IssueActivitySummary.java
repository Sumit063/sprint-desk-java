package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueActivitySummary(
    @JsonProperty("_id") String id,
    String ticketId,
    String title,
    String status) {}