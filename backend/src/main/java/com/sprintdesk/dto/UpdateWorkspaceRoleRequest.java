package com.sprintdesk.dto;

import com.sprintdesk.model.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkspaceRoleRequest(@NotNull WorkspaceRole role) {}