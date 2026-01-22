package com.sprintdesk.repository;

import com.sprintdesk.model.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
  List<Article> findByWorkspaceId(UUID workspaceId);

  List<Article> findByWorkspaceIdAndLinkedIssueIdsContains(UUID workspaceId, UUID issueId);

  Optional<Article> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

  long countByWorkspaceIdAndCreatedBy(UUID workspaceId, UUID createdBy);

  long countByWorkspaceIdAndUpdatedBy(UUID workspaceId, UUID updatedBy);

  List<Article> findTop5ByWorkspaceIdAndUpdatedByOrderByUpdatedAtDesc(
      UUID workspaceId, UUID updatedBy);
}