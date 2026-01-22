package com.sprintdesk.service;

import com.sprintdesk.dto.CommentRequest;
import com.sprintdesk.dto.CommentResponse;
import com.sprintdesk.dto.UserSummary;
import com.sprintdesk.model.Comment;
import com.sprintdesk.model.Issue;
import com.sprintdesk.model.User;
import com.sprintdesk.model.WorkspaceMember;
import com.sprintdesk.repository.CommentRepository;
import com.sprintdesk.repository.IssueRepository;
import com.sprintdesk.repository.UserRepository;
import com.sprintdesk.repository.WorkspaceMemberRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {
  private static final Pattern MENTION_PATTERN =
      Pattern.compile("@([\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,})");

  private final CommentRepository commentRepository;
  private final IssueRepository issueRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final UserRepository userRepository;
  private final WorkspaceService workspaceService;
  private final ActivityService activityService;
  private final NotificationService notificationService;
  private final RealtimeService realtimeService;

  public CommentService(
      CommentRepository commentRepository,
      IssueRepository issueRepository,
      WorkspaceMemberRepository memberRepository,
      UserRepository userRepository,
      WorkspaceService workspaceService,
      ActivityService activityService,
      NotificationService notificationService,
      RealtimeService realtimeService) {
    this.commentRepository = commentRepository;
    this.issueRepository = issueRepository;
    this.memberRepository = memberRepository;
    this.userRepository = userRepository;
    this.workspaceService = workspaceService;
    this.activityService = activityService;
    this.notificationService = notificationService;
    this.realtimeService = realtimeService;
  }

  /**
   * Load comments for an issue after workspace membership validation.
   */
  public List<CommentResponse> listComments(UUID issueId, UUID userId) {
    Issue issue = loadIssue(issueId);
    workspaceService.requireMember(issue.getWorkspaceId(), userId);
    List<Comment> comments = commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
    return mapComments(comments);
  }

  public CommentResponse createComment(UUID issueId, UUID userId, CommentRequest request) {
    Issue issue = loadIssue(issueId);
    WorkspaceMember member = workspaceService.requireMember(issue.getWorkspaceId(), userId);

    Comment comment = new Comment();
    comment.setIssueId(issueId);
    comment.setUserId(userId);
    comment.setBody(request.body());
    comment.setInternal(false);
    Comment saved = commentRepository.save(comment);

    issue.setUpdatedAt(java.time.Instant.now());
    issueRepository.save(issue);

    activityService.logActivity(
        issue.getWorkspaceId(),
        userId,
        issueId,
        "comment_added",
        java.util.Map.of("commentId", saved.getId().toString()));

    realtimeService.publishWorkspaceEvent(
        issue.getWorkspaceId().toString(),
        "comment_added",
        java.util.Map.of("issueId", issueId.toString(), "actorId", userId.toString()));

    notifyMentions(issue, member, request.body());

    return mapComment(saved, userRepository.findById(userId).orElse(null));
  }

  private Issue loadIssue(UUID issueId) {
    return issueRepository
        .findById(issueId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
  }

  private void notifyMentions(Issue issue, WorkspaceMember author, String body) {
    if (body == null || body.isBlank()) {
      return;
    }
    Matcher matcher = MENTION_PATTERN.matcher(body);
    Set<String> emails = new HashSet<>();
    while (matcher.find()) {
      emails.add(matcher.group(1).toLowerCase());
    }
    if (emails.isEmpty()) {
      return;
    }
    List<User> users = userRepository.findByEmailIgnoreCaseIn(emails);
    if (users.isEmpty()) {
      return;
    }

    List<WorkspaceMember> members = memberRepository.findByWorkspaceId(issue.getWorkspaceId());
    Set<UUID> workspaceUsers = new HashSet<>();
    for (WorkspaceMember member : members) {
      workspaceUsers.add(member.getUser().getId());
    }

    for (User user : users) {
      if (!workspaceUsers.contains(user.getId())) {
        continue;
      }
      if (user.getId().equals(author.getUser().getId())) {
        continue;
      }
      notificationService.createNotification(
          user.getId(),
          issue.getWorkspaceId(),
          issue.getId(),
          "mention",
          "You were mentioned in issue \"" + issue.getTitle() + "\"");
    }
  }

  private List<CommentResponse> mapComments(List<Comment> comments) {
    if (comments.isEmpty()) {
      return List.of();
    }
    Set<UUID> userIds = new HashSet<>();
    for (Comment comment : comments) {
      userIds.add(comment.getUserId());
    }
    List<User> users = userRepository.findByIdIn(userIds);
    java.util.Map<UUID, User> userLookup = new java.util.HashMap<>();
    for (User user : users) {
      userLookup.put(user.getId(), user);
    }
    List<CommentResponse> responses = new ArrayList<>();
    for (Comment comment : comments) {
      responses.add(mapComment(comment, userLookup.get(comment.getUserId())));
    }
    return responses;
  }

  private CommentResponse mapComment(Comment comment, User user) {
    return new CommentResponse(
        comment.getId().toString(),
        comment.getBody(),
        toSummary(user),
        comment.getCreatedAt().toString());
  }

  private UserSummary toSummary(User user) {
    if (user == null) {
      return null;
    }
    return new UserSummary(user.getId().toString(), user.getName(), user.getEmail(), user.getAvatarUrl());
  }
}
