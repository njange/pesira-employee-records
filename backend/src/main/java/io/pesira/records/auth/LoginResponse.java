package io.pesira.records.auth;

import java.time.Instant;

import io.pesira.records.user.UserResponse;

public record LoginResponse(String token, Instant expiresAt, UserResponse user) {
}
