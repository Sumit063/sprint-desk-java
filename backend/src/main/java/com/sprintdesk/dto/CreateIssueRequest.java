package com.sprintdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateIssueRequest(
    @NotBlank String title,
    String description,
    String status,
    String priority,
    List<String> labels,
    String assigneeId,
    @Size(max = 64) String dueDate) {}