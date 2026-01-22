package com.sprintdesk.dto;

public record WorkspaceResponse(
    String id,
    String name,
    String key,
    String ownerId,
    String role) {}