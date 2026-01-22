package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArticleSummary(
    @JsonProperty("_id") String id,
    String kbId,
    String title,
    String updatedAt) {}