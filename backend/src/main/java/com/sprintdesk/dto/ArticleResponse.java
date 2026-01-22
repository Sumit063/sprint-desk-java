package com.sprintdesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ArticleResponse(
    @JsonProperty("_id") String id,
    String kbId,
    String title,
    String body,
    List<String> linkedIssueIds,
    String createdAt,
    String updatedAt,
    UserSummary createdBy,
    UserSummary updatedBy) {}