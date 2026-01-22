package com.sprintdesk.repository;

import com.sprintdesk.model.Issue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
  Optional<Issue> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

  Optional<Issue> findByWorkspaceIdAndTicketId(UUID workspaceId, String ticketId);

  Page<Issue> findByWorkspaceId(UUID workspaceId, Pageable pageable);

  List<Issue> findByWorkspaceId(UUID workspaceId);

  void deleteByWorkspaceId(UUID workspaceId);

  long countByWorkspaceIdAndCreatedBy(UUID workspaceId, UUID createdBy);

  long countByWorkspaceIdAndAssigneeId(UUID workspaceId, UUID assigneeId);

  List<Issue> findTop5ByWorkspaceIdAndCreatedByOrderByCreatedAtDesc(
      UUID workspaceId, UUID createdBy);

  List<Issue> findTop5ByWorkspaceIdAndAssigneeIdOrderByCreatedAtDesc(
      UUID workspaceId, UUID assigneeId);
}
