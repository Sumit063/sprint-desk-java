package com.sprintdesk.dto;

public record AuthResponse(String accessToken, UserResponse user) {}