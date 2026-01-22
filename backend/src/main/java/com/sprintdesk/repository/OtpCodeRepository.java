package com.sprintdesk.repository;

import com.sprintdesk.model.OtpCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
  Optional<OtpCode> findTopByEmailOrderByCreatedAtDesc(String email);

  void deleteByEmail(String email);
}
