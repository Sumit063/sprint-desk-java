package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserSummary(
    @JsonProperty("_id") String id,
    String name,
    String email,
    String avatarUrl) {}