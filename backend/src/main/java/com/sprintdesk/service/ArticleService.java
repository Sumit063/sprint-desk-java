package com.sprintdesk.service;

import com.sprintdesk.dto.ArticleRequest;
import com.sprintdesk.dto.ArticleResponse;
import com.sprintdesk.dto.UserSummary;
import com.sprintdesk.model.Article;
import com.sprintdesk.model.Issue;
import com.sprintdesk.model.User;
import com.sprintdesk.model.Workspace;
import com.sprintdesk.model.WorkspaceMember;
import com.sprintdesk.model.WorkspaceRole;
import com.sprintdesk.repository.ArticleRepository;
import com.sprintdesk.repository.IssueRepository;
import com.sprintdesk.repository.UserRepository;
import com.sprintdesk.repository.WorkspaceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleService {
  /**
   * Manages knowledge base articles scoped to a workspace.
   */
  private final ArticleRepository articleRepository;
  private final WorkspaceRepository workspaceRepository;
  private final IssueRepository issueRepository;
  private final UserRepository userRepository;
  private final WorkspaceService workspaceService;
  private final ActivityService activityService;

  public ArticleService(
      ArticleRepository articleRepository,
      WorkspaceRepository workspaceRepository,
      IssueRepository issueRepository,
      UserRepository userRepository,
      WorkspaceService workspaceService,
      ActivityService activityService) {
    this.articleRepository = articleRepository;
    this.workspaceRepository = workspaceRepository;
    this.issueRepository = issueRepository;
    this.userRepository = userRepository;
    this.workspaceService = workspaceService;
    this.activityService = activityService;
  }

  public List<ArticleResponse> listArticles(UUID workspaceId, UUID userId, UUID issueId) {
    workspaceService.requireMember(workspaceId, userId);
    List<Article> articles =
        issueId == null
            ? articleRepository.findByWorkspaceId(workspaceId)
            : articleRepository.findByWorkspaceIdAndLinkedIssueIdsContains(workspaceId, issueId);
    return mapArticles(articles);
  }

  public ArticleResponse getArticle(UUID workspaceId, UUID userId, UUID articleId) {
    workspaceService.requireMember(workspaceId, userId);
    Article article =
        articleRepository
            .findByIdAndWorkspaceId(articleId, workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    return mapArticle(article, loadUsers(article));
  }

  public ArticleResponse createArticle(UUID workspaceId, UUID userId, ArticleRequest request) {
    WorkspaceMember member = workspaceService.requireMember(workspaceId, userId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);

    Workspace workspace =
        workspaceRepository
            .findById(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

    workspace.setKbCounter(workspace.getKbCounter() + 1);
    workspaceRepository.save(workspace);

    Article article = new Article();
    article.setWorkspaceId(workspaceId);
    article.setKbId(workspace.getKey() + "-KB-" + workspace.getKbCounter());
    article.setTitle(safeTitle(request.title()));
    article.setBody(request.body() == null ? "" : request.body());
    article.setCreatedBy(userId);
    article.setUpdatedBy(userId);
    article.setLinkedIssueIds(parseLinkedIssues(workspaceId, request.linkedIssueIds()));

    Article saved = articleRepository.save(article);

    Map<String, Object> meta =
        Map.of(
            "articleId", saved.getId().toString(),
            "kbId", saved.getKbId(),
            "title", saved.getTitle(),
            "linkedIssueIds", toStringIds(saved.getLinkedIssueIds()));

    String action = saved.getLinkedIssueIds().isEmpty() ? "kb_created" : "kb_linked";
    activityService.logActivity(workspaceId, userId, null, action, meta);

    return mapArticle(saved, loadUsers(saved));
  }

  public ArticleResponse updateArticle(
      UUID workspaceId, UUID userId, UUID articleId, ArticleRequest request) {
    WorkspaceMember member = workspaceService.requireMember(workspaceId, userId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);

    Article article =
        articleRepository
            .findByIdAndWorkspaceId(articleId, workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));

    boolean titleChanged = request.title() != null;
    boolean bodyChanged = request.body() != null;
    List<UUID> previousLinked = new ArrayList<>(article.getLinkedIssueIds());

    if (titleChanged) {
      article.setTitle(safeTitle(request.title()));
    }
    if (bodyChanged) {
      article.setBody(request.body() == null ? "" : request.body());
    }
    if (request.linkedIssueIds() != null) {
      article.setLinkedIssueIds(parseLinkedIssues(workspaceId, request.linkedIssueIds()));
    }
    article.setUpdatedBy(userId);

    Article saved = articleRepository.save(article);

    List<String> addedLinks = new ArrayList<>();
    for (UUID linked : saved.getLinkedIssueIds()) {
      if (!previousLinked.contains(linked)) {
        addedLinks.add(linked.toString());
      }
    }

    if (!addedLinks.isEmpty()) {
      activityService.logActivity(
          workspaceId,
          userId,
          null,
          "kb_linked",
          Map.of(
              "articleId", saved.getId().toString(),
              "kbId", saved.getKbId(),
              "title", saved.getTitle(),
              "linkedIssueIds", addedLinks));
    } else if (titleChanged || bodyChanged) {
      activityService.logActivity(
          workspaceId,
          userId,
          null,
          "kb_updated",
          Map.of(
              "articleId", saved.getId().toString(),
              "kbId", saved.getKbId(),
              "title", saved.getTitle()));
    }

    return mapArticle(saved, loadUsers(saved));
  }

  public void deleteArticle(UUID workspaceId, UUID userId, UUID articleId) {
    WorkspaceMember member = workspaceService.requireMember(workspaceId, userId);
    workspaceService.requireRole(member, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);

    Article article =
        articleRepository
            .findByIdAndWorkspaceId(articleId, workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    articleRepository.delete(article);
  }

  private String safeTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
    }
    return title.trim();
  }

  private List<UUID> parseLinkedIssues(UUID workspaceId, List<String> ids) {
    if (ids == null) {
      return List.of();
    }
    List<UUID> results = new ArrayList<>();
    for (String raw : ids) {
      if (raw == null || raw.isBlank()) {
        continue;
      }
      UUID id = parseUuid(raw);
      Issue issue =
          issueRepository
              .findByIdAndWorkspaceId(id, workspaceId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid issue link"));
      results.add(issue.getId());
    }
    return results;
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id");
    }
  }

  private List<ArticleResponse> mapArticles(List<Article> articles) {
    if (articles.isEmpty()) {
      return List.of();
    }
    Map<UUID, User> users = loadUsers(articles);
    return articles.stream().map(article -> mapArticle(article, users)).toList();
  }

  private Map<UUID, User> loadUsers(Article article) {
    return loadUsers(List.of(article));
  }

  private Map<UUID, User> loadUsers(List<Article> articles) {
    Set<UUID> ids = new HashSet<>();
    for (Article article : articles) {
      if (article.getCreatedBy() != null) {
        ids.add(article.getCreatedBy());
      }
      if (article.getUpdatedBy() != null) {
        ids.add(article.getUpdatedBy());
      }
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<UUID, User> lookup = new HashMap<>();
    for (User user : userRepository.findByIdIn(ids)) {
      lookup.put(user.getId(), user);
    }
    return lookup;
  }

  private ArticleResponse mapArticle(Article article, Map<UUID, User> users) {
    return new ArticleResponse(
        article.getId().toString(),
        article.getKbId(),
        article.getTitle(),
        article.getBody(),
        toStringIds(article.getLinkedIssueIds()),
        article.getCreatedAt().toString(),
        article.getUpdatedAt().toString(),
        toSummary(users.get(article.getCreatedBy())),
        toSummary(users.get(article.getUpdatedBy())));
  }

  private List<String> toStringIds(List<UUID> ids) {
    List<String> results = new ArrayList<>();
    for (UUID id : ids) {
      results.add(id.toString());
    }
    return results;
  }

  private UserSummary toSummary(User user) {
    if (user == null) {
      return null;
    }
    return new UserSummary(user.getId().toString(), user.getName(), user.getEmail(), user.getAvatarUrl());
  }
}
