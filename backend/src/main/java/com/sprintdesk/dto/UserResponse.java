package com.sprintdesk.dto;

public record UserResponse(
    String id,
    String email,
    String name,
    String avatarUrl,
    String contact) {}