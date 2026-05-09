package com.blogplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordStrengthResponse {
    private int score;
    private String level;
    private String suggestion;
}
