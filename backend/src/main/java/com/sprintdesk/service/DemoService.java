package com.sprintdesk.service;

import com.sprintdesk.config.DemoProperties;
import com.sprintdesk.model.Article;
import com.sprintdesk.model.Issue;
import com.sprintdesk.model.IssuePriority;
import com.sprintdesk.model.IssueStatus;
import com.sprintdesk.model.Role;
import com.sprintdesk.model.User;
import com.sprintdesk.model.Workspace;
import com.sprintdesk.model.WorkspaceMember;
import com.sprintdesk.model.WorkspaceRole;
import com.sprintdesk.repository.ArticleRepository;
import com.sprintdesk.repository.CommentRepository;
import com.sprintdesk.repository.IssueRepository;
import com.sprintdesk.repository.NotificationRepository;
import com.sprintdesk.repository.ActivityRepository;
import com.sprintdesk.repository.WorkspaceMemberRepository;
import com.sprintdesk.repository.WorkspaceRepository;
import com.sprintdesk.repository.WorkspaceInviteRepository;
import com.sprintdesk.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoService {
  /**
   * Creates or resets demo data for local development.
   */
  public static final String OWNER_EMAIL = "demo_owner@demo.com";
  public static final String MEMBER_EMAIL = "demo_member@demo.com";
  public static final String DEMO_PASSWORD = "Demo@1234";

  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final IssueRepository issueRepository;
  private final ArticleRepository articleRepository;
  private final CommentRepository commentRepository;
  private final ActivityRepository activityRepository;
  private final NotificationRepository notificationRepository;
  private final WorkspaceInviteRepository inviteRepository;
  private final PasswordEncoder passwordEncoder;
  private final DemoProperties demoProperties;

  public DemoService(
      UserRepository userRepository,
      WorkspaceRepository workspaceRepository,
      WorkspaceMemberRepository memberRepository,
      IssueRepository issueRepository,
      ArticleRepository articleRepository,
      CommentRepository commentRepository,
      ActivityRepository activityRepository,
      NotificationRepository notificationRepository,
      WorkspaceInviteRepository inviteRepository,
      PasswordEncoder passwordEncoder,
      DemoProperties demoProperties) {
    this.userRepository = userRepository;
    this.workspaceRepository = workspaceRepository;
    this.memberRepository = memberRepository;
    this.issueRepository = issueRepository;
    this.articleRepository = articleRepository;
    this.commentRepository = commentRepository;
    this.activityRepository = activityRepository;
    this.notificationRepository = notificationRepository;
    this.inviteRepository = inviteRepository;
    this.passwordEncoder = passwordEncoder;
    this.demoProperties = demoProperties;
  }

  public boolean isEnabled() {
    return demoProperties.isEnabled();
  }

  public DemoUsers ensureDemoUsers() {
    User owner = userRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseGet(() -> createUser(OWNER_EMAIL, "Demo Owner", Role.OWNER));
    User member = userRepository.findByEmailIgnoreCase(MEMBER_EMAIL).orElseGet(() -> createUser(MEMBER_EMAIL, "Demo Member", Role.MEMBER));
    return new DemoUsers(owner, member);
  }

  @Transactional
  public void resetDemoData() {
    DemoUsers users = ensureDemoUsers();

    Workspace workspace = workspaceRepository.findByKeyIgnoreCase("DEMO").orElse(null);
    if (workspace != null) {
      clearWorkspaceData(workspace.getId(), users);
      workspace.setIssueCounter(0);
      workspace.setKbCounter(0);
    } else {
      workspace = new Workspace();
      workspace.setKey("DEMO");
    }

    workspace.setName("Demo Workspace");
    workspace.setOwnerId(users.owner().getId());
    workspaceRepository.save(workspace);

    saveMembership(workspace, users.owner(), WorkspaceRole.OWNER);
    saveMembership(workspace, users.member(), WorkspaceRole.MEMBER);

    Issue issueOne = createIssue(workspace, users.owner(), "First demo issue", IssueStatus.OPEN, IssuePriority.HIGH, users.member().getId());
    Issue issueTwo = createIssue(workspace, users.member(), "Payment webhook failing", IssueStatus.IN_PROGRESS, IssuePriority.MEDIUM, users.owner().getId());
    Issue issueThree = createIssue(workspace, users.owner(), "Update FAQ", IssueStatus.DONE, IssuePriority.LOW, null);

    Article article = new Article();
    workspace.setKbCounter(workspace.getKbCounter() + 1);
    article.setWorkspaceId(workspace.getId());
    article.setKbId(workspace.getKey() + "-KB-" + workspace.getKbCounter());
    article.setTitle("Demo Knowledge Base");
    article.setBody("This is a seeded knowledge base article for SprintDesk.");
    article.setCreatedBy(users.owner().getId());
    article.setUpdatedBy(users.owner().getId());
    article.setLinkedIssueIds(List.of(issueOne.getId(), issueTwo.getId()));
    articleRepository.save(article);

    workspaceRepository.save(workspace);

    addComment(issueOne, users.owner(), "Created demo issue. Let's fix it quickly.");
    addComment(issueTwo, users.member(), "Investigating logs now.");
    addComment(issueThree, users.owner(), "Marked as done.");
  }

  private void clearWorkspaceData(UUID workspaceId, DemoUsers users) {
    inviteRepository.deleteByWorkspace_Id(workspaceId);
    memberRepository.deleteByWorkspace_Id(workspaceId);
    activityRepository.deleteByWorkspaceId(workspaceId);
    articleRepository.deleteByWorkspaceId(workspaceId);

    List<Issue> issues = issueRepository.findByWorkspaceId(workspaceId);
    List<UUID> issueIds = issues.stream().map(Issue::getId).collect(Collectors.toList());
    if (!issueIds.isEmpty()) {
      commentRepository.deleteByIssueIdIn(issueIds);
    }
    issueRepository.deleteByWorkspaceId(workspaceId);

    notificationRepository.deleteByUserIdIn(List.of(users.owner().getId(), users.member().getId()));
  }

  private User createUser(String email, String name, Role role) {
    User user = new User();
    user.setEmail(email.toLowerCase(Locale.ROOT));
    user.setName(name);
    user.setRole(role);
    user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
    return userRepository.save(user);
  }

  private Issue createIssue(
      Workspace workspace,
      User creator,
      String title,
      IssueStatus status,
      IssuePriority priority,
      UUID assigneeId) {
    workspace.setIssueCounter(workspace.getIssueCounter() + 1);
    Issue issue = new Issue();
    issue.setWorkspaceId(workspace.getId());
    issue.setCreatedBy(creator.getId());
    issue.setAssigneeId(assigneeId);
    issue.setTicketId(workspace.getKey() + "-" + workspace.getIssueCounter());
    issue.setTitle(title);
    issue.setDescription("Seeded demo issue for " + title);
    issue.setStatus(status);
    issue.setPriority(priority);
    return issueRepository.save(issue);
  }

  private void addComment(Issue issue, User user, String body) {
    com.sprintdesk.model.Comment comment = new com.sprintdesk.model.Comment();
    comment.setIssueId(issue.getId());
    comment.setUserId(user.getId());
    comment.setBody(body);
    comment.setInternal(false);
    commentRepository.save(comment);
  }

  private void saveMembership(Workspace workspace, User user, WorkspaceRole role) {
    WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspace.getId(), user.getId())
        .orElseGet(WorkspaceMember::new);
    member.setWorkspace(workspace);
    member.setUser(user);
    member.setRole(role);
    memberRepository.save(member);
  }

  public record DemoUsers(User owner, User member) {}
}
