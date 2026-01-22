package com.sprintdesk.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinWorkspaceRequest(@NotBlank String code) {}