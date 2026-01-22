package com.sprintdesk.dto;

public record MemberOverviewStats(
    int issuesCreated,
    int issuesAssigned,
    int kbWorkedOn) {}