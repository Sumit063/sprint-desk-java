package com.sprintdesk.dto;

public record MemberOverviewResponse(
    UserResponse user,
    MemberOverviewStats stats,
    MemberOverviewRecent recent) {}