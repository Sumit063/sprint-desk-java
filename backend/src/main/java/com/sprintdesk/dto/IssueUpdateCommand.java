package com.sprintdesk.dto;

import com.sprintdesk.model.IssuePriority;
import com.sprintdesk.model.IssueStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueUpdateCommand(
    boolean hasTitle,
    String title,
    boolean hasDescription,
    String description,
    boolean hasStatus,
    IssueStatus status,
    boolean hasPriority,
    IssuePriority priority,
    boolean hasLabels,
    List<String> labels,
    boolean hasAssigneeId,
    UUID assigneeId,
    boolean hasDueDate,
    Instant dueDate) {}