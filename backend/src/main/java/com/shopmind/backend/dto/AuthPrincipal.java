package com.shopmind.backend.dto;

import java.util.UUID;

public record AuthPrincipal(UUID id, String email) {}
