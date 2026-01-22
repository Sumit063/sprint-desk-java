package com.sprintdesk.repository;

import com.sprintdesk.model.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
  List<Comment> findByIssueIdOrderByCreatedAtAsc(UUID issueId);

  void deleteByIssueIdIn(List<UUID> issueIds);
}
