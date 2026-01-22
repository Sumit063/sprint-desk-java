package com.sprintdesk.api;

public record ApiError(String message, String code, Object details) {}