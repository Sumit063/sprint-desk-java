package com.sprintdesk.dto;

import java.util.List;

public record ArticleRequest(
    String title,
    String body,
    List<String> linkedIssueIds) {}