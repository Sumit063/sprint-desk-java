package com.sprintdesk.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprintdesk.dto.CreateIssueRequest;
import com.sprintdesk.dto.IssueResponse;
import com.sprintdesk.dto.IssueUpdateCommand;
import com.sprintdesk.model.IssuePriority;
import com.sprintdesk.model.IssueStatus;
import com.sprintdesk.security.SecurityUtils;
import com.sprintdesk.service.IssueService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/issues")
public class IssueController {
  private final IssueService issueService;

  public IssueController(IssueService issueService) {
    this.issueService = issueService;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> listIssues(
      @PathVariable UUID workspaceId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) String assigneeId,
      @RequestParam(required = false) String ticketId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit) {
    UUID userId = requireUser();
    IssueStatus statusEnum = parseStatus(status);
    IssuePriority priorityEnum = parsePriority(priority);
    UUID assignee = assigneeId == null || assigneeId.isBlank() ? null : parseUuid(assigneeId);
    int safeLimit = Math.min(Math.max(limit, 1), 50);
    int safePage = Math.max(page, 1);

    IssueService.IssuePageResult result =
        issueService.listIssues(
            workspaceId,
            userId,
            new IssueService.IssueFilter(
                statusEnum, priorityEnum, assignee, ticketId, q, safePage, safeLimit));

    return ResponseEntity.ok(
        Map.of(
            "issues",
            result.issues(),
            "pagination",
            Map.of(
                "page", result.page(),
                "limit", result.limit(),
                "total", result.total())));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> createIssue(
      @PathVariable UUID workspaceId, @Valid @RequestBody CreateIssueRequest request) {
    UUID userId = requireUser();
    IssueResponse issue = issueService.createIssue(workspaceId, userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("issue", issue));
  }

  @GetMapping("/{issueId}")
  public ResponseEntity<Map<String, Object>> getIssue(
      @PathVariable UUID workspaceId, @PathVariable UUID issueId) {
    UUID userId = requireUser();
    IssueResponse issue = issueService.getIssue(workspaceId, userId, issueId);
    return ResponseEntity.ok(Map.of("issue", issue));
  }

  @PatchMapping("/{issueId}")
  public ResponseEntity<Map<String, Object>> updateIssue(
      @PathVariable UUID workspaceId,
      @PathVariable UUID issueId,
      @RequestBody JsonNode payload) {
    UUID userId = requireUser();
    IssueUpdateCommand command = parseUpdate(payload);
    IssueResponse issue = issueService.updateIssue(workspaceId, userId, issueId, command);
    return ResponseEntity.ok(Map.of("issue", issue));
  }

  @DeleteMapping("/{issueId}")
  public ResponseEntity<Map<String, Object>> deleteIssue(
      @PathVariable UUID workspaceId, @PathVariable UUID issueId) {
    UUID userId = requireUser();
    issueService.deleteIssue(workspaceId, userId, issueId);
    return ResponseEntity.ok(Map.of("ok", true));
  }

  private UUID requireUser() {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return userId;
  }

  private IssueUpdateCommand parseUpdate(JsonNode payload) {
    if (payload == null || payload.isNull()) {
      return new IssueUpdateCommand(
          false,
          null,
          false,
          null,
          false,
          null,
          false,
          null,
          false,
          null,
          false,
          null,
          false,
          null);
    }

    boolean hasTitle = payload.has("title");
    String title = hasTitle && !payload.get("title").isNull() ? payload.get("title").asText() : null;

    boolean hasDescription = payload.has("description");
    String description = hasDescription && !payload.get("description").isNull()
        ? payload.get("description").asText()
        : null;

    boolean hasStatus = payload.has("status");
    IssueStatus status = hasStatus && !payload.get("status").isNull()
        ? parseStatus(payload.get("status").asText())
        : null;

    boolean hasPriority = payload.has("priority");
    IssuePriority priority = hasPriority && !payload.get("priority").isNull()
        ? parsePriority(payload.get("priority").asText())
        : null;

    boolean hasLabels = payload.has("labels");
    List<String> labels = null;
    if (hasLabels) {
      if (payload.get("labels").isNull()) {
        labels = List.of();
      } else if (payload.get("labels").isArray()) {
        List<String> collected = new ArrayList<>();
        for (JsonNode node : payload.get("labels")) {
          collected.add(node.asText());
        }
        labels = collected;
      }
    }

    boolean hasAssigneeId = payload.has("assigneeId");
    UUID assigneeId = null;
    if (hasAssigneeId && !payload.get("assigneeId").isNull()) {
      assigneeId = parseUuid(payload.get("assigneeId").asText());
    }

    boolean hasDueDate = payload.has("dueDate");
    Instant dueDate = null;
    if (hasDueDate && !payload.get("dueDate").isNull()) {
      try {
        dueDate = Instant.parse(payload.get("dueDate").asText());
      } catch (Exception ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
      }
    }

    return new IssueUpdateCommand(
        hasTitle,
        title,
        hasDescription,
        description,
        hasStatus,
        status,
        hasPriority,
        priority,
        hasLabels,
        labels,
        hasAssigneeId,
        assigneeId,
        hasDueDate,
        dueDate);
  }

  private IssueStatus parseStatus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return IssueStatus.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
    }
  }

  private IssuePriority parsePriority(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return IssuePriority.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid priority");
    }
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id");
    }
  }
}
