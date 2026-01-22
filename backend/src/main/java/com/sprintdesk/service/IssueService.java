package com.sprintdesk.service;

import com.sprintdesk.dto.CreateIssueRequest;
import com.sprintdesk.dto.IssueResponse;
import com.sprintdesk.dto.IssueUpdateCommand;
import com.sprintdesk.dto.UserSummary;
import com.sprintdesk.model.Issue;
import com.sprintdesk.model.IssuePriority;
import com.sprintdesk.model.IssueStatus;
import com.sprintdesk.model.User;
import com.sprintdesk.model.Workspace;
import com.sprintdesk.model.WorkspaceMember;
import com.sprintdesk.model.WorkspaceRole;
import com.sprintdesk.repository.IssueRepository;
import com.sprintdesk.repository.UserRepository;
import com.sprintdesk.repository.WorkspaceMemberRepository;
import com.sprintdesk.repository.WorkspaceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IssueService {
  private final IssueRepository issueRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final UserRepository userRepository;
  private final WorkspaceService workspaceService;

  public IssueService(
      IssueRepository issueRepository,
      WorkspaceRepository workspaceRepository,
      WorkspaceMemberRepository memberRepository,
      UserRepository userRepository,
      WorkspaceService workspaceService) {
    this.issueRepository = issueRepository;
    this.workspaceRepository = workspaceRepository;
    this.memberRepository = memberRepository;
    this.userRepository = userRepository;
    this.workspaceService = workspaceService;
  }

  public IssuePageResult listIssues(UUID workspaceId, UUID userId, IssueFilter filter) {
    workspaceService.requireMember(workspaceId, userId);

    Specification<Issue> spec = (root, query, cb) -> cb.equal(root.get("workspaceId"), workspaceId);
    if (filter.status() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
    }
    if (filter.priority() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), filter.priority()));
    }
    if (filter.assigneeId() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("assigneeId"), filter.assigneeId()));
    }
    if (filter.ticketId() != null && !filter.ticketId().isBlank()) {
      String ticket = filter.ticketId().toUpperCase(Locale.ROOT);
      spec = spec.and((root, query, cb) -> cb.equal(root.get("ticketId"), ticket));
    }
    if (filter.query() != null && !filter.query().isBlank()) {
      String like = "%" + filter.query().toLowerCase(Locale.ROOT) + "%";
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("title")), like),
                      cb.like(cb.lower(root.get("description")), like)));
    }

    PageRequest pageRequest =
        PageRequest.of(
            Math.max(filter.page() - 1, 0),
            filter.limit(),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Issue> page = issueRepository.findAll(spec, pageRequest);
    List<IssueResponse> issues = mapIssues(page.getContent());
    return new IssuePageResult(issues, filter.page(), filter.limit(), page.getTotalElements());
  }

  public IssueResponse getIssue(UUID workspaceId, UUID userId, UUID issueId) {
    workspaceService.requireMember(workspaceId, userId);
    Issue issue =
        issueRepository
            .findByIdAndWorkspaceId(issueId, workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
    return mapIssue(issue, loadUsers(issue));
  }

  public IssueResponse createIssue(UUID workspaceId, UUID userId, CreateIssueRequest request) {
    WorkspaceMember member = workspaceService.requireMember(workspaceId, userId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);

    Workspace workspace =
        workspaceRepository
            .findById(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

    workspace.setIssueCounter(workspace.getIssueCounter() + 1);
    workspaceRepository.save(workspace);

    Issue issue = new Issue();
    issue.setWorkspaceId(workspaceId);
    issue.setCreatedBy(userId);
    issue.setTicketId(workspace.getKey() + "-" + workspace.getIssueCounter());
    issue.setTitle(request.title().trim());
    issue.setDescription(request.description() == null ? "" : request.description());

    IssueStatus status = parseStatus(request.status()).orElse(IssueStatus.OPEN);
    IssuePriority priority = parsePriority(request.priority()).orElse(IssuePriority.MEDIUM);
    issue.setStatus(status);
    issue.setPriority(priority);
    issue.setLabels(request.labels());

    if (request.dueDate() != null && !request.dueDate().isBlank()) {
      issue.setDueDate(parseInstant(request.dueDate()));
    }

    if (request.assigneeId() != null && !request.assigneeId().isBlank()) {
      UUID assigneeId = parseUuid(request.assigneeId(), "Invalid assignee id");
      ensureMember(workspaceId, assigneeId);
      issue.setAssigneeId(assigneeId);
    }

    Issue saved = issueRepository.save(issue);
    return mapIssue(saved, loadUsers(saved));
  }

  public IssueResponse updateIssue(
      UUID workspaceId, UUID userId, UUID issueId, IssueUpdateCommand command) {
    WorkspaceMember member = workspaceService.requireMember(workspaceId, userId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);

    Issue issue =
        issueRepository
            .findByIdAndWorkspaceId(issueId, workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

    if (command.hasTitle()) {
      if (command.title() == null || command.title().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
      }
      issue.setTitle(command.title().trim());
    }
    if (command.hasDescription()) {
      issue.setDescription(command.description() == null ? "" : command.description());
    }
    if (command.hasStatus()) {
      issue.setStatus(command.status());
    }
    if (command.hasPriority()) {
      issue.setPriority(command.priority());
    }
    if (command.hasLabels()) {
      issue.setLabels(command.labels());
    }
    if (command.hasDueDate()) {
      issue.setDueDate(command.dueDate());
    }
    if (command.hasAssigneeId()) {
      UUID assigneeId = command.assigneeId();
      if (assigneeId != null) {
        ensureMember(workspaceId, assigneeId);
      }
      issue.setAssigneeId(assigneeId);
    }

    Issue saved = issueRepository.save(issue);
    return mapIssue(saved, loadUsers(saved));
  }

  public void deleteIssue(UUID workspaceId, UUID userId, UUID issueId) {
    WorkspaceMember member = workspaceService.requireMember(workspaceId, userId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);

    Issue issue =
        issueRepository
            .findByIdAndWorkspaceId(issueId, workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
    issueRepository.delete(issue);
  }

  private Optional<IssueStatus> parseStatus(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(IssueStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
    }
  }

  private Optional<IssuePriority> parsePriority(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(IssuePriority.valueOf(value.trim().toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid priority");
    }
  }

  private Instant parseInstant(String value) {
    try {
      return Instant.parse(value);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
    }
  }

  private UUID parseUuid(String value, String message) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }

  private void ensureMember(UUID workspaceId, UUID userId) {
    memberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not in workspace"));
  }

  private List<IssueResponse> mapIssues(List<Issue> issues) {
    if (issues.isEmpty()) {
      return List.of();
    }
    Map<UUID, User> users = loadUsers(issues);
    return issues.stream().map(issue -> mapIssue(issue, users)).toList();
  }

  private Map<UUID, User> loadUsers(Issue issue) {
    return loadUsers(List.of(issue));
  }

  private Map<UUID, User> loadUsers(List<Issue> issues) {
    Set<UUID> ids = new HashSet<>();
    for (Issue issue : issues) {
      ids.add(issue.getCreatedBy());
      if (issue.getAssigneeId() != null) {
        ids.add(issue.getAssigneeId());
      }
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    List<User> users = userRepository.findByIdIn(ids);
    Map<UUID, User> lookup = new HashMap<>();
    for (User user : users) {
      lookup.put(user.getId(), user);
    }
    return lookup;
  }

  private IssueResponse mapIssue(Issue issue, Map<UUID, User> users) {
    UserSummary createdBy = toSummary(users.get(issue.getCreatedBy()));
    UserSummary assignee = toSummary(users.get(issue.getAssigneeId()));
    return new IssueResponse(
        issue.getId().toString(),
        issue.getTicketId(),
        issue.getTitle(),
        issue.getDescription(),
        issue.getStatus().name(),
        issue.getPriority().name(),
        new ArrayList<>(issue.getLabels()),
        assignee,
        createdBy,
        issue.getCreatedAt().toString(),
        issue.getUpdatedAt().toString());
  }

  private UserSummary toSummary(User user) {
    if (user == null) {
      return null;
    }
    return new UserSummary(
        user.getId().toString(), user.getName(), user.getEmail(), user.getAvatarUrl());
  }

  public record IssueFilter(
      IssueStatus status,
      IssuePriority priority,
      UUID assigneeId,
      String ticketId,
      String query,
      int page,
      int limit) {}

  public record IssuePageResult(
      List<IssueResponse> issues, int page, int limit, long total) {}
}