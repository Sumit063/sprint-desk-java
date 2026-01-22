package com.sprintdesk.dto;

import java.util.List;

public record MemberOverviewRecent(
    List<IssueSummary> issuesCreated,
    List<IssueSummary> issuesAssigned,
    List<ArticleSummary> kbWorkedOn) {}