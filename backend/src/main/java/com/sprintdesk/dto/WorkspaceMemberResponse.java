package com.sprintdesk.dto;

public record WorkspaceMemberResponse(
    String id,
    String role,
    UserResponse user) {}