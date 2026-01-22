package com.sprintdesk.controller;

import com.sprintdesk.dto.UpdateProfileRequest;
import com.sprintdesk.dto.UserResponse;
import com.sprintdesk.security.SecurityUtils;
import com.sprintdesk.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getMe() {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return ResponseEntity.ok(userService.getUser(userId));
  }

  @PatchMapping("/me")
  public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
    UUID userId = SecurityUtils.getCurrentUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return ResponseEntity.ok(userService.updateProfile(userId, request));
  }
}