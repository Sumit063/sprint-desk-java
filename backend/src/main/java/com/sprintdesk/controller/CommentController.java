package com.sprintdesk.controller;

import com.sprintdesk.dto.CommentRequest;
import com.sprintdesk.dto.CommentResponse;
import com.sprintdesk.security.SecurityUtils;
import com.sprintdesk.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/issues/{issueId}/comments")
public class CommentController {
  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> listComments(@PathVariable UUID issueId) {
    UUID userId = requireUser();
    List<CommentResponse> comments = commentService.listComments(issueId, userId);
    return ResponseEntity.ok(Map.of("comments", comments));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> createComment(
      @PathVariable UUID issueId, @Valid @RequestBody CommentRequest request) {
    UUID userId = requireUser();
    CommentResponse comment = commentService.createComment(issueId, userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("comment", comment));
  }

  private UUID requireUser() {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return userId;
  }
}