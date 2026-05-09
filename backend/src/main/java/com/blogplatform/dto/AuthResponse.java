package com.blogplatform.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private Long expiresAt;
    private UserProfileResponse user;
    private String verificationToken;
}
