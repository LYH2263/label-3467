package com.blogplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendVerificationEmail(String email, String verifyLink) {
        log.info("[MAIL] verification email to {}: {}", email, verifyLink);
    }

    public void sendResetPasswordEmail(String email, String resetLink) {
        log.info("[MAIL] password reset email to {}: {}", email, resetLink);
    }
}
