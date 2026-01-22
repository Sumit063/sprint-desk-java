package com.sprintdesk.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 1) String name,
    @Size(max = 2000000) String avatarUrl,
    @Size(max = 255) String contact) {}
