package com.blogplatform.dto;

import com.blogplatform.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean emailVerified;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createdAt;
}
