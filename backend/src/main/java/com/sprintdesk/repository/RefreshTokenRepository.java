package com.sprintdesk.repository;

import com.sprintdesk.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  @EntityGraph(attributePaths = "user")
  Optional<RefreshToken> findByTokenHash(String tokenHash);
}