package com.dbwb.platform.auth.dto;

import com.dbwb.platform.account.entity.Role;

import java.util.UUID;

public record AuthResponse(String accessToken, UUID accountId, String email, Role role) {
}
