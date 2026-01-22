package com.sprintdesk.service;

import com.sprintdesk.dto.AuthResponse;

public record AuthPayload(AuthResponse response, String refreshToken) {}