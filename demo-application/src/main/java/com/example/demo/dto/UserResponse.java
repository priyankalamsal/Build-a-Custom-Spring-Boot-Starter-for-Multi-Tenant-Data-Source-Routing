package com.example.demo.dto;

import com.example.demo.entity.User;

public record UserResponse(Long id, String name, String email) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
