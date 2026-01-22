package com.sprintdesk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintdesk.dto.ActivityResponse;
import com.sprintdesk.dto.IssueActivitySummary;
import com.sprintdesk.dto.UserSummary;
import com.sprintdesk.model.Activity;
import com.sprintdesk.model.Issue;
import com.sprintdesk.model.User;
import com.sprintdesk.repository.ActivityRepository;
import com.sprintdesk.repository.IssueRepository;
import com.sprintdesk.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
  private final ActivityRepository activityRepository;
  private final UserRepository userRepository;
  private final IssueRepository issueRepository;
  private final ObjectMapper objectMapper;

  public ActivityService(
      ActivityRepository activityRepository,
      UserRepository userRepository,
      IssueRepository issueRepository,
      ObjectMapper objectMapper) {
    this.activityRepository = activityRepository;
    this.userRepository = userRepository;
    this.issueRepository = issueRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Persist an activity event with optional metadata payload.
   */
  public Activity logActivity(UUID workspaceId, UUID actorId, UUID issueId, String action, Map<String, Object> meta) {
    Activity activity = new Activity();
    activity.setWorkspaceId(workspaceId);
    activity.setActorId(actorId);
    activity.setIssueId(issueId);
    activity.setAction(action);
    if (meta != null && !meta.isEmpty()) {
      try {
        activity.setMeta(objectMapper.writeValueAsString(meta));
      } catch (Exception ex) {
        activity.setMeta(null);
      }
    }
    return activityRepository.save(activity);
  }

  public List<ActivityResponse> listActivities(UUID workspaceId, int limit) {
    List<Activity> activities =
        activityRepository
            .findByWorkspaceId(
                workspaceId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent();

    Map<UUID, User> users = loadUsers(activities);
    Map<UUID, Issue> issues = loadIssues(activities);

    List<ActivityResponse> results = new ArrayList<>();
    for (Activity activity : activities) {
      UserSummary actor = toSummary(users.get(activity.getActorId()));
      IssueActivitySummary issue = toIssueSummary(issues.get(activity.getIssueId()));
      Map<String, Object> meta = parseMeta(activity.getMeta());
      results.add(
          new ActivityResponse(
              activity.getId().toString(),
              activity.getAction(),
              activity.getCreatedAt().toString(),
              actor,
              issue,
              meta));
    }
    return results;
  }

  private Map<String, Object> parseMeta(String metaJson) {
    if (metaJson == null || metaJson.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(metaJson, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      return null;
    }
  }

  private Map<UUID, User> loadUsers(List<Activity> activities) {
    Set<UUID> ids = new HashSet<>();
    for (Activity activity : activities) {
      if (activity.getActorId() != null) {
        ids.add(activity.getActorId());
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

  private Map<UUID, Issue> loadIssues(List<Activity> activities) {
    Set<UUID> ids = new HashSet<>();
    for (Activity activity : activities) {
      if (activity.getIssueId() != null) {
        ids.add(activity.getIssueId());
      }
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Issue> lookup = new HashMap<>();
    for (Issue issue : issueRepository.findAllById(ids)) {
      lookup.put(issue.getId(), issue);
    }
    return lookup;
  }

  private UserSummary toSummary(User user) {
    if (user == null) {
      return null;
    }
    return new UserSummary(user.getId().toString(), user.getName(), user.getEmail(), user.getAvatarUrl());
  }

  private IssueActivitySummary toIssueSummary(Issue issue) {
    if (issue == null) {
      return null;
    }
    return new IssueActivitySummary(
        issue.getId().toString(),
        issue.getTicketId(),
        issue.getTitle(),
        issue.getStatus().name());
  }
}