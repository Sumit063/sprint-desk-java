package com.sprintdesk.service;

import com.sprintdesk.dto.UpdateProfileRequest;
import com.sprintdesk.dto.UserResponse;
import com.sprintdesk.model.User;
import com.sprintdesk.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UserResponse getUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return toUserResponse(user);
  }

  public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (request.name() != null) {
      String trimmed = request.name().trim();
      if (!trimmed.isEmpty()) {
        user.setName(trimmed);
      }
    }
    if (request.avatarUrl() != null) {
      user.setAvatarUrl(request.avatarUrl());
    }
    if (request.contact() != null) {
      user.setContact(request.contact());
    }

    userRepository.save(user);
    return toUserResponse(user);
  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId().toString(),
        user.getEmail(),
        user.getName(),
        user.getAvatarUrl(),
        user.getContact());
  }
}