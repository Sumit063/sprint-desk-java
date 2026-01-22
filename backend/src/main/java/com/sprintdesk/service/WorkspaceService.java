package com.sprintdesk.service;

import com.sprintdesk.dto.CreateWorkspaceRequest;
import com.sprintdesk.dto.MemberOverviewRecent;
import com.sprintdesk.dto.MemberOverviewResponse;
import com.sprintdesk.dto.MemberOverviewStats;
import com.sprintdesk.dto.UpdateWorkspaceRoleRequest;
import com.sprintdesk.dto.UserResponse;
import com.sprintdesk.dto.WorkspaceMemberResponse;
import com.sprintdesk.dto.WorkspaceResponse;
import com.sprintdesk.model.User;
import com.sprintdesk.model.Workspace;
import com.sprintdesk.model.WorkspaceInvite;
import com.sprintdesk.model.WorkspaceMember;
import com.sprintdesk.model.WorkspaceRole;
import com.sprintdesk.repository.UserRepository;
import com.sprintdesk.repository.WorkspaceInviteRepository;
import com.sprintdesk.repository.WorkspaceMemberRepository;
import com.sprintdesk.repository.WorkspaceRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceService {
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final WorkspaceInviteRepository inviteRepository;
  private final UserRepository userRepository;
  private final SecureRandom random = new SecureRandom();

  public WorkspaceService(
      WorkspaceRepository workspaceRepository,
      WorkspaceMemberRepository memberRepository,
      WorkspaceInviteRepository inviteRepository,
      UserRepository userRepository) {
    this.workspaceRepository = workspaceRepository;
    this.memberRepository = memberRepository;
    this.inviteRepository = inviteRepository;
    this.userRepository = userRepository;
  }

  public List<WorkspaceResponse> listWorkspaces(UUID userId) {
    return memberRepository.findByUserId(userId).stream()
        .map(
            membership -> {
              Workspace workspace = membership.getWorkspace();
              return new WorkspaceResponse(
                  workspace.getId().toString(),
                  workspace.getName(),
                  workspace.getKey(),
                  workspace.getOwnerId().toString(),
                  membership.getRole().name());
            })
        .toList();
  }

  public WorkspaceMembershipResult createWorkspace(UUID userId, CreateWorkspaceRequest request) {
    String key = request.key().trim().toUpperCase(Locale.ROOT);
    if (workspaceRepository.existsByKeyIgnoreCase(key)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Workspace key already in use");
    }

    Workspace workspace = new Workspace();
    workspace.setName(request.name().trim());
    workspace.setKey(key);
    workspace.setOwnerId(userId);
    workspaceRepository.save(workspace);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    WorkspaceMember member = new WorkspaceMember();
    member.setWorkspace(workspace);
    member.setUser(user);
    member.setRole(WorkspaceRole.OWNER);
    memberRepository.save(member);

    return new WorkspaceMembershipResult(workspace, member);
  }

  public WorkspaceMembershipResult joinWorkspace(UUID userId, String code) {
    WorkspaceInvite invite =
        inviteRepository
            .findByCode(code)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite invalid"));

    if (invite.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite expired");
    }

    Workspace workspace = invite.getWorkspace();
    WorkspaceMember member =
        memberRepository
            .findByWorkspaceIdAndUserId(workspace.getId(), userId)
            .orElseGet(
                () -> {
                  User user =
                      userRepository
                          .findById(userId)
                          .orElseThrow(
                              () ->
                                  new ResponseStatusException(
                                      HttpStatus.NOT_FOUND, "User not found"));
                  WorkspaceMember created = new WorkspaceMember();
                  created.setWorkspace(workspace);
                  created.setUser(user);
                  created.setRole(WorkspaceRole.MEMBER);
                  return memberRepository.save(created);
                });

    return new WorkspaceMembershipResult(workspace, member);
  }

  public WorkspaceMember requireMember(UUID workspaceId, UUID userId) {
    return memberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
  }

  public void requireRole(WorkspaceMember member, WorkspaceRole... allowed) {
    for (WorkspaceRole role : allowed) {
      if (member.getRole() == role) {
        return;
      }
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
  }

  public List<WorkspaceMemberResponse> listMembers(UUID workspaceId) {
    return memberRepository.findByWorkspaceId(workspaceId).stream()
        .map(
            member ->
                new WorkspaceMemberResponse(
                    member.getId().toString(),
                    member.getRole().name(),
                    toUserResponse(member.getUser())))
        .toList();
  }

  public WorkspaceMemberResponse updateMemberRole(
      UUID workspaceId, UUID memberId, UpdateWorkspaceRoleRequest request, UUID actorId) {
    WorkspaceMember member =
        memberRepository
            .findById(memberId)
            .filter(m -> m.getWorkspace().getId().equals(workspaceId))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

    if (member.getUser().getId().equals(actorId)
        && request.role() != WorkspaceRole.OWNER) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot remove role");
    }

    member.setRole(request.role());
    memberRepository.save(member);

    return new WorkspaceMemberResponse(
        member.getId().toString(), member.getRole().name(), toUserResponse(member.getUser()));
  }

  public WorkspaceInvite createInvite(UUID workspaceId, UUID actorId, Duration ttl) {
    Workspace workspace =
        workspaceRepository
            .findById(workspaceId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

    WorkspaceInvite invite = new WorkspaceInvite();
    invite.setWorkspace(workspace);
    invite.setCode(generateCode());
    invite.setExpiresAt(Instant.now().plus(ttl));
    invite.setCreatedBy(actorId);
    return inviteRepository.save(invite);
  }

  public MemberOverviewResponse getMemberOverview(UUID workspaceId, UUID memberId) {
    WorkspaceMember member =
        memberRepository
            .findById(memberId)
            .filter(m -> m.getWorkspace().getId().equals(workspaceId))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

    UserResponse user = toUserResponse(member.getUser());

    MemberOverviewStats stats = new MemberOverviewStats(0, 0, 0);
    MemberOverviewRecent recent =
        new MemberOverviewRecent(
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    return new MemberOverviewResponse(user, stats, recent);
  }

  private String generateCode() {
    byte[] bytes = new byte[4];
    random.nextBytes(bytes);
    StringBuilder builder = new StringBuilder();
    for (byte value : bytes) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId().toString(),
        user.getEmail(),
        user.getName(),
        user.getAvatarUrl(),
        user.getContact());
  }

  public record WorkspaceMembershipResult(Workspace workspace, WorkspaceMember member) {}
}