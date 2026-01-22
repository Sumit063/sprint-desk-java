package com.sprintdesk.controller;

import com.sprintdesk.config.AppProperties;
import com.sprintdesk.dto.CreateWorkspaceRequest;
import com.sprintdesk.dto.JoinWorkspaceRequest;
import com.sprintdesk.dto.MemberOverviewResponse;
import com.sprintdesk.dto.UpdateWorkspaceRoleRequest;
import com.sprintdesk.dto.WorkspaceMemberResponse;
import com.sprintdesk.dto.WorkspaceResponse;
import com.sprintdesk.model.WorkspaceInvite;
import com.sprintdesk.model.WorkspaceMember;
import com.sprintdesk.model.WorkspaceRole;
import com.sprintdesk.security.SecurityUtils;
import com.sprintdesk.service.WorkspaceService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
  private final WorkspaceService workspaceService;
  private final AppProperties appProperties;

  public WorkspaceController(WorkspaceService workspaceService, AppProperties appProperties) {
    this.workspaceService = workspaceService;
    this.appProperties = appProperties;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> listWorkspaces() {
    UUID userId = requireUser();
    List<WorkspaceResponse> workspaces = workspaceService.listWorkspaces(userId);
    return ResponseEntity.ok(Map.of("workspaces", workspaces));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> createWorkspace(
      @Valid @RequestBody CreateWorkspaceRequest request) {
    UUID userId = requireUser();
    var result = workspaceService.createWorkspace(userId, request);
    WorkspaceResponse workspace =
        new WorkspaceResponse(
            result.workspace().getId().toString(),
            result.workspace().getName(),
            result.workspace().getKey(),
            result.workspace().getOwnerId().toString(),
            result.member().getRole().name());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("workspace", workspace, "role", result.member().getRole().name()));
  }

  @PostMapping("/join")
  public ResponseEntity<Map<String, Object>> joinWorkspace(
      @Valid @RequestBody JoinWorkspaceRequest request) {
    UUID userId = requireUser();
    var result = workspaceService.joinWorkspace(userId, request.code().trim());
    WorkspaceResponse workspace =
        new WorkspaceResponse(
            result.workspace().getId().toString(),
            result.workspace().getName(),
            result.workspace().getKey(),
            result.workspace().getOwnerId().toString(),
            result.member().getRole().name());

    return ResponseEntity.ok(Map.of("workspace", workspace, "role", result.member().getRole().name()));
  }

  @GetMapping("/{workspaceId}/members")
  public ResponseEntity<Map<String, Object>> listMembers(@PathVariable UUID workspaceId) {
    WorkspaceMember member = requireMember(workspaceId);
    List<WorkspaceMemberResponse> members = workspaceService.listMembers(member.getWorkspace().getId());
    return ResponseEntity.ok(Map.of("members", members));
  }

  @PatchMapping("/{workspaceId}/members/{memberId}")
  public ResponseEntity<Map<String, Object>> updateMemberRole(
      @PathVariable UUID workspaceId,
      @PathVariable UUID memberId,
      @Valid @RequestBody UpdateWorkspaceRoleRequest request) {
    WorkspaceMember member = requireMember(workspaceId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER);
    WorkspaceMemberResponse updated =
        workspaceService.updateMemberRole(workspaceId, memberId, request, member.getUser().getId());
    return ResponseEntity.ok(Map.of("member", updated));
  }

  @GetMapping("/{workspaceId}/members/{memberId}/overview")
  public ResponseEntity<MemberOverviewResponse> getMemberOverview(
      @PathVariable UUID workspaceId, @PathVariable UUID memberId) {
    requireMember(workspaceId);
    return ResponseEntity.ok(workspaceService.getMemberOverview(workspaceId, memberId));
  }

  @PostMapping("/{workspaceId}/invite")
  public ResponseEntity<Map<String, Object>> createInvite(@PathVariable UUID workspaceId) {
    WorkspaceMember member = requireMember(workspaceId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
    WorkspaceInvite invite = workspaceService.createInvite(workspaceId, member.getUser().getId(), Duration.ofDays(7));

    String inviteLink = appProperties.getBaseUrl();
    if (!inviteLink.endsWith("/")) {
      inviteLink += "/";
    }
    inviteLink += "join?code=" + invite.getCode();

    return ResponseEntity.ok(
        Map.of(
            "inviteCode", invite.getCode(),
            "inviteLink", inviteLink,
            "expiresAt", invite.getExpiresAt().toString()));
  }

  private UUID requireUser() {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return userId;
  }

  private WorkspaceMember requireMember(UUID workspaceId) {
    UUID userId = requireUser();
    return workspaceService.requireMember(workspaceId, userId);
  }
}