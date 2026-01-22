package com.sprintdesk.dto;

import jakarta.validation.constraints.NotBlank;

public record DemoLoginRequest(@NotBlank String type) {}