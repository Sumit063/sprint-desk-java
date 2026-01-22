package com.sprintdesk.repository;

import com.sprintdesk.model.WorkspaceInvite;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, UUID> {
  @EntityGraph(attributePaths = {"workspace"})
  Optional<WorkspaceInvite> findByCode(String code);
}