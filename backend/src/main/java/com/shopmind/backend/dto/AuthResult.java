package com.shopmind.backend.dto;

public record AuthResult(AuthResponse response, String refreshToken) {}