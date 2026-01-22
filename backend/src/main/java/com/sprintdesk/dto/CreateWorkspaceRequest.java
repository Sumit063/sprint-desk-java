package com.sprintdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
    @NotBlank String name,
    @NotBlank @Size(min = 2, max = 10) String key) {}