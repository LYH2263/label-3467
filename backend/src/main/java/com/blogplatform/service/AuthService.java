package com.blogplatform.service;

import com.blogplatform.dto.*;
import com.blogplatform.entity.Role;
import com.blogplatform.entity.User;
import com.blogplatform.exception.BusinessException;
import com.blogplatform.repository.UserRepository;
import com.blogplatform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordStrengthService passwordStrengthService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        if (!passwordStrengthService.isStrongEnough(request.getPassword())) {
            throw new BusinessException("密码强度不足，请至少包含大小写字母和数字，并建议加入特殊字符");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());

        userRepository.save(user);

        String verifyLink = frontendUrl + "/verify-email?token=" + user.getVerificationToken();
        emailService.sendVerificationEmail(user.getEmail(), verifyLink);

        return AuthResponse.builder()
                .user(toUserProfile(user))
                .verificationToken(user.getVerificationToken())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "用户不存在"));

        if (!user.isEmailVerified()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "请先完成邮箱验证");
        }

        JwtUtil.TokenBundle tokenBundle = jwtUtil.generateToken(user, request.isRememberMe());

        return AuthResponse.builder()
                .token(tokenBundle.getToken())
                .expiresAt(tokenBundle.getExpiresAt())
                .user(toUserProfile(user))
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException("验证令牌无效"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            user.setResetToken(UUID.randomUUID().toString());
            user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            String resetLink = frontendUrl + "/reset-password?token=" + user.getResetToken();
            emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BusinessException("重置令牌无效"));

        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置令牌已过期");
        }

        if (!passwordStrengthService.isStrongEnough(request.getNewPassword())) {
            throw new BusinessException("新密码强度不足");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    public PasswordStrengthResponse checkPasswordStrength(String password) {
        return passwordStrengthService.evaluate(password);
    }

    private UserProfileResponse toUserProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
