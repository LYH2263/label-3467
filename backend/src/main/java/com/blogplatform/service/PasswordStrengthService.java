package com.blogplatform.service;

import com.blogplatform.dto.PasswordStrengthResponse;
import org.springframework.stereotype.Service;

@Service
public class PasswordStrengthService {

    public PasswordStrengthResponse evaluate(String password) {
        if (password == null || password.isBlank()) {
            return new PasswordStrengthResponse(0, "WEAK", "密码不能为空");
        }

        int score = 0;
        if (password.length() >= 8) {
            score++;
        }
        if (password.length() >= 12) {
            score++;
        }
        if (password.matches(".*[a-z].*")) {
            score++;
        }
        if (password.matches(".*[A-Z].*")) {
            score++;
        }
        if (password.matches(".*\\d.*")) {
            score++;
        }
        if (password.matches(".*[^a-zA-Z0-9].*")) {
            score++;
        }

        if (score <= 2) {
            return new PasswordStrengthResponse(score, "WEAK", "请增加长度并加入大小写字母、数字和符号");
        }
        if (score <= 4) {
            return new PasswordStrengthResponse(score, "MEDIUM", "建议加入特殊符号并提高长度到12位以上");
        }
        return new PasswordStrengthResponse(score, "STRONG", "密码强度良好");
    }

    public boolean isStrongEnough(String password) {
        return evaluate(password).getScore() >= 4;
    }
}
