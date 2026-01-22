package com.sprintdesk.repository;

import com.sprintdesk.model.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
  @EntityGraph(attributePaths = {"workspace"})
  List<WorkspaceMember> findByUserId(UUID userId);

  @EntityGraph(attributePaths = {"user"})
  List<WorkspaceMember> findByWorkspaceId(UUID workspaceId);

  Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}