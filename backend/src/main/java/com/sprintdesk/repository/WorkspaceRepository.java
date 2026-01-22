package com.sprintdesk.repository;

import com.sprintdesk.model.Workspace;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
  Optional<Workspace> findByKeyIgnoreCase(String key);
  boolean existsByKeyIgnoreCase(String key);
}