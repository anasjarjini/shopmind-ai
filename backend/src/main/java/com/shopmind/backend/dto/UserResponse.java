package com.shopmind.backend.dto;

import com.shopmind.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String city;
    private String address;
    private String role;

    public static UserResponse from(User u) {
        if (u == null) {
            return null;
        }
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .city(u.getCity())
                .address(u.getAddress())
                .role(u.getRole() != null ? u.getRole().name() : null)
                .build();
    }
}