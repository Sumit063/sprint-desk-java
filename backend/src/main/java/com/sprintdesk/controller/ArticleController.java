package com.sprintdesk.controller;

import com.sprintdesk.dto.ArticleRequest;
import com.sprintdesk.dto.ArticleResponse;
import com.sprintdesk.security.SecurityUtils;
import com.sprintdesk.service.ArticleService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/workspaces/{workspaceId}/articles")
public class ArticleController {
  private final ArticleService articleService;

  public ArticleController(ArticleService articleService) {
    this.articleService = articleService;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> listArticles(
      @PathVariable UUID workspaceId, @RequestParam(required = false) String issueId) {
    UUID userId = requireUser();
    UUID issue = issueId == null || issueId.isBlank() ? null : parseUuid(issueId);
    List<ArticleResponse> articles = articleService.listArticles(workspaceId, userId, issue);
    return ResponseEntity.ok(Map.of("articles", articles));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> createArticle(
      @PathVariable UUID workspaceId, @Valid @RequestBody ArticleRequest request) {
    UUID userId = requireUser();
    ArticleResponse article = articleService.createArticle(workspaceId, userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("article", article));
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<Map<String, Object>> getArticle(
      @PathVariable UUID workspaceId, @PathVariable UUID articleId) {
    UUID userId = requireUser();
    ArticleResponse article = articleService.getArticle(workspaceId, userId, articleId);
    return ResponseEntity.ok(Map.of("article", article));
  }

  @PatchMapping("/{articleId}")
  public ResponseEntity<Map<String, Object>> updateArticle(
      @PathVariable UUID workspaceId,
      @PathVariable UUID articleId,
      @RequestBody ArticleRequest request) {
    UUID userId = requireUser();
    ArticleResponse article = articleService.updateArticle(workspaceId, userId, articleId, request);
    return ResponseEntity.ok(Map.of("article", article));
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Map<String, Object>> deleteArticle(
      @PathVariable UUID workspaceId, @PathVariable UUID articleId) {
    UUID userId = requireUser();
    articleService.deleteArticle(workspaceId, userId, articleId);
    return ResponseEntity.ok(Map.of("ok", true));
  }

  private UUID requireUser() {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return userId;
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id");
    }
  }
}
